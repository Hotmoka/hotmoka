/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.instrumentation.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.instrumentation.api.InstrumentedClass;
import io.hotmoka.instrumentation.internal.instrumentationsOfClass.AddAccessorMethods;
import io.hotmoka.instrumentation.internal.instrumentationsOfClass.AddConstructorForDeserializationFromStore;
import io.hotmoka.instrumentation.internal.instrumentationsOfClass.AddEnsureLoadedMethods;
import io.hotmoka.instrumentation.internal.instrumentationsOfClass.AddOldAndIfAlreadyLoadedFields;
import io.hotmoka.instrumentation.internal.instrumentationsOfClass.DesugarBootstrapsInvokingEntries;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.AddExtraArgsToCallsToFromContract;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.AddGasUpdates;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.AddRuntimeChecksForWhiteListingProofObligations;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.InstrumentMethodsOfSupportClasses;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.ReplaceFieldAccessesWithAccessors;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.SetCallerAndBalanceAtTheBeginningOfFromContracts;
import io.hotmoka.verification.api.Annotations;
import io.hotmoka.verification.api.Bootstraps;
import io.hotmoka.verification.api.Pushers;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.VerifiedClass;
import it.univr.bcel.StackMapReplacer;

/**
 * An instrumented class file. For instance, storage classes are instrumented
 * by adding the serialization support, and contracts are instrumented in order
 * to deal with calls from contracts. Instrumented classes are ordered by name.
 */
