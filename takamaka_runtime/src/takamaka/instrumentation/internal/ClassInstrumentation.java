package takamaka.instrumentation.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ExceptionThrower;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.StackConsumer;
import org.apache.bcel.generic.StackProducer;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import it.univr.bcel.StackMapReplacer;
import takamaka.blockchain.GasCosts;
import takamaka.blockchain.values.StorageReferenceAlreadyInBlockchain;
import takamaka.instrumentation.Dummy;
import takamaka.instrumentation.TakamakaClassLoader;
import takamaka.lang.Contract;
import takamaka.lang.Storage;
import takamaka.lang.Takamaka;
import takamaka.whitelisted.MustBeFalse;
import takamaka.whitelisted.MustBeOrdered;
import takamaka.whitelisted.MustRedefineHashCodeOrToString;
import takamaka.whitelisted.WhiteListingProofObligation;

/**
 * An instrumenter of a single class file. For instance, it instruments storage
 * classes, by adding the serialization support, and contracts, to deal with
 * entries.
 */
public class ClassInstrumentation {
	private final static String OLD_PREFIX = "§old_";
	private final static String IF_ALREADY_LOADED_PREFIX = "§ifAlreadyLoaded_";
	private final static String ENSURE_LOADED_PREFIX = "§ensureLoaded_";
	private final static String GETTER_PREFIX = "§get_";
	private final static String SETTER_PREFIX = "§set_";
	private final static String EXTRA_LAMBDA_NAME = "lambda";
	private final static String EXTRA_VERIFIER_NAME = "verifier";
	private final static String EXTRA_ALLOCATOR_NAME = "multianewarray";
	private final static String EXTRACT_UPDATES = "extractUpdates";
	private final static String RECURSIVE_EXTRACT = "recursiveExtract";
	private final static String ADD_UPDATE_FOR = "addUpdateFor";
	private final static String PAYABLE_ENTRY = "payableEntry";
	private final static String ENTRY = "entry";
	private final static String IN_STORAGE_NAME = "inStorage";
	private final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastLazyUpdateFor";
	private final static String DESERIALIZE_LAST_UPDATE_FOR_FINAL = "deserializeLastLazyUpdateForFinal";
	private final static String CONTRACT_CLASS_NAME = "takamaka.lang.Contract";
	private final static String TAKAMAKA_CLASS_NAME = Takamaka.class.getName();
	private final static String STORAGE_CLASS_NAME = Storage.class.getName();
	private final static short PUBLIC_SYNTHETIC = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC;
	private final static short PUBLIC_SYNTHETIC_FINAL = PUBLIC_SYNTHETIC | Const.ACC_FINAL;
	private final static short PROTECTED_SYNTHETIC = Const.ACC_PROTECTED | Const.ACC_SYNTHETIC;
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;
	private final static short PRIVATE_SYNTHETIC_STATIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_STATIC;
	private final static short PRIVATE_SYNTHETIC_TRANSIENT = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_TRANSIENT;

	/**
	 * The order used for generating the parameters of the instrumented
	 * constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName)
			.thenComparing(field -> field.getType().toString());

	private final static ObjectType CONTRACT_OT = new ObjectType(CONTRACT_CLASS_NAME);
	private final static ObjectType BIGINTEGER_OT = new ObjectType(BigInteger.class.getName());
	private final static ObjectType ENUM_OT = new ObjectType(Enum.class.getName());
	private final static ObjectType SET_OT = new ObjectType(Set.class.getName());
	private final static ObjectType LIST_OT = new ObjectType(List.class.getName());
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());
	private final static Type[] THREE_STRINGS_ARGS = { Type.STRING, Type.STRING, Type.STRING };
	private final static Type[] EXTRACT_UPDATES_ARGS = { SET_OT, SET_OT, LIST_OT };
	private final static Type[] ADD_UPDATES_FOR_ARGS = { Type.STRING, Type.STRING, SET_OT };
	private final static Type[] RECURSIVE_EXTRACT_ARGS = { Type.OBJECT, SET_OT, SET_OT, LIST_OT };
	private final static Type[] ENTRY_ARGS = { CONTRACT_OT };
	private final static Type[] ONE_INT_ARGS = { Type.INT };
	private final static Type[] ONE_LONG_ARGS = { Type.LONG };
	private final static Type[] ONE_BIGINTEGER_ARGS = { BIGINTEGER_OT };
	private final static Type[] TWO_OBJECTS_ARGS = { Type.OBJECT, Type.OBJECT };

	/**
	 * Performs the instrumentation of a single class file.
	 * 
	 * @param clazz the class to instrument
	 * @param instrumentedJar the jar where the instrumented class will be added
	 * @throws ClassFormatException if some class file is not legal
	 * @throws IOException if there is an error accessing the disk
	 */
	public ClassInstrumentation(VerifiedClass clazz, JarOutputStream instrumentedJar) throws ClassFormatException, IOException {
		// performs instrumentation on the class
		new Initializer(clazz);

		// dump the instrumented class on disk
		clazz.getJavaClass().dump(instrumentedJar);
	}

	/**
	 * Local scope for the instrumentation of a single class.
	 */
	private class Initializer {

		/**
		 * The class that is being instrumented.
		 */
		private final VerifiedClass clazz;

		/**
		 * The name of the class being instrumented.
		 */
		private final String className;

		/**
		 * The constant pool of the class being instrumented.
		 */
		private final ConstantPoolGen cpg;

		/**
		 * The object that can be used to build complex instructions.
		 */
		private final InstructionFactory factory;

		/**
		 * True if and only if the class being instrumented is a storage class, distinct
		 * form {@link takamaka.lang.Storage} itself, that must not be instrumented.
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
		 * instrumented or in its superclasses up to {@link takamaka.lang.Storage}
		 * (excluded). This list is non-empty for storage classes only. The first set in
		 * the list are the fields of the topmost class; the last are the fields of the
		 * class being considered.
		 */
		private final LinkedList<SortedSet<Field>> eagerNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of type {@link takamaka.lang.Storage} or
		 * subclass, defined in the class being instrumented (superclasses are not
		 * considered). This set is non-empty for storage classes only.
		 */
		private final SortedSet<Field> lazyNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The class loader that loaded the class under instrumentation and those of the program it belongs to.
		 */
		private final TakamakaClassLoader classLoader;

		/**
		 * The bootstrap methods that have been instrumented since they must receive an
		 * extra parameter, since they call an entry and need the calling contract for
		 * that.
		 */
		private final Set<BootstrapMethod> bootstrapMethodsThatWillRequireExtraThis = new HashSet<>();

		/**
		 * A map from a description of invoke instructions that lead into a white-listed method
		 * with proof obligations into the replacement instruction
		 * that has been already computed for them. This is used to avoid recomputing
		 * the replacement for invoke instructions that occurs more times inside the same
		 * class. This is not just an optimization, since, for invokedynamic, their bootstrap
		 * might be modified, hence the repeated construction of their checking method
		 * would lead into exception.
		 */
		private final Map<String, InvokeInstruction> whiteListingCache = new HashMap<>();

		/**
		 * Performs the instrumentation of a single class.
		 * 
		 * @param clazz the class to instrument
		 */
		private Initializer(VerifiedClass clazz) {
			this.clazz = clazz;
			this.className = clazz.getClassName();
			this.classLoader = clazz.classLoader;
			this.cpg = clazz.getConstantPool();
			this.factory = new InstructionFactory(cpg);
			this.isStorage = !className.equals(STORAGE_CLASS_NAME) && classLoader.isStorage(className);
			this.isContract = classLoader.isContract(className);

			// the fields of the class are relevant only for storage classes
			if (isStorage)
				ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					collectNonTransientInstanceFieldsOf(classLoader.loadClass(className), true);
				});

			instrumentClass();
		}

		/**
		 * performs the instrumentation.
		 */
		private void instrumentClass() {
			// local instrumentations are those that apply at method level
			localInstrumentation();

			// global instrumentations are those that apply at class level
			globalInstrumentation();
		}

		/**
		 * Performs class-level instrumentations.
		 */
		private void globalInstrumentation() {
			if (isStorage) {
				// storage classes need the serialization machinery
				addOldAndIfAlreadyLoadedFields();
				addConstructorForDeserializationFromBlockchain();
				addAccessorMethods();
				addEnsureLoadedMethods();
				addExtractUpdates();
			}
		}

		/**
		 * Instruments bootstrap methods that invoke an entry as their target code. They
		 * are the result of compiling method references to entries. Since entries
		 * receive extra parameters, we transform those bootstrap methods by calling
		 * brand new target code, that calls the entry with a normal invoke instruction.
		 * That instruction will be later instrumented during local instrumentation.
		 */
		private void instrumentBootstrapsInvokingEntries() {
			clazz.bootstraps.getBootstrapsLeadingToEntries().forEach(this::instrumentBootstrapCallingEntry);
		}

		private void instrumentBootstrapCallingEntry(BootstrapMethod bootstrap) {
			if (clazz.bootstraps.lambdaIsEntry(bootstrap))
				instrumentLambdaEntry(bootstrap);
			else
				instrumentLambdaCallingEntry(bootstrap);
		}

		private void instrumentLambdaCallingEntry(BootstrapMethod bootstrap) {
			int[] args = bootstrap.getBootstrapArguments();
			ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(args[1]);
			int invokeKind = mh.getReferenceKind();

			if (invokeKind == Const.REF_invokeStatic) {
				// we instrument bootstrap methods that call a static lambda that calls an entry:
				// the problem is that the instrumentation of the entry will need local 0 (this)
				// to pass the calling contract, consequently it must be made into an instance method

				ConstantMethodref mr = (ConstantMethodref) cpg.getConstant(mh.getReferenceIndex());
				ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
				String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
				String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
				Optional<Method> old = Stream.of(clazz.getMethods())
						.filter(method -> method.getName().equals(methodName)
								&& method.getSignature().equals(methodSignature) && method.isPrivate())
						.findAny();
				old.ifPresent(method -> {
					// we can modify the method handle since the lambda is becoming an instance
					// method and all calls must be made through invokespecial
					mh.setReferenceKind(Const.REF_invokeSpecial);
					makeFromStaticToInstance(method);
					bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
				});
			}
		}

		private void makeFromStaticToInstance(Method old) {
			MethodGen _new = new MethodGen(old, className, cpg);
			_new.isStatic(false);
			if (!_new.isAbstract())
				// we increase the indexes of the local variables used in the method
				for (InstructionHandle ih : _new.getInstructionList()) {
					Instruction ins = ih.getInstruction();
					if (ins instanceof LocalVariableInstruction) {
						int index = ((LocalVariableInstruction) ins).getIndex();
						if (ins instanceof IINC)
							ih.setInstruction(new IINC(index + 1, ((IINC) ins).getIncrement()));
						else if (ins instanceof LoadInstruction)
							ih.setInstruction(
									InstructionFactory.createLoad(((LoadInstruction) ins).getType(cpg), index + 1));
						else if (ins instanceof StoreInstruction)
							ih.setInstruction(
									InstructionFactory.createStore(((LoadInstruction) ins).getType(cpg), index + 1));
					}
				}

			StackMapReplacer.replace(_new);
			clazz.replaceMethod(old, _new.getMethod());
		}

		private void instrumentLambdaEntry(BootstrapMethod bootstrap) {
			int[] args = bootstrap.getBootstrapArguments();
			ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(args[1]);
			int invokeKind = mh.getReferenceKind();
			ConstantMethodref mr = (ConstantMethodref) cpg.getConstant(mh.getReferenceIndex());
			int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
			String entryClassName = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
			ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
			String entryName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
			String entrySignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
			Type[] entryArgs = Type.getArgumentTypes(entrySignature);
			Type entryReturnType = Type.getReturnType(entrySignature);
			String implementedInterfaceMethosSignature = ((ConstantUtf8) cpg
					.getConstant(((ConstantMethodType) cpg.getConstant(args[2])).getDescriptorIndex())).getBytes();
			Type lambdaReturnType = Type.getReturnType(implementedInterfaceMethosSignature);

			// we replace the target code: it was an invokeX C.entry(pars):r and we
			// transform it
			// into invokespecial className.lambda(C, pars):r where the name "lambda" is
			// not used in className. The extra parameter className is not added for
			// constructor references, since they create the new object themselves
			String lambdaName = getNewNameForPrivateMethod(EXTRA_LAMBDA_NAME);

			Type[] lambdaArgs;
			if (invokeKind == Const.REF_newInvokeSpecial)
				lambdaArgs = entryArgs;
			else {
				lambdaArgs = new Type[entryArgs.length + 1];
				System.arraycopy(entryArgs, 0, lambdaArgs, 1, entryArgs.length);
				lambdaArgs[0] = new ObjectType(entryClassName);
			}

			String lambdaSignature = Type.getMethodSignature(lambdaReturnType, lambdaArgs);

			// replace inside the bootstrap method
			args[1] = addMethodHandleToConstantPool(new ConstantMethodHandle(Const.REF_invokeSpecial,
					cpg.addMethodref(className, lambdaName, lambdaSignature)));

			// we create the target code: it is a new private synthetic instance method
			// inside className,
			// called lambdaName and with signature lambdaSignature; its code loads all its
			// explicit parameters
			// on the stack then calls the entry and returns its value (if any)
			InstructionList il = new InstructionList();
			if (invokeKind == Const.REF_newInvokeSpecial) {
				il.append(factory.createNew(entryClassName));
				if (lambdaReturnType != Type.VOID)
					il.append(InstructionConst.DUP);
			}

			int local = 1;
			for (Type arg : lambdaArgs) {
				il.append(InstructionFactory.createLoad(arg, local));
				local += arg.getSize();
			}

			il.append(factory.createInvoke(entryClassName, entryName, entryReturnType, entryArgs,
					invokeCorrespondingToBootstrapInvocationType(invokeKind)));
			il.append(InstructionFactory.createReturn(lambdaReturnType));

			MethodGen addedLambda = new MethodGen(PRIVATE_SYNTHETIC, lambdaReturnType, lambdaArgs, null, lambdaName,
					className, il, cpg);
			il.setPositions();
			addedLambda.setMaxLocals();
			addedLambda.setMaxStack();
			clazz.addMethod(addedLambda.getMethod());
			bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
		}

		private short invokeCorrespondingToBootstrapInvocationType(int invokeKind) {
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
		 * constant. Ugly, but it currently seem to be the only way.
		 * 
		 * @param mh the constant to add
		 * @return the index at which the constant has been added
		 */
		private int addMethodHandleToConstantPool(ConstantMethodHandle mh) {
			// first we check if an equal constant method handle was already in the constant
			// pool
			int size = cpg.getSize(), index;
			for (index = 0; index < size; index++)
				if (cpg.getConstant(index) instanceof ConstantMethodHandle) {
					ConstantMethodHandle c = (ConstantMethodHandle) cpg.getConstant(index);
					if (c.getReferenceIndex() == mh.getReferenceIndex()
							&& c.getReferenceKind() == mh.getReferenceKind())
						return index; // found
				}

			// otherwise, we first add an integer that was not already there
			int counter = 0;
			do {
				index = cpg.addInteger(counter++);
			} while (cpg.getSize() == size);

			// and then replace the integer constant with the method handle constant
			cpg.setConstant(index, mh);

			return index;
		}

		/**
		 * BCEL does not (yet?) provide a method to add an invokedynamic constant into a
		 * constant pool. Hence we have to rely to a trick: first we add a new integer
		 * constant to the constant pool; then we replace it with the invokedynamic
		 * constant. Ugly, but it currently seem to be the only way.
		 * 
		 * @param cid the constant to add
		 * @return the index at which the constant has been added
		 */
		private int addInvokeDynamicToConstantPool(ConstantInvokeDynamic cid) {
			// first we check if an equal constant method handle was already in the constant
			// pool
			int size = cpg.getSize(), index;
			for (index = 0; index < size; index++)
				if (cpg.getConstant(index) instanceof ConstantInvokeDynamic) {
					ConstantInvokeDynamic c = (ConstantInvokeDynamic) cpg.getConstant(index);
					if (c.getBootstrapMethodAttrIndex() == cid.getBootstrapMethodAttrIndex()
							&& c.getNameAndTypeIndex() == cid.getNameAndTypeIndex())
						return index; // found
				}

			// otherwise, we first add an integer that was not already there
			int counter = 0;
			do {
				index = cpg.addInteger(counter++);
			} while (cpg.getSize() == size);

			// and then replace the integer constant with the method handle constant
			cpg.setConstant(index, cid);

			return index;
		}

		private String getNewNameForPrivateMethod(String innerName) {
			int counter = 0;
			String newName;
			Method[] methods = clazz.getMethods();

			do {
				newName = "§" + innerName + counter++;
			} while (Stream.of(methods).map(Method::getName).anyMatch(newName::equals));

			return newName;
		}

		/**
		 * Performs method-level instrumentations.
		 */
		private void localInstrumentation() {
			applyToAllMethods(this::preProcess);
			instrumentBootstrapsInvokingEntries();
			applyToAllMethods(this::postProcess);
		}

		private void applyToAllMethods(Function<Method, Method> what) {
			Method[] methods = clazz.getMethods();

			List<Method> processedMethods = Stream.of(methods).map(what).collect(Collectors.toList());

			// replacing old with new methods
			int pos = 0;
			for (Method processed: processedMethods)
				clazz.replaceMethod(methods[pos++], processed);
		}

		/**
		 * Pre-processing instrumentation of a single method of the class. This is
		 * performed before instrumentation of the bootstraps.
		 * 
		 * @param method the method to instrument
		 * @return the result of the instrumentation
		 */
		private Method preProcess(Method method) {
			MethodGen methodGen = new MethodGen(method, className, cpg);
			addRuntimeChecksForWhiteListingProofObligations(methodGen);
			return methodGen.getMethod();
		}

		/**
		 * Post-processing instrumentation of a single method of the class. This is
		 * performed after instrumentation of the bootstraps.
		 * 
		 * @param method the method to instrument
		 * @return the result of the instrumentation
		 */
		private Method postProcess(Method method) {
			MethodGen methodGen = new MethodGen(method, className, cpg);
			replaceFieldAccessesWithAccessors(methodGen);
			addContractToCallsToEntries(methodGen);

			Optional<Class<?>> callerContract;
			if (isContract && (callerContract = clazz.annotations.isEntry(className, method.getName(),
					method.getArgumentTypes(), method.getReturnType())).isPresent())
				instrumentEntry(methodGen, callerContract.get(), clazz.annotations.isPayable(className, method.getName(),
						method.getArgumentTypes(), method.getReturnType()));

			addGasUpdates(methodGen);

			methodGen.setMaxLocals();
			methodGen.setMaxStack();
			if (!methodGen.isAbstract()) {
				methodGen.getInstructionList().setPositions();
				StackMapReplacer.replace(methodGen);
			}

			return methodGen.getMethod();
		}

		private void addRuntimeChecksForWhiteListingProofObligations(MethodGen method) {
			if (!method.isAbstract())
				for (InstructionHandle ih: method.getInstructionList()) {
					Instruction ins = ih.getInstruction();
					if (ins instanceof FieldInstruction) {
						FieldInstruction fi = (FieldInstruction) ins;
						Field model = clazz.whiteListingModelOf(fi);
						if (hasProofObligations(model))
							// proof obligations are currently not implemented nor used on fields
							throw new IllegalStateException("unexpected white-listing proof obligation for field " + fi.getReferenceType(cpg) + "." + fi.getFieldName(cpg));
					}
					else if (ins instanceof InvokeInstruction) {
						// we share the same checker for equivalent invoke instructions
						String key = keyFor(ih);
						InvokeInstruction replacement = whiteListingCache.get(key);
						if (replacement != null)
							ih.setInstruction(replacement);
						else {
							Executable model = clazz.whiteListingModelOf((InvokeInstruction) ins);
							if (hasProofObligations(model)) {
								replacement = addWhiteListVerificationMethod(ih, (InvokeInstruction) ins, model, key);
								whiteListingCache.put(key, replacement);
								ih.setInstruction(replacement);
							}
						}
					}
				}
		}

		private boolean isCallToConcatenationMetaFactory(INVOKEDYNAMIC invokedynamic) {
			BootstrapMethod bootstrap = clazz.bootstraps.getBootstrapFor(invokedynamic);
			Constant constant = cpg.getConstant(bootstrap.getBootstrapMethodRef());
			ConstantMethodHandle mh = (ConstantMethodHandle) constant;
			Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
			ConstantMethodref mr = (ConstantMethodref) constant2;
			int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
			String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
			ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
			String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
			String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

			// this meta-factory is used by Java compilers for optimized concatenation into string
			return "java.lang.invoke.StringConcatFactory".equals(className)
					&& "makeConcatWithConstants".equals(methodName)
					&& "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
							.equals(methodSignature);
		}

		/**
		 * Adds a static method to the class under instrumentation, that checks that the
		 * white-listing proof obligations in the model hold at run time.
		 * 
		 * @param ins the call instruction whose parameters must be verified
		 * @param model the model that contains the proof obligations in order, for the
		 *              call, to be white-listed
		 * @param key the key used to identify equivalent invoke instructions. It is used to check which
		 *            proof obligations need to be checked at run time
		 * @return the invoke instruction that must be used, instead of {@code ins}, to
		 *         call the freshly added method
		 */
		private InvokeInstruction addWhiteListVerificationMethod(InstructionHandle ih, InvokeInstruction ins, Executable model, String key) {
			if (ins instanceof INVOKEDYNAMIC)
				if (isCallToConcatenationMetaFactory((INVOKEDYNAMIC) ins))
					return addWhiteListVerificationMethodForINVOKEDYNAMICForStringConcatenation((INVOKEDYNAMIC) ins);
				else
					return addWhiteListVerificationMethod((INVOKEDYNAMIC) ins, model);
			else
				return addWhiteListVerificationMethodForNonINVOKEDYNAMIC(ih, ins, model, key);
		}

		private String keyFor(InstructionHandle ih) {
			InvokeInstruction ins = (InvokeInstruction) ih.getInstruction();

			String key;
			if (ins instanceof INVOKEDYNAMIC)
				key = ins.getName() + " #" + ((ConstantInvokeDynamic) cpg.getConstant(((INVOKEDYNAMIC) ins).getIndex())).getBootstrapMethodAttrIndex();
			else {
				key = ins.getName() + " " + ins.getReferenceType(cpg) + "." + ins.getMethodName(cpg) + ins.getSignature(cpg);
				// we add a mask that specifies the white-listing proof obligations that can be discharged, since
				// we can use the same verifier only if two instructions need verification of the same proof obligations
				Executable model = clazz.whiteListingModelOf((InvokeInstruction) ins);
				if (hasProofObligations(model)) {
					int slots = ins.consumeStack(cpg);
					String mask = "";

					if (!(ins instanceof INVOKESTATIC)) {
						int slotsCopy = slots;
						mask += Stream.of(model.getAnnotations())
								.map(Annotation::annotationType)
								.filter(annotationType -> annotationType.isAnnotationPresent(WhiteListingProofObligation.class))
								.map(annotationType -> canBeStaticallyDicharged(annotationType, ih, slotsCopy) ? "0" : "1")
								.collect(Collectors.joining());
						slots--;
					}

					Annotation[][] anns = model.getParameterAnnotations();
					int par = 0;
					for (Type argType: ins.getArgumentTypes(cpg)) {
						int slotsCopy = slots;
						mask += Stream.of(anns[par])
								.flatMap(Stream::of)
								.map(Annotation::annotationType)
								.filter(annotationType -> annotationType.isAnnotationPresent(WhiteListingProofObligation.class))
								.map(annotationType -> canBeStaticallyDicharged(annotationType, ih, slotsCopy) ? "0" : "1")
								.collect(Collectors.joining());
						par++;
						slots -= argType.getSize();
					}

					key = mask + ": " + key;
				}
			}

			return key;
		}

		private InvokeInstruction addWhiteListVerificationMethodForINVOKEDYNAMICForStringConcatenation(INVOKEDYNAMIC invokedynamic) {
			String verifierName = getNewNameForPrivateMethod(EXTRA_VERIFIER_NAME);
			InstructionList il = new InstructionList();
			String signature = invokedynamic.getSignature(cpg);
			Type verifierReturnType = Type.getReturnType(signature);
			Type[] args = Type.getArgumentTypes(signature);

			int index = 0;
			boolean atLeastOneCheck = false;

			for (Type argType: args) {
				il.append(InstructionFactory.createLoad(argType, index));
				index += argType.getSize();
				if (argType instanceof ObjectType) {
					Class<?> argClass = ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> classLoader.loadClass(((ObjectType) argType).getClassName()));

					// we check if we can statically verify that the value redefines hashCode or toString
					if (!Takamaka.redefinesHashCodeOrToString(argClass)) {
						il.append(InstructionFactory.createDup(argType.getSize()));
						il.append(factory.createConstant("string concatenation"));
						il.append(createInvokeForWhiteListingCheck(Takamaka.getWhiteListingCheckFor(MustRedefineHashCodeOrToString.class).get()));
						atLeastOneCheck = true;
					}
				}
			}

			// if all proof obligations can be discharged statically, we do not generate
			// any verification method and yield the same invoke instruction. Note that this
			// optimization depends on the static types of the arguments of the call only,
			// hence it can be safely cached
			if (!atLeastOneCheck)
				return invokedynamic;

			il.append(invokedynamic);
			il.append(InstructionFactory.createReturn(verifierReturnType));

			MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, args, null, verifierName, className, il, cpg);

			il.setPositions();
			addedVerifier.setMaxLocals();
			addedVerifier.setMaxStack();
			clazz.addMethod(addedVerifier.getMethod());

			return factory.createInvoke(className, verifierName, verifierReturnType, args, Const.INVOKESTATIC);
		}

		/**
		 * Adds a static method to the class under instrumentation, that checks that the
		 * white-listing proof obligations in the model hold at run time.
		 * 
		 * @param invokedynamic the call instruction whose parameters must be verified
		 * @param model         the model that contains the proof obligations in order,
		 *                      for the call, to be white-listed
		 */
		private InvokeInstruction addWhiteListVerificationMethod(INVOKEDYNAMIC invokedynamic, Executable model) {
			String verifierName = getNewNameForPrivateMethod(EXTRA_VERIFIER_NAME);
			Bootstraps classBootstraps = clazz.bootstraps;
			InstructionList il = new InstructionList();
			List<Type> args = new ArrayList<>();
			BootstrapMethod bootstrap = classBootstraps.getBootstrapFor(invokedynamic);
			int[] bootstrapArgs = bootstrap.getBootstrapArguments();
			ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(bootstrapArgs[1]);
			int invokeKind = mh.getReferenceKind();
			Executable target = classBootstraps.getTargetOf(bootstrap).get();
			Class<?> receiverClass = target.getDeclaringClass();
			if (receiverClass.isArray())
				receiverClass = Object.class;
			Type receiver = Type.getType(receiverClass);
			Type verifierReturnType = target instanceof Constructor<?> ? Type.VOID : Type.getType(((java.lang.reflect.Method) target).getReturnType());
			int index = 0;

			if (!Modifier.isStatic(target.getModifiers())) {
				il.append(InstructionFactory.createLoad(receiver, index));
				index += receiver.getSize();
				addWhiteListingChecksFor(null, model.getAnnotations(), receiver, il, target.getName(), null, -1);
			}

			int par = 0;
			Annotation[][] anns = model.getParameterAnnotations();

			for (Class<?> arg : target.getParameterTypes()) {
				Type argType = Type.getType(arg);
				args.add(argType);
				il.append(InstructionFactory.createLoad(argType, index));
				index += argType.getSize();
				addWhiteListingChecksFor(null, anns[par], argType, il, target.getName(), null, -1);
				par++;
			}

			Type[] argsAsArray = args.toArray(new Type[args.size()]);
			il.append(factory.createInvoke(receiverClass.getName(), target.getName(), verifierReturnType, argsAsArray,
					invokeCorrespondingToBootstrapInvocationType(invokeKind)));
			il.append(InstructionFactory.createReturn(verifierReturnType));

			MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, argsAsArray, null, verifierName, className, il, cpg);

			il.setPositions();
			addedVerifier.setMaxLocals();
			addedVerifier.setMaxStack();
			clazz.addMethod(addedVerifier.getMethod());

			// replace inside the bootstrap method
			bootstrapArgs[1] = addMethodHandleToConstantPool(new ConstantMethodHandle(Const.REF_invokeStatic, cpg
				.addMethodref(className, verifierName, Type.getMethodSignature(verifierReturnType, argsAsArray))));

			// we return the same invoke instruction, but its bootstrap method has been modified
			return invokedynamic;
		}

		/**
		 * Adds a static method to the class under instrumentation, that checks that the
		 * white-listing proof obligations in the model hold at run time.
		 * 
		 * @param invoke the call instruction whose parameters must be verified
		 * @param key the key used to identify equivalent invoke instructions. It is used to check which
		 *            proof obligations need to be checked at run time
		 * @param model the model that contains the proof obligations in order, for the
		 *              call, to be white-listed
		 */
		private InvokeInstruction addWhiteListVerificationMethodForNonINVOKEDYNAMIC(InstructionHandle ih, InvokeInstruction invoke, Executable model, String key) {
			String verifierName = getNewNameForPrivateMethod(EXTRA_VERIFIER_NAME);
			Type verifierReturnType = invoke.getReturnType(cpg);
			String methodName = invoke.getMethodName(cpg);
			InstructionList il = new InstructionList();
			List<Type> args = new ArrayList<>();
			int index = 0;
			boolean atLeastOne = false;
			int annotationsCursor = 0;

			if (!(invoke instanceof INVOKESTATIC)) {
				ReferenceType receiver;

				if (invoke instanceof INVOKESPECIAL && !Const.CONSTRUCTOR_NAME.equals(methodName)) {
					// call to a private instance method or to an instance method through super.m():
					// we provide a more precise type for the receiver, that is needed for JVM verification
					receiver = new ObjectType(className);
					args.add(receiver);
				}
				else {
					receiver = invoke.getReferenceType(cpg);
					if (receiver instanceof ObjectType)
						args.add(receiver);
					else
						args.add(ObjectType.OBJECT);
				}

				il.append(InstructionFactory.createLoad(receiver, index));
				Annotation[] anns = model.getAnnotations();
				atLeastOne |= addWhiteListingChecksFor(ih, anns, receiver, il, methodName, key, annotationsCursor);
				index++;
				annotationsCursor += anns.length;
			}

			int par = 0;
			Annotation[][] anns = model.getParameterAnnotations();

			for (Type argType: invoke.getArgumentTypes(cpg)) {
				args.add(argType);
				il.append(InstructionFactory.createLoad(argType, index));
				atLeastOne |= addWhiteListingChecksFor(ih, anns[par], argType, il, methodName, key, annotationsCursor);
				index += argType.getSize();
				annotationsCursor += anns[par].length;
				par++;
			}

			if (!atLeastOne)
				// all proof obligations can be discharged statically: we do not generate the checker
				return invoke;

			il.append(invoke);
			il.append(InstructionFactory.createReturn(verifierReturnType));

			Type[] argsAsArray = args.toArray(new Type[args.size()]);
			MethodGen addedVerifier = new MethodGen(PRIVATE_SYNTHETIC_STATIC, verifierReturnType, argsAsArray, null,
					verifierName, className, il, cpg);

			il.setPositions();
			addedVerifier.setMaxLocals();
			addedVerifier.setMaxStack();
			clazz.addMethod(addedVerifier.getMethod());

			return factory.createInvoke(className, verifierName, verifierReturnType, argsAsArray, Const.INVOKESTATIC);
		}

		private boolean addWhiteListingChecksFor(InstructionHandle ih, Annotation[] annotations, Type argType, InstructionList il, String methodName, String key, int annotationsCursor) {
			int initialSize = il.getLength();

			for (Annotation ann: annotations) {
				Optional<java.lang.reflect.Method> checkMethod = Takamaka.getWhiteListingCheckFor(ann);
				if (checkMethod.isPresent())
					// we check if the annotation could not be statically discharged
					if (ih == null || key.charAt(annotationsCursor++) == '1') {
						il.append(InstructionFactory.createDup(argType.getSize()));
						il.append(factory.createConstant(methodName));
						il.append(createInvokeForWhiteListingCheck(checkMethod.get()));
					}
			}

			return il.getLength() > initialSize;
		}

		private InvokeInstruction createInvokeForWhiteListingCheck(java.lang.reflect.Method checkMethod) {
			return factory.createInvoke(TAKAMAKA_CLASS_NAME, checkMethod.getName(), Type.VOID,
				new Type[] { Type.getType(checkMethod.getParameterTypes()[0]), Type.STRING }, Const.INVOKESTATIC);
		}

		private boolean canBeStaticallyDicharged(Class<? extends Annotation> annotationType, InstructionHandle ih, int slots) {
			// ih contains an InvokeInstructoin distinct from INVOKEDYNAMIC

			List<Instruction> pushers = new ArrayList<>();

			if (annotationType == MustBeFalse.class) {
				forEachPusher(ih, slots, where -> pushers.add(where.getInstruction()), () -> pushers.add(null));
				return pushers.stream().allMatch(ins -> ins != null && ins instanceof ICONST && ((ICONST) ins).getValue().equals(0));
			}
			else if (annotationType == MustBeOrdered.class) {
				InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
				int consumed = invoke.consumeStack(cpg);
				Type type;

				if (invoke instanceof INVOKESTATIC)
					type = invoke.getArgumentTypes(cpg)[consumed - slots];
				else if (consumed == slots)
					type = invoke.getReferenceType(cpg);
				else
					type = invoke.getArgumentTypes(cpg)[consumed - slots - 1];

				ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					return type instanceof ObjectType && Takamaka.isOrdered(classLoader.loadClass(((ObjectType) type).getClassName()));
				});
			}

			return false;
		}

		private boolean hasProofObligations(Field field) {
			return Stream.of(field.getAnnotations()).map(Annotation::annotationType).anyMatch(annotation -> annotation.isAnnotationPresent(WhiteListingProofObligation.class));
		}

		private boolean hasProofObligations(Executable method) {
			return Stream.of(method.getAnnotations()).map(Annotation::annotationType).anyMatch(annotation -> annotation.isAnnotationPresent(WhiteListingProofObligation.class))
					||
				Stream.of(method.getParameterAnnotations()).flatMap(Stream::of).map(Annotation::annotationType).anyMatch(annotation -> annotation.isAnnotationPresent(WhiteListingProofObligation.class));
		}

		/**
		 * Adds a gas decrease at the beginning of each basic block of code.
		 * 
		 * @param method the method that gets instrumented
		 */
		private void addGasUpdates(MethodGen method) {
			if (!method.isAbstract()) {
				SortedSet<InstructionHandle> dominators = computeDominators(method);
				InstructionList il = method.getInstructionList();
				CodeExceptionGen[] ceg = method.getExceptionHandlers();
				dominators.stream().forEachOrdered(dominator -> addCpuGasUpdate(dominator, il, ceg, dominators));
				StreamSupport.stream(il.spliterator(), false).forEachOrdered(ih -> addRamGasUpdate(ih, il, ceg));			
			}
		}

		private void addRamGasUpdate(InstructionHandle ih, InstructionList il, CodeExceptionGen[] ceg) {
			Instruction bytecode = ih.getInstruction();

			if (bytecode instanceof InvokeInstruction) {
				// we compute an estimation of the size of the activation frame for the callee
				InvokeInstruction invoke = (InvokeInstruction) bytecode;
				long size = invoke.getArgumentTypes(cpg).length;
				if (invoke instanceof INVOKEVIRTUAL || invoke instanceof INVOKESPECIAL || invoke instanceof INVOKEINTERFACE)
					size++;

				// non risk of overflow, since there are at most 256 arguments in a method
				size *= GasCosts.RAM_COST_PER_ACTIVATION_SLOT;
				size += GasCosts.RAM_COST_PER_ACTIVATION_RECORD;

				InstructionHandle newTarget = il.insert(ih, createConstantPusher(size));
				il.insert(ih, chargeCall(size, "chargeForRAM"));
				il.redirectBranches(ih, newTarget);
				il.redirectExceptionHandlers(ceg, ih, newTarget);
			}
			else if (bytecode instanceof NEW) {
				NEW _new = (NEW) bytecode;
				ObjectType createdClass = _new.getLoadClassType(cpg);
				long size = numberOfInstanceFieldsOf(createdClass) * GasCosts.RAM_COST_PER_FIELD;
				InstructionHandle newTarget = il.insert(ih, createConstantPusher(size));
				il.insert(ih, chargeCall(size, "chargeForRAM"));
				il.redirectBranches(ih, newTarget);
				il.redirectExceptionHandlers(ceg, ih, newTarget);
			}
			else if (bytecode instanceof NEWARRAY || bytecode instanceof ANEWARRAY) {
				InstructionHandle newTarget = il.insert(ih, InstructionConst.DUP);
				il.insert(ih, factory.createInvoke(TAKAMAKA_CLASS_NAME, "chargeForRAMForArrayOfLength", Type.VOID, ONE_INT_ARGS, Const.INVOKESTATIC));
				il.redirectBranches(ih, newTarget);
				il.redirectExceptionHandlers(ceg, ih, newTarget);
			}
			else if (bytecode instanceof MULTIANEWARRAY) {
				MULTIANEWARRAY multianewarray = (MULTIANEWARRAY) bytecode;
				Type createdType = multianewarray.getType(cpg);
				// this bytecode might only create some dimensions of the created array type 
				int createdDimensions = multianewarray.getDimensions();
				//TODO exception if createdDimensions <= 0 ?
				Type[] args = IntStream.range(0, createdDimensions)
					.mapToObj(dim -> Type.INT)
					.toArray(Type[]::new);
				String allocatorName = getNewNameForPrivateMethod(EXTRA_ALLOCATOR_NAME);
				InstructionList allocatorIl = new InstructionList();
				IntStream.range(0, createdDimensions)
					.mapToObj(dim -> InstructionFactory.createLoad(Type.INT, dim))
					.forEach(allocatorIl::append);

				// the allocation is moved into the allocator method
				allocatorIl.append(multianewarray);
				allocatorIl.append(InstructionConst.ARETURN);

				// this is where to jump to create the array
				InstructionHandle creation = allocatorIl.getStart();

				// where to jump if the last dimension is negative: of course this will lead to a run-time exception
				// since dimensions of multianewarray must be non-negative
				InstructionHandle fallBack = allocatorIl.insert(InstructionConst.POP2);

				// where to jump if a dimension is negative: of course this will lead to a run-time exception
				// since dimensions of multianewarray must be non-negative
				allocatorIl.insert(InstructionFactory.createBranchInstruction(Const.GOTO, creation));
				allocatorIl.insert(InstructionConst.POP2);
				InstructionHandle fallBack2 = allocatorIl.insert(InstructionConst.POP);

				String bigInteger = BigInteger.class.getName();
				InvokeInstruction valueOf = factory.createInvoke(bigInteger, "valueOf", BIGINTEGER_OT, ONE_LONG_ARGS, Const.INVOKESTATIC);
				InvokeInstruction multiply = factory.createInvoke(bigInteger, "multiply", BIGINTEGER_OT, ONE_BIGINTEGER_ARGS, Const.INVOKEVIRTUAL);
				InvokeInstruction add = factory.createInvoke(bigInteger, "add", BIGINTEGER_OT, ONE_BIGINTEGER_ARGS, Const.INVOKEVIRTUAL);

				// we start from 1
				allocatorIl.insert(fallBack2, factory.createGetStatic(bigInteger, "ONE", BIGINTEGER_OT));

				// we multiply all dimensions but one, computing over BigInteger, to infer the number of arrays that get created
				IntStream.range(0, createdDimensions - 1).forEach(dimension -> {
					allocatorIl.insert(fallBack2, InstructionFactory.createLoad(Type.INT, dimension));
					allocatorIl.insert(fallBack2, InstructionConst.DUP);
					allocatorIl.insert(fallBack2, InstructionFactory.createBranchInstruction(Const.IFLT, fallBack));
					allocatorIl.insert(fallBack2, InstructionConst.I2L);
					allocatorIl.insert(fallBack2, valueOf);
					allocatorIl.insert(fallBack2, multiply);
				});

				// the number of arrays is duplicated and left below the stack, adding a unit for the main array
				// and multiplying for the cost of a single array
				allocatorIl.insert(fallBack2, InstructionConst.DUP);
				allocatorIl.insert(fallBack2, factory.createGetStatic(bigInteger, "ONE", BIGINTEGER_OT));
				allocatorIl.insert(fallBack2, add);
				allocatorIl.insert(fallBack2, factory.createConstant((long) GasCosts.RAM_COST_PER_ARRAY));
				allocatorIl.insert(fallBack2, valueOf);	
				allocatorIl.insert(fallBack2, multiply);
				allocatorIl.insert(fallBack2, InstructionConst.SWAP);

				// the last dimension is computed apart, since it contributes to the elements only,
				// but not to the number of created arrays
				allocatorIl.insert(fallBack2, InstructionFactory.createLoad(Type.INT, createdDimensions - 1));
				allocatorIl.insert(fallBack2, InstructionConst.DUP);
				allocatorIl.insert(fallBack2, InstructionFactory.createBranchInstruction(Const.IFLT, fallBack2));
				allocatorIl.insert(fallBack2, InstructionConst.I2L);
				allocatorIl.insert(fallBack2, valueOf);
				allocatorIl.insert(fallBack2, multiply);

				// we multiply the number of elements for the RAM cost of a single element
				allocatorIl.insert(fallBack2, factory.createConstant((long) GasCosts.RAM_COST_PER_ARRAY_SLOT));
				allocatorIl.insert(fallBack2, valueOf);	
				allocatorIl.insert(fallBack2, multiply);

				// we add the cost of the arrays
				allocatorIl.insert(fallBack2, add);

				// we charge the gas
				allocatorIl.insert(fallBack2, factory.createInvoke(TAKAMAKA_CLASS_NAME, "chargeForRAM", Type.VOID, ONE_BIGINTEGER_ARGS, Const.INVOKESTATIC));
				allocatorIl.insert(fallBack2, InstructionFactory.createBranchInstruction(Const.GOTO, creation));

				MethodGen allocator = new MethodGen(PRIVATE_SYNTHETIC_STATIC, createdType, args, null, allocatorName, className, allocatorIl, cpg);

				allocatorIl.setPositions();
				allocator.setMaxLocals();
				allocator.setMaxStack();
				StackMapReplacer.replace(allocator);
				clazz.addMethod(allocator.getMethod());

				// the original multianewarray gets replaced with a call to the allocation method
				ih.setInstruction(factory.createInvoke(className, allocatorName, createdType, args, Const.INVOKESTATIC));
			}
		}

		private long numberOfInstanceFieldsOf(ObjectType type) {
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				long size = 0L;
				for (Class<?> clazz = classLoader.loadClass(type.getClassName()); clazz != Object.class; clazz = clazz.getSuperclass())
					size += Stream.of(clazz.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).count();

				return size;
			});
		}

		private void addCpuGasUpdate(InstructionHandle dominator, InstructionList il, CodeExceptionGen[] ceg, SortedSet<InstructionHandle> dominators) {
			long cost = cpuCostOf(dominator, dominators);
			InstructionHandle newTarget;

			// up to this value, there is a special compact method for charging gas
			if (cost <= Takamaka.MAX_COMPACT)
				newTarget = il.insert(dominator, factory.createInvoke(TAKAMAKA_CLASS_NAME, "charge" + cost, Type.VOID, Type.NO_ARGS, Const.INVOKESTATIC));
			else {
				newTarget = il.insert(dominator, createConstantPusher(cost));

				il.insert(dominator, chargeCall(cost, "charge"));
			}

			il.redirectBranches(dominator, newTarget);
			il.redirectExceptionHandlers(ceg, dominator, newTarget);
		}

		private InvokeInstruction chargeCall(long value, String name) {
			return factory.createInvoke(TAKAMAKA_CLASS_NAME, name, Type.VOID, value < Integer.MAX_VALUE ? ONE_INT_ARGS : ONE_LONG_ARGS, Const.INVOKESTATIC);
		}

		private Instruction createConstantPusher(long value) {
			// we determine if we can use an integer or we need a long (highly unlikely...)
			if (value < Integer.MAX_VALUE)
				return factory.createConstant((int) value);
			else
				return factory.createConstant(value);
		}

		private long cpuCostOf(InstructionHandle dominator, SortedSet<InstructionHandle> dominators) {
			long cost = 0L;

			InstructionHandle cursor = dominator;
			do {
				cost += GasCosts.cpuCostOf(cursor.getInstruction());
				cursor = cursor.getNext();
			}
			while (cursor != null && !dominators.contains(cursor));

			return cost;
		}

		/**
		 * Computes the set of dominators of the given method or constructor, that is,
		 * the instructions where basic blocks start.
		 * 
		 * @param method the method whose dominators must be computed
		 * @return the set of dominators, ordered in increasing position
		 */
		private SortedSet<InstructionHandle> computeDominators(MethodGen method) {
			return StreamSupport.stream(method.getInstructionList().spliterator(), false).filter(this::isDominator)
					.collect(Collectors.toCollection(() -> new TreeSet<InstructionHandle>(
							Comparator.comparing(InstructionHandle::getPosition))));
		}

		private boolean isDominator(InstructionHandle ih) {
			InstructionHandle prev = ih.getPrev();
			// the first instruction is a dominator
			return prev == null || prev.getInstruction() instanceof BranchInstruction
					|| prev.getInstruction() instanceof ExceptionThrower || Stream.of(ih.getTargeters()).anyMatch(
							targeter -> targeter instanceof BranchInstruction || targeter instanceof CodeExceptionGen);
		}

		/**
		 * Passes the trailing implicit parameters to calls to entries. They are the
		 * contract where the entry is called and {@code null} (for the dummy argument).
		 * 
		 * @param method the method
		 */
		private void addContractToCallsToEntries(MethodGen method) {
			if (!method.isAbstract()) {
				InstructionList il = method.getInstructionList();
				List<InstructionHandle> callsToEntries = StreamSupport.stream(il.spliterator(), false)
						.filter(ih -> isCallToEntry(ih.getInstruction())).collect(Collectors.toList());

				for (InstructionHandle ih : callsToEntries)
					passContractToCallToEntry(il, ih, method.getName());
			}
		}

		/**
		 * Passes the trailing implicit parameters to the given call to an entry. They
		 * are the contract where the entry is called and {@code null} (for the dummy
		 * argument).
		 * 
		 * @param il     the instructions of the method being instrumented
		 * @param ih     the call to the entry
		 * @param callee the name of the method where the calls are being looked for
		 */
		private void passContractToCallToEntry(InstructionList il, InstructionHandle ih, String callee) {
			InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
			if (invoke instanceof INVOKEDYNAMIC) {
				INVOKEDYNAMIC invokedynamic = (INVOKEDYNAMIC) invoke;
				String methodName = invoke.getMethodName(cpg);
				ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());

				// this is an invokedynamic that calls an entry: we must capture the calling contract
				Type[] args = invoke.getArgumentTypes(cpg);
				Type[] expandedArgs = new Type[args.length + 1];
				System.arraycopy(args, 0, expandedArgs, 1, args.length);
				expandedArgs[0] = new ObjectType(className);
				ConstantInvokeDynamic expandedCid = new ConstantInvokeDynamic(cid.getBootstrapMethodAttrIndex(), cpg
						.addNameAndType(methodName, Type.getMethodSignature(invoke.getReturnType(cpg), expandedArgs)));
				int index = addInvokeDynamicToConstantPool(expandedCid);
				INVOKEDYNAMIC copied = (INVOKEDYNAMIC) invokedynamic.copy();
				copied.setIndex(index);
				ih.setInstruction(copied);

				int slots = Stream.of(args).mapToInt(Type::getSize).sum();
				forEachPusher(ih, slots, where -> {
					il.append(where, where.getInstruction());
					where.setInstruction(InstructionConst.ALOAD_0);
				}, () -> {
					throw new IllegalStateException("Cannot find stack pushers for calls inside " + callee);
				});
			} else {
				Type[] args = invoke.getArgumentTypes(cpg);
				Type[] expandedArgs = new Type[args.length + 2];
				System.arraycopy(args, 0, expandedArgs, 0, args.length);
				expandedArgs[args.length] = CONTRACT_OT;
				expandedArgs[args.length + 1] = DUMMY_OT;

				ih.setInstruction(InstructionConst.ALOAD_0); // the call must be inside a contract "this"
				il.append(ih, factory.createInvoke(invoke.getClassName(cpg), invoke.getMethodName(cpg),
						invoke.getReturnType(cpg), expandedArgs, invoke.getOpcode()));
				il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
			}
		}

		/**
		 * Finds the closest instructions whose stack height, at their beginning, is
		 * equal to the height of the stack at {@code ih} minus {@code slots}.
		 * 
		 * @param ih    the start instruction of the look up
		 * @param slots the difference in stack height
		 */
		private void forEachPusher(InstructionHandle ih, int slots, Consumer<InstructionHandle> what,
				Runnable ifCannotFollow) {
			Set<HeightAtBytecode> seen = new HashSet<>();
			List<HeightAtBytecode> workingSet = new ArrayList<>();
			HeightAtBytecode start = new HeightAtBytecode(ih, slots);
			workingSet.add(start);
			seen.add(start);

			do {
				HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
				InstructionHandle currentIh = current.ih;
				if (current.stackHeightBeforeBytecode <= 0)
					what.accept(currentIh);
				else {
					InstructionHandle previous = currentIh.getPrev();
					if (previous != null) {
						Instruction previousIns = previous.getInstruction();
						if (!(previousIns instanceof ReturnInstruction) && !(previousIns instanceof ATHROW)
								&& !(previousIns instanceof GotoInstruction)) {
							// we proceed with previous
							int stackHeightBefore = current.stackHeightBeforeBytecode;
							stackHeightBefore -= previousIns.produceStack(cpg);
							stackHeightBefore += previousIns.consumeStack(cpg);

							HeightAtBytecode added = new HeightAtBytecode(previous, stackHeightBefore);
							if (seen.add(added))
								workingSet.add(added);
						}
					}

					// we proceed with the instructions that jump at currentIh
					InstructionTargeter[] targeters = currentIh.getTargeters();
					if (Stream.of(targeters).anyMatch(targeter -> targeter instanceof CodeExceptionGen))
						ifCannotFollow.run();

					Stream.of(targeters).filter(targeter -> targeter instanceof BranchInstruction)
							.map(targeter -> (BranchInstruction) targeter).forEach(branch -> {
								int stackHeightBefore = current.stackHeightBeforeBytecode;
								stackHeightBefore -= branch.produceStack(cpg);
								stackHeightBefore += branch.consumeStack(cpg);

								HeightAtBytecode added = new HeightAtBytecode(previous, stackHeightBefore);
								if (seen.add(added))
									workingSet.add(added);
							});
				}
			} while (!workingSet.isEmpty());
		}

		/**
		 * Determines if the given instruction calls an entry.
		 * 
		 * @param instruction the instruction
		 * @return true if and only if that condition holds
		 */
		private boolean isCallToEntry(Instruction instruction) {
			if (instruction instanceof INVOKEDYNAMIC)
				return bootstrapMethodsThatWillRequireExtraThis
					.contains(clazz.bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction));
			else if (instruction instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) instruction;
				ReferenceType receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType)
					return clazz.annotations.isEntryPossiblyAlreadyInstrumented(((ObjectType) receiver).getClassName(),
						invoke.getMethodName(cpg), invoke.getSignature(cpg));
			}

			return false;
		}

		/**
		 * Instruments an entry, by setting the caller and transferring funds for
		 * payable entries.
		 * 
		 * @param method         the entry
		 * @param callerContract the class of the caller contract
		 * @param isPayable      true if and only if the entry is payable
		 */
		private void instrumentEntry(MethodGen method, Class<?> callerContract, boolean isPayable) {
			// slotForCaller is the local variable used for the extra "caller" parameter;
			// there is no need to shift the local variables one slot up, since the use
			// of caller is limited to the prolog of the synthetic code
			int slotForCaller = addExtraParameters(method);
			if (!method.isAbstract())
				setCallerAndBalance(method, callerContract, slotForCaller, isPayable);
		}

		/**
		 * Instruments an entry by calling the contract method that sets caller and
		 * balance.
		 * 
		 * @param method         the entry
		 * @param callerContract the class of the caller contract
		 * @param slotForCaller  the local variable for the caller implicit argument
		 * @param isPayable      true if and only if the entry is payable
		 */
		private void setCallerAndBalance(MethodGen method, Class<?> callerContract, int slotForCaller,
				boolean isPayable) {
			InstructionList il = method.getInstructionList();

			// the call to the method that sets caller and balance cannot be put at the
			// beginning of the method, always: for constructors, Java bytecode requires
			// that their code starts with a call to a constructor of the superclass
			InstructionHandle where = determineWhereToSetCallerAndBalance(il, method, slotForCaller);
			InstructionHandle start = il.getStart();

			il.insert(start, InstructionFactory.createThis());
			il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
			if (callerContract != classLoader.contractClass)
				il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
			if (isPayable) {
				// a payable entry method can have a first argument of type int/long/BigInteger
				Type amountType = method.getArgumentType(0);
				il.insert(start, InstructionFactory.createLoad(amountType, 1));
				Type[] paybleEntryArgs = new Type[] { CONTRACT_OT, amountType };
				il.insert(where, factory.createInvoke(className, PAYABLE_ENTRY, Type.VOID, paybleEntryArgs,
						Const.INVOKESPECIAL));
			} else
				il.insert(where, factory.createInvoke(className, ENTRY, Type.VOID, ENTRY_ARGS, Const.INVOKESPECIAL));
		}

		/**
		 * Entries call {@link takamaka.lang.Contract#entry(Contract)} or
		 * {@link takamaka.lang.Contract#payableEntry(Contract,BigInteger)} at their
		 * beginning, to set the caller and the balance of the called entry. In general,
		 * such call can be placed at the very beginning of the code. The only problem
		 * is related to constructors, that require their code to start with a call to a
		 * constructor of their superclass. In that case, this method finds the place
		 * where that contractor of the superclass is called: after which, we can add
		 * the call that sets caller and balance.
		 * 
		 * @param il            the list of instructions of the entry
		 * @param method        the entry
		 * @param slotForCaller the local where the caller contract is passed to the
		 *                      entry
		 * @return the instruction before which the code that sets caller and balance
		 *         can be placed
		 */
		private InstructionHandle determineWhereToSetCallerAndBalance(InstructionList il, MethodGen method,
				int slotForCaller) {
			InstructionHandle start = il.getStart();

			if (method.getName().equals(Const.CONSTRUCTOR_NAME)) {
				// we have to identify the call to the constructor of the superclass:
				// the code of a constructor normally starts with an aload_0 whose value is
				// consumed
				// by a call to a constructor of the superclass. In the middle, slotForCaller is
				// not expected
				// to be modified. Note that this is the normal situation, as results from a
				// normal
				// Java compiler. In principle, the Java bytecode might instead do very weird
				// things,
				// including calling two constructors of the superclass at different places. In
				// all such cases
				// this method fails and rejects the code: such non-standard code is not
				// supported by Takamaka
				Instruction startInstruction = start.getInstruction();
				if (startInstruction.getOpcode() == Const.ALOAD_0 || (startInstruction.getOpcode() == Const.ALOAD
						&& ((LoadInstruction) startInstruction).getIndex() == 0)) {
					Set<InstructionHandle> callsToConstructorsOfSuperclass = new HashSet<>();

					HeightAtBytecode seed = new HeightAtBytecode(start.getNext(), 1);
					Set<HeightAtBytecode> seen = new HashSet<>();
					seen.add(seed);
					List<HeightAtBytecode> workingSet = new ArrayList<>();
					workingSet.add(seed);

					do {
						HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
						int stackHeightAfterBytecode = current.stackHeightBeforeBytecode;
						Instruction bytecode = current.ih.getInstruction();

						if (bytecode instanceof StoreInstruction) {
							int modifiedLocal = ((StoreInstruction) bytecode).getIndex();
							int size = ((StoreInstruction) bytecode).getType(cpg).getSize();
							if (modifiedLocal == slotForCaller || (size == 2 && modifiedLocal == slotForCaller - 1))
								throw new IllegalStateException("Unexpected modification of local " + slotForCaller
										+ " before initialization of " + className);
						}

						if (bytecode instanceof StackProducer)
							stackHeightAfterBytecode += ((StackProducer) bytecode).produceStack(cpg);
						if (bytecode instanceof StackConsumer)
							stackHeightAfterBytecode -= ((StackConsumer) bytecode).consumeStack(cpg);

						if (stackHeightAfterBytecode == 0) {
							// found a consumer of the aload_0: is it really a call to a constructor of the
							// superclass?
							if (bytecode instanceof INVOKESPECIAL
									&& ((INVOKESPECIAL) bytecode).getClassName(cpg).equals(clazz.getSuperclassName())
									&& ((INVOKESPECIAL) bytecode).getMethodName(cpg).equals(Const.CONSTRUCTOR_NAME))
								callsToConstructorsOfSuperclass.add(current.ih);
							else
								throw new IllegalStateException("Unexpected consumer of local 0 " + bytecode
										+ " before initialization of " + className);
						} else if (bytecode instanceof GotoInstruction) {
							HeightAtBytecode added = new HeightAtBytecode(((GotoInstruction) bytecode).getTarget(),
									stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
						} else if (bytecode instanceof IfInstruction) {
							HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(),
									stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
							added = new HeightAtBytecode(((IfInstruction) bytecode).getTarget(),
									stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
						} else if (bytecode instanceof BranchInstruction || bytecode instanceof ATHROW
								|| bytecode instanceof RETURN || bytecode instanceof RET)
							throw new IllegalStateException(
									"Unexpected instruction " + bytecode + " before initialization of " + className);
						else {
							HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(),
									stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
						}
					} while (!workingSet.isEmpty());

					if (callsToConstructorsOfSuperclass.size() == 1)
						return callsToConstructorsOfSuperclass.iterator().next().getNext();
					else
						throw new IllegalStateException(
								"Cannot identify single call to constructor of superclass inside a constructor ot "
										+ className);
				} else
					throw new IllegalStateException("Constructor of " + className + " does not start with aload 0");
			} else
				return start;
		}

		/**
		 * Adds an extra caller parameter to the given entry.
		 * 
		 * @param method the entry
		 * @return the local variable used for the extra parameter
		 */
		private int addExtraParameters(MethodGen method) {
			List<Type> args = new ArrayList<>();
			int slotsForParameters = 0;
			for (Type arg : method.getArgumentTypes()) {
				args.add(arg);
				slotsForParameters += arg.getSize();
			}
			args.add(CONTRACT_OT);
			args.add(DUMMY_OT); // to avoid name clashes after the addition
			method.setArgumentTypes(args.toArray(Type.NO_ARGS));

			String[] names = method.getArgumentNames();
			if (names != null) {
				List<String> namesAsList = new ArrayList<>();
				for (String name : names)
					namesAsList.add(name);
				namesAsList.add("caller");
				namesAsList.add("unused");
				method.setArgumentNames(namesAsList.toArray(new String[namesAsList.size()]));
			}

			return slotsForParameters + 1;
		}

		/**
		 * Replaces accesses to fields of storage classes with calls to accessor
		 * methods.
		 * 
		 * @param method the method where the replacement occurs
		 */
		private void replaceFieldAccessesWithAccessors(MethodGen method) {
			if (!method.isAbstract()) {
				InstructionList il = method.getInstructionList();
				StreamSupport.stream(il.spliterator(), false).filter(this::isAccessToLazilyLoadedFieldInStorageClass)
						.forEach(ih -> ih.setInstruction(accessorCorrespondingTo((FieldInstruction) ih.getInstruction())));
			}
		}

		/**
		 * Yields the accessor call corresponding to the access to the given field.
		 * 
		 * @param fieldInstruction the field access instruction
		 * @return the corresponding accessor call instruction
		 */
		private Instruction accessorCorrespondingTo(FieldInstruction fieldInstruction) {
			ObjectType referencedClass = (ObjectType) fieldInstruction.getReferenceType(cpg);
			Type fieldType = fieldInstruction.getFieldType(cpg);
			String fieldName = fieldInstruction.getFieldName(cpg);

			if (fieldInstruction instanceof GETFIELD)
				return factory.createInvoke(referencedClass.getClassName(),
						getterNameFor(referencedClass.getClassName(), fieldName), fieldType, Type.NO_ARGS,
						Const.INVOKEVIRTUAL);
			else // PUTFIELD
				return factory.createInvoke(referencedClass.getClassName(),
						setterNameFor(referencedClass.getClassName(), fieldName), Type.VOID, new Type[] { fieldType },
						Const.INVOKEVIRTUAL);
		}

		/**
		 * Determines if the given instruction is an access to a field of a storage
		 * class that is lazily loaded.
		 * 
		 * @param ih the instruction
		 * @return true if and only if that condition holds
		 */
		private boolean isAccessToLazilyLoadedFieldInStorageClass(InstructionHandle ih) {
			Instruction instruction = ih.getInstruction();

			if (instruction instanceof GETFIELD) {
				FieldInstruction fi = (FieldInstruction) instruction;
				ObjectType receiverType = (ObjectType) fi.getReferenceType(cpg);
				String receiverClassName = receiverType.getClassName();
				Class<?> fieldType;
				return classLoader.isStorage(receiverClassName)
						&& classLoader.isLazilyLoaded(fieldType = classLoader.bcelToClass(fi.getFieldType(cpg)))
						&& !isTransient(receiverClassName, fi.getFieldName(cpg), fieldType);
			} else if (instruction instanceof PUTFIELD) {
				FieldInstruction fi = (FieldInstruction) instruction;
				ObjectType receiverType = (ObjectType) fi.getReferenceType(cpg);
				String receiverClassName = receiverType.getClassName();
				Class<?> fieldType;
				return classLoader.isStorage(receiverClassName)
						&& classLoader.isLazilyLoaded(fieldType = classLoader.bcelToClass(fi.getFieldType(cpg)))
						&& !isTransientOrFinal(receiverClassName, fi.getFieldName(cpg), fieldType);
			} else
				return false;
		}

		/**
		 * Determines if an instance field of a storage class is transient.
		 * 
		 * @param className the class from which the field must be looked-up. This is guaranteed to be a storage class
		 * @param fieldName the name of the field
		 * @param fieldType the type of the field
		 * @return true if and only if that condition holds
		 */
		private boolean isTransient(String className, String fieldName, Class<?> fieldType) {
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				Class<?> clazz = classLoader.loadClass(className);
				
				do {
					Optional<Field> match = Stream.of(clazz.getDeclaredFields())
						.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
						.findFirst();

					if (match.isPresent())
						return Modifier.isTransient(match.get().getModifiers());

					clazz = clazz.getSuperclass();
				}
				while (clazz != classLoader.storageClass && clazz != classLoader.contractClass);

				return false;
			});
		}

		/**
		 * Determines if an instance field of a storage class is transient or final.
		 * 
		 * @param className the class from which the field must be looked-up. This is guaranteed to be a storage class
		 * @param fieldName the name of the field
		 * @param fieldType the type of the field
		 * @return true if and only if that condition holds
		 */
		private boolean isTransientOrFinal(String className, String fieldName, Class<?> fieldType) {
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				Class<?> clazz = classLoader.loadClass(className);
				
				do {
					Optional<Field> match = Stream.of(clazz.getDeclaredFields())
						.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
						.findFirst();

					if (match.isPresent()) {
						int modifiers = match.get().getModifiers();
						return Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers);
					}

					clazz = clazz.getSuperclass();
				}
				while (clazz != classLoader.storageClass && clazz != classLoader.contractClass);

				return false;
			});
		}

		/**
		 * Adds, to a storage class, the method that extract all updates to an instance
		 * of a class, since the beginning of a transaction.
		 */
		private void addExtractUpdates() {
			if (eagerNonTransientInstanceFields.getLast().isEmpty() && lazyNonTransientInstanceFields.isEmpty())
				return;

			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(InstructionConst.ALOAD_1);
			il.append(InstructionConst.ALOAD_2);
			il.append(InstructionFactory.createLoad(LIST_OT, 3));
			il.append(factory.createInvoke(clazz.getSuperclassName(), EXTRACT_UPDATES, Type.VOID,
					EXTRACT_UPDATES_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(STORAGE_CLASS_NAME, IN_STORAGE_NAME, Type.BOOLEAN));
			il.append(InstructionFactory.createStore(Type.BOOLEAN, 4));

			InstructionHandle end = il.append(InstructionConst.RETURN);

			for (Field field : eagerNonTransientInstanceFields.getLast())
				end = addUpdateExtractionForEagerField(field, il, end);

			for (Field field : lazyNonTransientInstanceFields)
				end = addUpdateExtractionForLazyField(field, il, end);

			MethodGen extractUpdates = new MethodGen(PROTECTED_SYNTHETIC, Type.VOID, EXTRACT_UPDATES_ARGS, null, EXTRACT_UPDATES, className, il, cpg);
			il.setPositions();
			extractUpdates.setMaxLocals();
			extractUpdates.setMaxStack();
			StackMapReplacer.replace(extractUpdates);
			clazz.addMethod(extractUpdates.getMethod());
		}

		/**
		 * Adds the code that check if a given lazy field has been updated since the
		 * beginning of a transaction and, in such a case, adds the corresponding
		 * update.
		 * 
		 * @param field the field
		 * @param il    the instruction list where the code must be added
		 * @param end   the instruction before which the extra code must be added
		 * @return the beginning of the added code
		 */
		private InstructionHandle addUpdateExtractionForLazyField(Field field, InstructionList il,
				InstructionHandle end) {
			Type type = Type.getType(field.getType());

			List<Type> args = new ArrayList<>();
			for (Type arg : ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			args.add(SET_OT);
			args.add(LIST_OT);
			args.add(ObjectType.STRING);
			args.add(ObjectType.OBJECT);

			InstructionHandle recursiveExtract;
			// we deal with special cases where the call to a recursive extract is useless:
			// this is just an optimization
			String fieldName = field.getName();
			if (field.getType() == String.class || field.getType() == BigInteger.class)
				recursiveExtract = end;
			else {
				recursiveExtract = il.insert(end, InstructionFactory.createThis());
				il.insert(end, InstructionConst.DUP);
				il.insert(end, factory.createGetField(className, OLD_PREFIX + fieldName, type));
				il.insert(end, InstructionConst.ALOAD_1);
				il.insert(end, InstructionConst.ALOAD_2);
				il.insert(end, InstructionFactory.createLoad(LIST_OT, 3));
				il.insert(end, factory.createInvoke(STORAGE_CLASS_NAME, RECURSIVE_EXTRACT, Type.VOID,
						RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
			}

			InstructionHandle addUpdatesFor = il.insert(recursiveExtract, InstructionFactory.createThis());
			il.insert(recursiveExtract, factory.createConstant(className));
			il.insert(recursiveExtract, factory.createConstant(fieldName));
			il.insert(recursiveExtract, InstructionConst.ALOAD_1);
			il.insert(recursiveExtract, InstructionConst.ALOAD_2);
			il.insert(recursiveExtract, InstructionFactory.createLoad(LIST_OT, 3));
			il.insert(recursiveExtract, factory.createConstant(field.getType().getName()));
			il.insert(recursiveExtract, InstructionFactory.createThis());
			il.insert(recursiveExtract, factory.createGetField(className, fieldName, type));
			il.insert(recursiveExtract, factory.createInvoke(STORAGE_CLASS_NAME, ADD_UPDATE_FOR, Type.VOID,
					args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, fieldName, type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + fieldName, type));

			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, recursiveExtract));

			return start;
		}

		/**
		 * Adds the code that check if a given eager field has been updated since the
		 * beginning of a transaction and, in such a case, adds the corresponding
		 * update.
		 * 
		 * @param field the field
		 * @param il    the instruction list where the code must be added
		 * @param end   the instruction before which the extra code must be added
		 * @return the beginning of the added code
		 */
		private InstructionHandle addUpdateExtractionForEagerField(Field field, InstructionList il,
				InstructionHandle end) {
			Class<?> fieldType = field.getType();
			Type type = Type.getType(fieldType);
			boolean isEnum = fieldType.isEnum();

			List<Type> args = new ArrayList<>();
			for (Type arg : ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			if (isEnum) {
				args.add(ObjectType.STRING);
				args.add(ENUM_OT);
			} else
				args.add(type);

			InstructionHandle addUpdatesFor = il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createConstant(className));
			il.insert(end, factory.createConstant(field.getName()));
			il.insert(end, InstructionConst.ALOAD_1);
			if (isEnum)
				il.insert(end, factory.createConstant(fieldType.getName()));
			il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createGetField(className, field.getName(), type));
			il.insert(end, factory.createInvoke(STORAGE_CLASS_NAME, ADD_UPDATE_FOR, Type.VOID,
					args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			if (fieldType == double.class) {
				il.insert(addUpdatesFor, InstructionConst.DCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			} else if (fieldType == float.class) {
				il.insert(addUpdatesFor, InstructionConst.FCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			} else if (fieldType == long.class) {
				il.insert(addUpdatesFor, InstructionConst.LCMP);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			} else if (fieldType == String.class || fieldType == BigInteger.class) {
				// comparing strings or BigInteger with their previous value is done by checking
				// if they
				// are equals rather than ==. This is just an optimization, to avoid storing an
				// equivalent value
				// as an update. It is relevant for the balance fields of contracts, that might
				// reach 0 at the
				// end of a transaction, as it was at the beginning, but has fluctuated during
				// the
				// transaction: it is useless to add an update for it
				il.insert(addUpdatesFor, factory.createInvoke("java.util.Objects", "equals", Type.BOOLEAN,
						TWO_OBJECTS_ARGS, Const.INVOKESTATIC));
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFNE, end));
			} else if (!fieldType.isPrimitive())
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, end));
			else
				// this covers int, short, byte, char, boolean
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ICMPEQ, end));

			return start;
		}

		/**
		 * Adds accessor methods for the fields of the class being instrumented.
		 */
		private void addAccessorMethods() {
			lazyNonTransientInstanceFields.forEach(this::addAccessorMethodsFor);
		}

		/**
		 * Adds accessor methods for the given field.
		 * 
		 * @param field the field
		 */
		private void addAccessorMethodsFor(Field field) {
			addGetterFor(field);

			if (!Modifier.isFinal(field.getModifiers()))
				addSetterFor(field);
		}

		/**
		 * Adds a setter method for the given field.
		 * 
		 * @param field the field
		 */
		private void addSetterFor(Field field) {
			Type type = Type.getType(field.getType());
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID,
					Type.NO_ARGS, Const.INVOKESPECIAL));
			il.append(InstructionConst.ALOAD_1);
			il.append(factory.createPutField(className, field.getName(), type));
			il.append(InstructionConst.RETURN);

			MethodGen setter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, BasicType.VOID, new Type[] { type }, null,
					setterNameFor(className, field.getName()), className, il, cpg);
			setter.setMaxLocals();
			setter.setMaxStack();
			clazz.addMethod(setter.getMethod());
		}

		/**
		 * Adds a getter method for the given field.
		 * 
		 * @param field the field
		 */
		private void addGetterFor(Field field) {
			Type type = Type.getType(field.getType());
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID,
					Type.NO_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(className, field.getName(), type));
			il.append(InstructionFactory.createReturn(type));

			MethodGen getter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, type, Type.NO_ARGS, null,
					getterNameFor(className, field.getName()), className, il, cpg);
			getter.setMaxLocals();
			getter.setMaxStack();
			clazz.addMethod(getter.getMethod());
		}

		private String getterNameFor(String className, String fieldName) {
			// we use the class name as well, in order to disambiguate fields with the same
			// name
			// in sub and superclass
			return GETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
		}

		private String setterNameFor(String className, String fieldName) {
			// we use the class name as well, in order to disambiguate fields with the same
			// name
			// in sub and superclass
			return SETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
		}

		/**
		 * Adds the ensure loaded methods for the lazy fields of the class being
		 * instrumented.
		 */
		private void addEnsureLoadedMethods() {
			lazyNonTransientInstanceFields.forEach(this::addEnsureLoadedMethodFor);
		}

		/**
		 * Adds the ensure loaded method for the given lazy field.
		 */
		private void addEnsureLoadedMethodFor(Field field) {
			boolean fieldIsFinal = Modifier.isFinal(field.getModifiers());

			// final fields cannot remain as such, since the ensureMethod will update them
			// and it is not a constructor. Java < 9 will not check this constraint but
			// newer versions of Java would reject the code without this change
			if (fieldIsFinal) {
				org.apache.bcel.classfile.Field oldField = Stream.of(clazz.getFields()).filter(
						f -> f.getName().equals(field.getName()) && f.getType().equals(Type.getType(field.getType())))
						.findFirst().get();
				FieldGen newField = new FieldGen(oldField, cpg);
				newField.setAccessFlags(oldField.getAccessFlags() ^ Const.ACC_FINAL);
				clazz.replaceField(oldField, newField.getField());
			}

			Type type = Type.getType(field.getType());
			InstructionList il = new InstructionList();
			InstructionHandle _return = il.append(InstructionConst.RETURN);
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(STORAGE_CLASS_NAME, IN_STORAGE_NAME, BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFEQ, _return));
			il.insert(_return, InstructionFactory.createThis());
			String fieldName = field.getName();
			il.insert(_return,
					factory.createGetField(className, IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFNE, _return));
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.ICONST_1);
			il.insert(_return,
					factory.createPutField(className, IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
			il.insert(_return, factory.createConstant(className));
			il.insert(_return, factory.createConstant(fieldName));
			il.insert(_return, factory.createConstant(field.getType().getName()));
			il.insert(_return,
					factory.createInvoke(className,
							fieldIsFinal ? DESERIALIZE_LAST_UPDATE_FOR_FINAL : DESERIALIZE_LAST_UPDATE_FOR,
							ObjectType.OBJECT, THREE_STRINGS_ARGS, Const.INVOKEVIRTUAL));
			il.insert(_return, factory.createCast(ObjectType.OBJECT, type));
			il.insert(_return, InstructionConst.DUP2);
			il.insert(_return, factory.createPutField(className, fieldName, type));
			il.insert(_return, factory.createPutField(className, OLD_PREFIX + fieldName, type));
			il.setPositions();

			MethodGen ensureLoaded = new MethodGen(PRIVATE_SYNTHETIC, BasicType.VOID, Type.NO_ARGS, null,
					ENSURE_LOADED_PREFIX + fieldName, className, il, cpg);
			ensureLoaded.setMaxLocals();
			ensureLoaded.setMaxStack();
			StackMapReplacer.replace(ensureLoaded);
			clazz.addMethod(ensureLoaded.getMethod());
		}

		/**
		 * Adds fields for the old value and the loading state of the fields of a
		 * storage class.
		 */
		private void addOldAndIfAlreadyLoadedFields() {
			eagerNonTransientInstanceFields.getLast().forEach(this::addOldFieldFor);

			for (Field field : lazyNonTransientInstanceFields) {
				addOldFieldFor(field);
				addIfAlreadyLoadedFieldFor(field);
			}
		}

		/**
		 * Adds the field for the loading state of the fields of a storage class.
		 */
		private void addIfAlreadyLoadedFieldFor(Field field) {
			clazz.addField(new FieldGen(PRIVATE_SYNTHETIC_TRANSIENT, BasicType.BOOLEAN,
					IF_ALREADY_LOADED_PREFIX + field.getName(), cpg).getField());
		}

		/**
		 * Adds the field for the old value of the fields of a storage class.
		 */
		private void addOldFieldFor(Field field) {
			clazz.addField(new FieldGen(PRIVATE_SYNTHETIC_TRANSIENT, Type.getType(field.getType()),
					OLD_PREFIX + field.getName(), cpg).getField());
		}

		/**
		 * Adds a constructor that deserializes an object of storage type. This
		 * constructor receives the values of the eager fields, ordered by putting first
		 * the fields of the superclasses, then those of the same class being
		 * constructed, ordered by name and then by {@code toString()} of their type.
		 */
		private void addConstructorForDeserializationFromBlockchain() {
			List<Type> args = new ArrayList<>();

			// the parameters of the constructor start with a storage reference
			// to the object being deserialized
			args.add(new ObjectType(StorageReferenceAlreadyInBlockchain.class.getName()));

			// then there are the fields of the class and superclasses, with superclasses
			// first
			eagerNonTransientInstanceFields.stream().flatMap(SortedSet::stream).map(Field::getType).map(Type::getType)
					.forEachOrdered(args::add);

			InstructionList il = new InstructionList();
			int nextLocal = addCallToSuper(il);
			addInitializationOfEagerFields(il, nextLocal);
			il.append(InstructionConst.RETURN);

			MethodGen constructor = new MethodGen(PUBLIC_SYNTHETIC, BasicType.VOID, args.toArray(Type.NO_ARGS), null,
					Const.CONSTRUCTOR_NAME, className, il, cpg);
			constructor.setMaxLocals();
			constructor.setMaxStack();
			clazz.addMethod(constructor.getMethod());
		}

		/**
		 * Adds a call from the deserialization constructor of a storage class to the
		 * deserialization constructor of the superclass.
		 * 
		 * @param il the instructions where the call must be added
		 * @return the number of local variables used to accomodate the arguments passed
		 *         to the constructor of the superclass
		 */
		private int addCallToSuper(InstructionList il) {
			List<Type> argsForSuperclasses = new ArrayList<>();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.ALOAD_1);
			argsForSuperclasses.add(new ObjectType(StorageReferenceAlreadyInBlockchain.class.getName()));

			// the fields of the superclasses are passed into a call to super(...)
			class PushLoad implements Consumer<Type> {
				// the first two slots are used for this and the storage reference
				private int local = 2;

				@Override
				public void accept(Type type) {
					argsForSuperclasses.add(type);
					il.append(InstructionFactory.createLoad(type, local));
					local += type.getSize();
				}
			}
			;

			PushLoad pushLoad = new PushLoad();
			eagerNonTransientInstanceFields.stream().limit(eagerNonTransientInstanceFields.size() - 1)
					.flatMap(SortedSet::stream).map(Field::getType).map(Type::getType).forEachOrdered(pushLoad);

			il.append(factory.createInvoke(clazz.getSuperclassName(), Const.CONSTRUCTOR_NAME, BasicType.VOID,
					argsForSuperclasses.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			return pushLoad.local;
		}

		/**
		 * Adds code that initializes the eager fields of the storage class being
		 * instrumented.
		 * 
		 * @param il        the instructions where the code must be added
		 * @param nextLocal the local variables where the parameters start, that must be
		 *                  stored in the fields
		 */
		private void addInitializationOfEagerFields(InstructionList il, int nextLocal) {
			Consumer<Field> putField = new Consumer<Field>() {
				private int local = nextLocal;

				@Override
				public void accept(Field field) {
					Type type = Type.getType(field.getType());
					int size = type.getSize();
					il.append(InstructionFactory.createThis());
					il.append(InstructionFactory.createLoad(type, local));

					// we reduce the size of the code for the frequent case of one slot values
					if (size == 1)
						il.append(InstructionConst.DUP2);
					il.append(factory.createPutField(className, field.getName(), type));
					if (size != 1) {
						il.append(InstructionFactory.createThis());
						il.append(InstructionFactory.createLoad(type, local));
					}
					il.append(factory.createPutField(className, OLD_PREFIX + field.getName(), type));
					local += size;
				}
			};

			eagerNonTransientInstanceFields.getLast().forEach(putField);
		}

		private void collectNonTransientInstanceFieldsOf(Class<?> clazz, boolean firstCall) {
			if (clazz != classLoader.storageClass) {
				// we put at the beginning the fields of the superclasses
				collectNonTransientInstanceFieldsOf(clazz.getSuperclass(), false);

				// then the eager fields of className, in order
				eagerNonTransientInstanceFields.add(Stream.of(clazz.getDeclaredFields())
						.filter(field -> !Modifier.isStatic(field.getModifiers())
								&& !Modifier.isTransient(field.getModifiers())
								&& classLoader.isEagerlyLoaded(field.getType()))
						.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

				// we collect lazy fields as well, but only for the class being instrumented
				if (firstCall)
					Stream.of(clazz.getDeclaredFields())
							.filter(field -> !Modifier.isStatic(field.getModifiers())
									&& !Modifier.isTransient(field.getModifiers())
									&& classLoader.isLazilyLoaded(field.getType()))
							.forEach(lazyNonTransientInstanceFields::add);
			}
		}
	}

	private static class HeightAtBytecode {
		private final InstructionHandle ih;
		private final int stackHeightBeforeBytecode;

		private HeightAtBytecode(InstructionHandle ih, int stackHeightBeforeBytecode) {
			this.ih = ih;
			this.stackHeightBeforeBytecode = stackHeightBeforeBytecode;
		}

		@Override
		public String toString() {
			return ih + " with " + stackHeightBeforeBytecode + " stack elements";
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof HeightAtBytecode && ((HeightAtBytecode) other).ih == ih
					&& ((HeightAtBytecode) other).stackHeightBeforeBytecode == stackHeightBeforeBytecode;
		}

		@Override
		public int hashCode() {
			return ih.getPosition() ^ stackHeightBeforeBytecode;
		}
	}
}