public class InstrumentedClassImpl implements InstrumentedClass {

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName)
		.thenComparing(field -> field.getType().toString());

	/**
	 * The Java class of this instrumented class.
	 */
	private final JavaClass javaClass;

	/**
	 * Performs the instrumentation of a single class file.
	 * 
	 * @param clazz the class to instrument
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	public InstrumentedClassImpl(VerifiedClass clazz, GasCostModel gasCostModel) throws ClassNotFoundException {
		this.javaClass = new Builder(clazz, gasCostModel).classGen.getJavaClass();
	}

	@Override
	public String getClassName() {
		return javaClass.getClassName();
	}

	@Override
	public JavaClass toJavaClass() {
		return javaClass;
	}

	@Override
	public int compareTo(InstrumentedClass other) {
		return getClassName().compareTo(other.getClassName());
	}

	/**
	 * Local scope for the instrumentation of a single class.
	 */
	public static class Builder {

		/**
		 * The class that is being instrumented.
		 */
		private final VerifiedClass verifiedClass;

		/**
		 * The class generator of the verified class.
		 */
		private final ClassGen classGen;

		/**
		 * The methods of the instrumented class, in editable version.
		 */
		private final List<MethodGen> methods;

		/**
		 * The gas cost model used for the instrumentation.
		 */
		private final GasCostModel gasCostModel;

		/**
		 * The constant pool of the class being instrumented.
		 */
		private final ConstantPoolGen cpg;

		/**
		 * The utility that knows about the bootstrap methods of the class being instrumented.
		 */
		private final Bootstraps bootstraps;

		/**
		 * The utility object that allows one to determine the pushers of values in the stack,
		 * for the code of the class under instrumentation.
		 */
		private final Pushers pushers;

		/**
		 * The object that can be used to build complex instructions.
		 */
		private final InstructionFactory factory;

		/**
		 * True if and only if the class being instrumented is a storage class.
		 */
		private final boolean isStorage;

		/**
		 * True if and only if the class being instrumented is a contract class.
		 */
		private final boolean isContract;

		/**
		 * The non-transient instance fields of primitive type or of special reference
		 * types that are allowed in storage objects (such as {@link java.lang.String}
		 * and {@link java.math.BigInteger}). They are defined in the class being
		 * instrumented or in its superclasses up to {@link io.takamaka.code.lang.Storage}.
		 * This list is non-empty for storage classes only. The first set in
		 * the list are the fields of {@link io.takamaka.code.lang.Storage};
		 * the last are the fields of the class being instrumented.
		 */
		private final LinkedList<SortedSet<Field>> eagerNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of reference type,
		 * defined in the class being instrumented (superclasses are not
		 * considered). This set is non-empty for storage classes only.
		 */
		private final SortedSet<Field> lazyNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The class loader that loaded the class under instrumentation and those of the program it belongs to.
		 */
		private final TakamakaClassLoader classLoader;

		/**
		 * The bootstrap methods that have been instrumented since they must receive an
		 * extra parameter, since they call an entry and need the calling contract for that.
		 */
		private final Set<BootstrapMethod> bootstrapMethodsThatWillRequireExtraThis = new HashSet<>();

		/**
		 * A map from a description of invoke instructions that lead into a white-listed method
		 * with proof obligations into the replacement instruction
		 * that has been already computed for them. This is used to avoid recomputing
		 * the replacement for invoke instructions that occur more times inside the same
		 * class. This is not just an optimization, since, for invokedynamic, their bootstrap
		 * might be modified, hence the repeated construction of their checking method
		 * would lead into exception.
		 */
		private final Map<String, InvokeInstruction> whiteListingCache = new HashMap<>();

		/**
		 * Performs the instrumentation of a single class.
		 * 
		 * @param clazz the class to instrument
		 * @param gasCostModel the gas cost model used for the instrumentation
		 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
		 */
		private Builder(VerifiedClass clazz, GasCostModel gasCostModel) throws ClassNotFoundException {
			this.verifiedClass = clazz;
			this.classGen = new ClassGen(clazz.toJavaClass());
			this.bootstraps = verifiedClass.getBootstraps();
			setBootstraps();
			this.pushers = verifiedClass.getPushers();
			this.gasCostModel = gasCostModel;
			this.classLoader = clazz.getJar().getClassLoader();
			this.cpg = classGen.getConstantPool();
			String className = classGen.getClassName();
			this.methods = Stream.of(classGen.getMethods())
				.map(method -> new MethodGen(method, className, cpg))
				.collect(Collectors.toList());
			this.factory = new InstructionFactory(cpg);
			this.isStorage = classLoader.isStorage(className);
			this.isContract = classLoader.isContract(className);

			partitionFieldsOfStorageClasses();
			methodLevelInstrumentations();
			classLevelInstrumentations();
			replaceMethods();
		}

		/**
		 * Sets the bootstrap description of the class to the clone that
		 * has been created in the constructor.
		 */
		private void setBootstraps() {
			Optional<BootstrapMethods> bootstraps = Stream.of(classGen.getAttributes())
				.filter(attribute -> attribute instanceof BootstrapMethods)
				.map(attribute -> (BootstrapMethods) attribute)
				.findFirst();

			if (bootstraps.isPresent()) {
				classGen.removeAttribute(bootstraps.get());
				BootstrapMethods newAttribute = new BootstrapMethods(bootstraps.get());
				newAttribute.setBootstrapMethods(this.bootstraps.getBootstraps().toArray(BootstrapMethod[]::new));
				classGen.addAttribute(newAttribute);
			}
		}

		/**
		 * Partitions the fields of a storage class into eager and lazy.
		 * If the class is not storage, it does not do anything.
		 * 
		 * @throws ClassNotFoundException if {@code classGen} cannot be found in the Takamaka program
		 */
		private void partitionFieldsOfStorageClasses() throws ClassNotFoundException {
			if (isStorage)
				collectNonTransientInstanceFieldsOf(classLoader.loadClass(classGen.getClassName()), true);
		}

		/**
		 * Replaces the methods of the class under instrumentation,
		 * putting the instrumented ones instead of the original ones.
		 */
		private void replaceMethods() {
			classGen.setMethods(new Method[0]);
			for (var method: methods) {
				method.setMaxLocals();
				method.setMaxStack();
				removeUselessAttributes(method);
				if (!method.isAbstract()) {
					method.getInstructionList().setPositions();
					StackMapReplacer.of(method);
				}
				classGen.addMethod(method.getMethod());
			}
		}

		private void removeUselessAttributes(MethodGen method) {
			for (var attribute: method.getAttributes())
				if (attribute instanceof Signature)
					method.removeAttribute(attribute);

			method.removeLocalVariables();
			method.removeLocalVariableTypeTable();
		}

		/**
		 * Shared code of instrumentations.
		 */
		private abstract class Instrumentation {

			/**
			 * The verified class for which instrumentation is performed.
			 */
			protected final VerifiedClass verifiedClass = Builder.this.verifiedClass;

			/**
			 * The annotation wizard of the class being instrumented.
			 */
			protected final Annotations annotations = verifiedClass.getJar().getAnnotations();

			/**
			 * The gas cost model used for the instrumentation.
			 */
			protected final GasCostModel gasCostModel = Builder.this.gasCostModel;

			/**
			 * The name of the class being instrumented.
			 */
			protected final String className = classGen.getClassName();

			/**
			 * The constant pool of the class being instrumented.
			 */
			protected final ConstantPoolGen cpg = Builder.this.cpg;

			/**
			 * The utility that knows about the bootstrap methods of the class being instrumented.
			 */
			protected final Bootstraps bootstraps = Builder.this.bootstraps;

			/**
			 * The utility object that allows one to determine the pushers of values in the stack,
			 * for the code of the class under instrumentation.
			 */
			protected final Pushers pushers = Builder.this.pushers;

			/**
			 * The object that can be used to build complex instructions.
			 */
			protected final InstructionFactory factory = Builder.this.factory;

			/**
			 * True if and only if the class being instrumented is a storage class.
			 */
			protected final boolean isStorage = Builder.this.isStorage;

			/**
			 * True if and only if the class being instrumented is a contract class.
			 */
			protected final boolean isContract = Builder.this.isContract;

			/**
			 * The non-transient instance fields of primitive type or of special reference
			 * types that are allowed in storage objects (such as {@link java.lang.String}
			 * and {@link java.math.BigInteger}). They are defined in the class being
			 * instrumented or in its superclasses up to {@link io.takamaka.code.lang.Storage}
			 * (included). This list is non-empty for storage classes only. The first set in
			 * the list are the fields of the topmost class; the last are the fields of the
			 * class being considered.
			 */
			protected final LinkedList<SortedSet<Field>> eagerNonTransientInstanceFields = Builder.this.eagerNonTransientInstanceFields;

			/**
			 * The non-transient instance fields of reference type,
			 * defined in the class being instrumented (superclasses are not
			 * considered). This set is non-empty for storage classes only.
			 */
			protected final SortedSet<Field> lazyNonTransientInstanceFields = Builder.this.lazyNonTransientInstanceFields;

			/**
			 * The bootstrap methods that have been instrumented since they must receive an
			 * extra parameter, since they call an entry and need the calling contract for that.
			 */
			protected final Set<BootstrapMethod> bootstrapMethodsThatWillRequireExtraThis = Builder.this.bootstrapMethodsThatWillRequireExtraThis;

			/**
			 * The class loader that loaded the class under instrumentation and those of the program it belongs to.
			 */
			protected final TakamakaClassLoader classLoader = Builder.this.classLoader;

			/**
			 * A map from a description of invoke instructions that lead into a white-listed method
			 * with proof obligations into the replacement instruction
			 * that has been already computed for them. This is used to avoid recomputing
			 * the replacement for invoke instructions that occur more times inside the same
			 * class. This is not just an optimization, since, for invokedynamic, their bootstrap
			 * might be modified, hence the repeated construction of their checking method
			 * would lead into exception.
			 */
			protected final Map<String, InvokeInstruction> whiteListingCache = Builder.this.whiteListingCache;

			protected final void addMethod(MethodGen methodGen, boolean needsStackMap) {
				methodGen.getInstructionList().setPositions();
				methodGen.setMaxLocals();
				methodGen.setMaxStack();
				if (needsStackMap)
					StackMapReplacer.of(methodGen);
				methods.add(methodGen);
			}

			protected final String getterNameFor(String className, String fieldName) {
				// we use the class name as well, in order to disambiguate fields with the same name in sub and superclass
				return InstrumentationConstants.GETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
			}

			protected final String setterNameFor(String className, String fieldName) {
				// we use the class name as well, in order to disambiguate fields with the same name in sub and superclass
				return InstrumentationConstants.SETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
			}

			protected final short invokeCorrespondingToBootstrapInvocationType(int invokeKind) {
				switch (invokeKind) {
				case Const.REF_invokeVirtual:
					return Const.INVOKEVIRTUAL;
				case Const.REF_invokeSpecial:
				case Const.REF_newInvokeSpecial:
					return Const.INVOKESPECIAL;
				case Const.REF_invokeInterface:
					return Const.INVOKEINTERFACE;
				case Const.REF_invokeStatic:
					return Const.INVOKESTATIC;
				default:
					throw new IllegalStateException("Unexpected lambda invocation kind " + invokeKind);
				}
			}

			/**
			 * BCEL does not (yet?) provide a method to add a method handle constant into a
			 * constant pool. Hence we have to rely to a trick: first we add a new integer
			 * constant to the constant pool; then we replace it with the method handle
			 * constant. Ugly, but it currently seems to be the only way.
			 * 
			 * @param mh the constant to add
			 * @return the index at which the constant has been added
			 */
			protected final int addMethodHandleToConstantPool(ConstantMethodHandle mh) {
				// first we check if an equal constant method handle was already in the constant pool
				int size = cpg.getSize(), index;
				for (index = 0; index < size; index++)
					if (cpg.getConstant(index) instanceof ConstantMethodHandle) {
						ConstantMethodHandle c = (ConstantMethodHandle) cpg.getConstant(index);
						if (c.getReferenceIndex() == mh.getReferenceIndex() && c.getReferenceKind() == mh.getReferenceKind())
							return index; // found
					}

				// otherwise, we first add an integer that was not already there
				int counter = 0;
				do {
					index = cpg.addInteger(counter++);
				}
				while (cpg.getSize() == size);

				// and then replace the integer constant with the method handle constant
				cpg.setConstant(index, mh);

				return index;
			}

			/**
			 * Yields the methods in this class.
			 * 
			 * @return the methods
			 */
			protected final Stream<MethodGen> getMethods() {
				return methods.stream();
			}

			/**
			 * Yields the fields in this class.
			 * 
			 * @return the fields
			 */
			protected final Stream<org.apache.bcel.classfile.Field> getFields() {
				return Stream.of(classGen.getFields());
			}

			/**
			 * Yields the name for a method that starts with the given prefix, followed
			 * by a numerical index. It guarantees that the name is not yet used for
			 * existing methods.
			 *
			 * @param prefix the prefix
			 * @return the name
			 */
			protected final String getNewNameForPrivateMethod(String prefix) {
				int counter = 0;
				String newName;

				do {
					newName = prefix + counter++;
				}
				while (getMethods().map(MethodGen::getName).anyMatch(newName::equals));

				return newName;
			}

			/**
			 * Sets the name of the superclass of this class.
			 * 
			 * @param name the new name of the superclass of this clas
			 */
			protected final void setSuperclassName(String name) {
				classGen.setSuperclassName(name);
			}

			/**
			 * Adds the given field to this class.
			 * 
			 * @param field the field to add
			 */
			protected final void addField(org.apache.bcel.classfile.Field field) {
				classGen.addField(field);
			}

			/**
			 * Replaces a field of this class with another.
			 * 
			 * @param old the old field to replace
			 * @param _new the new field to put at its place
			 */
			protected final void replaceField(org.apache.bcel.classfile.Field old, org.apache.bcel.classfile.Field _new) {
				classGen.replaceField(old, _new);
			}

			/**
			 * Yields the name of the superclass of this class, if any.
			 * 
			 * @return the name
			 */
			protected final String getSuperclassName() {
				return classGen.getSuperclassName();
			}
		}

		/**
		 * An implementation that works for a class as a whole.
		 */
		public abstract class ClassLevelInstrumentation extends Instrumentation {
		}

		/**
		 * An instrumentation that works at the level of a single method of a class.
		 */
		public abstract class MethodLevelInstrumentation extends Instrumentation {
			protected final MethodGen method;

			protected MethodLevelInstrumentation(MethodGen method) {
				this.method = method;
			}
		}

		/**
		 * Performs class-level instrumentations.
		 */
		private void classLevelInstrumentations() {
			new AddConstructorForDeserializationFromStore(this);
			new AddOldAndIfAlreadyLoadedFields(this);
			new AddAccessorMethods(this);
			new AddEnsureLoadedMethods(this);
		}

		/**
		 * Performs method-level instrumentations.
		 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
		 */
		private void methodLevelInstrumentations() throws ClassNotFoundException {
			for (var method: new ArrayList<>(methods))
				preProcess(method);

			new DesugarBootstrapsInvokingEntries(this);

			for (var method: new ArrayList<>(methods))
				postProcess(method);
		}

		/**
		 * Pre-processing instrumentation of a single method of the class. This is
		 * performed before instrumentation of the bootstraps.
		 * 
		 * @param method the method to instrument
		 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
		 */
		private void preProcess(MethodGen method) throws ClassNotFoundException {
			new AddRuntimeChecksForWhiteListingProofObligations(this, method);
		}

		/**
		 * Post-processing instrumentation of a single method of the class. This is
		 * performed after instrumentation of the bootstraps.
		 * 
		 * @param method the method to instrument
		 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be found
		 */
		private void postProcess(MethodGen method) throws ClassNotFoundException {
			new InstrumentMethodsOfSupportClasses(this, method);
			new ReplaceFieldAccessesWithAccessors(this, method);
			new AddExtraArgsToCallsToFromContract(this, method);
			new SetCallerAndBalanceAtTheBeginningOfFromContracts(this, method);
			new AddGasUpdates(this, method);
		}

		private boolean isNotStaticAndNotTransient(Field field) {
			int modifiers = field.getModifiers();
			return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers);
		}

		private void collectNonTransientInstanceFieldsOf(Class<?> clazz, boolean firstCall) {
			if (clazz != classLoader.getStorage())
				// we put at the beginning the fields of the superclasses
				collectNonTransientInstanceFieldsOf(clazz.getSuperclass(), false);

			Field[] fields = clazz.getDeclaredFields();

			// then the eager fields of className, in order
			eagerNonTransientInstanceFields.add(Stream.of(fields)
				.filter(field -> isNotStaticAndNotTransient(field) && classLoader.isEagerlyLoaded(field.getType()))
				.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

			// we collect lazy fields as well, but only for the class being instrumented
			if (firstCall)
				Stream.of(fields)
					.filter(field -> isNotStaticAndNotTransient(field) && classLoader.isLazilyLoaded(field.getType()))
					.forEach(lazyNonTransientInstanceFields::add);
		}
	}
}