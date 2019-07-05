package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ExceptionThrower;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
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
import org.apache.bcel.generic.MethodGen;
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
import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.Storage;
import takamaka.lang.Takamaka;

/**
 * An instrumenter of a single class file. For instance, it instruments storage classes,
 * by adding the serialization support, and contracts, to deal with entries.
 */
class ClassInstrumentation {
	private final static String OLD_PREFIX = "§old_";
	private final static String IF_ALREADY_LOADED_PREFIX = "§ifAlreadyLoaded_";
	private final static String ENSURE_LOADED_PREFIX = "§ensureLoaded_";
	private final static String GETTER_PREFIX = "§get_";
	private final static String SETTER_PREFIX = "§set_";
	private final static String EXTRA_LAMBDA_PREFIX = "§takamakalambda";
	private final static String EXTRACT_UPDATES = "extractUpdates";
	private final static String RECURSIVE_EXTRACT = "recursiveExtract";
	private final static String ADD_UPDATE_FOR = "addUpdateFor";
	private final static String PAYABLE_ENTRY = "payableEntry";
	private final static String ENTRY = "entry";
	private final static String IN_STORAGE_NAME = "inStorage";
	private final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastLazyUpdateFor";
	private final static String CONTRACT_CLASS_NAME = "takamaka.lang.Contract";
	private final static String TAKAMAKA_CLASS_NAME = Takamaka.class.getName();
	private final static String STORAGE_CLASS_NAME = Storage.class.getName();
	private final static short PUBLIC_SYNTHETIC = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC;
	private final static short PUBLIC_SYNTHETIC_FINAL = PUBLIC_SYNTHETIC | Const.ACC_FINAL;
	private final static short PROTECTED_SYNTHETIC = Const.ACC_PROTECTED | Const.ACC_SYNTHETIC;
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName).thenComparing(field -> field.getType().toString());

	private final static ObjectType CONTRACT_OT = new ObjectType(CONTRACT_CLASS_NAME);
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
	private final static Type[] TWO_OBJECTS_ARGS = { Type.OBJECT, Type.OBJECT };

	/**
	 * Performs the instrumentation of a single class file.
	 * 
	 * @param input the input stream containing the class to instrument
	 * @param className the name of the class
	 * @param instrumentedJar the jar where the instrumented class will be added
	 * @param classLoader the class loader for resolving the classes under instrumentation and of their dependent libraries
	 * @throws ClassFormatException if some class file is not legal
	 * @throws IOException if there is an error accessing the disk
	 */
	public ClassInstrumentation(InputStream input, String className, JarOutputStream instrumentedJar, ClassLoader classLoader) throws ClassFormatException, IOException {
		// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
		ClassGen classGen = new ClassGen(new ClassParser(input, className).parse());

		// performs instrumentation on that image
		new Initializer(classGen, classLoader);

		// dump the image on disk
		classGen.getJavaClass().dump(instrumentedJar);
	}

	/**
	 * Local scope for the instrumentation of a single class.
	 */
	private class Initializer {

		/**
		 * The class that is being instrumented.
		 */
		private final ClassGen classGen;

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
		 * The non-transient instance fields of primitive type or of special reference types that are
		 * allowed in storage objects (such as {@link java.lang.String} and {@link java.math.BigInteger}).
		 * They are defined in the class being instrumented or in its superclasses
		 * up to {@link takamaka.lang.Storage} (excluded). This list is non-empty for storage classes only.
		 * The first set in the list are the fields of the topmost class; the last are the fields
		 * of the class being considered.
		 */
		private final LinkedList<SortedSet<Field>> eagerNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of type {@link takamaka.lang.Storage} or subclass, defined
		 * in the class being instrumented (superclasses are not considered). This set
		 * is non-empty for storage classes only.
		 */
		private final SortedSet<Field> lazyNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The class loader for resolving the classes under instrumentation and those of the supporting libraries.
		 */
		private final ClassLoader classLoader;

		/**
		 * The bootstrap methods of the class being processed.
		 */
		private final BootstrapMethod[] bootstrapMethods;

		/**
		 * The bootstrap methods that have been instrumented since they must receive
		 * an extra parameter, since they call an entry and need the calling contract for that.
		 */
		private final Set<BootstrapMethod> bootstrapMethodsThatWillRequireExtraThis = new HashSet<>();

		/**
		 * The class token of the contract class.
		 */
		private final Class<?> contractClass;

		/**
		 * The class token of the storage class.
		 */
		private final Class<?> storageClass;

		/**
		 * Performs the instrumentation of a single class.
		 * 
		 * @param classGen the class to instrument
		 * @param classLoader the class loader for resolving the classes under instrumentation and those of
		 *        the dependent libraries
		 */
		private Initializer(ClassGen classGen, ClassLoader classLoader) {
			try {
				this.contractClass = classLoader.loadClass(CONTRACT_CLASS_NAME);
				this.storageClass = classLoader.loadClass(STORAGE_CLASS_NAME);
			} catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
			this.classGen = classGen;
			this.className = classGen.getClassName();
			this.cpg = classGen.getConstantPool();
			this.factory = new InstructionFactory(cpg);
			this.classLoader = classLoader;
			this.isStorage = !className.equals(STORAGE_CLASS_NAME) && isStorage(className);
			this.isContract = isContract(className);

			Optional<BootstrapMethods> bootstraps = Stream.of(classGen.getAttributes())
				.filter(attribute -> attribute instanceof BootstrapMethods)
				.map(attribute -> (BootstrapMethods) attribute)
				.findAny();

			if (bootstraps.isPresent())
				this.bootstrapMethods = bootstraps.get().getBootstrapMethods();
			else
				this.bootstrapMethods = new BootstrapMethod[0];

			// the fields of the class are relevant only for storage classes
			if (isStorage) {
				try {
					collectNonTransientInstanceFieldsOf(classLoader.loadClass(className), true);
				}
				catch (ClassNotFoundException e) {
					throw new IncompleteClasspathError(e);
				}
			}

			instrumentClass();
		}

		/**
		 * performs the instrumentation.
		 */
		private void instrumentClass() {
			instrumentBootstrapsInvokingEntries();

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
		 * Instruments bootstrap methods that invoke an entry as their target code.
		 * They are the result of compiling method references to entries.
		 * Since entries receive extra parameters, we transform those bootstrap methods
		 * by calling brand new target code, that calls the entry with a normal invoke
		 * instruction. That instruction will be later instrumented during local instrumentation.
		 */
		private void instrumentBootstrapsInvokingEntries() {
			collectBootstrapsLeadingToEntries().stream()
				.forEach(this::instrumentBootstrapCallingEntry);
		}

		private Set<BootstrapMethod> collectBootstrapsLeadingToEntries() {
			Set<BootstrapMethod> result = new HashSet<>();

			int initialSize;
			do {
				initialSize = result.size();
				Stream.of(bootstrapMethods)
					.filter(bootstrap -> lambdaIsEntry(bootstrap) || lambdaCallsEntry(bootstrap, result))
					.forEach(result::add);
			}
			while (result.size() > initialSize);

			return result;
		}

		private boolean lambdaCallsEntry(BootstrapMethod bootstrap, Set<BootstrapMethod> bootstrapsCallingEntry) {
			if (bootstrap.getNumBootstrapArguments() == 3) {
				Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
				if (constant instanceof ConstantMethodHandle) {
					ConstantMethodHandle mh = (ConstantMethodHandle) constant;
					Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
					if (constant2 instanceof ConstantMethodref) {
						ConstantMethodref mr = (ConstantMethodref) constant2;
						int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
						String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
						ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
						String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
						String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

						// a lambda bridge can only be present in the same class that calls it
						if (className.equals(this.className)) {
							Optional<Method> lambda = Stream.of(classGen.getMethods())
								.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
								.findAny();

							return lambda.isPresent() && callsEntry(lambda.get(), bootstrapsCallingEntry);
						}
					}
				}
			};

			return false;
		}

		/**
		 * Determines if the given lambda method calls an entry, possibly through an
		 * {@code invokedynamic} with one of the given bootstraps.
		 * 
		 * @param lambda the lambda method
		 * @param bootstrapsCallingEntry the bootstraps known to call an entry
		 * @return true if that condition holds
		 */
		private boolean callsEntry(Method lambda, Set<BootstrapMethod> bootstrapsCallingEntry) {
			if (!lambda.isAbstract()) {
				MethodGen mg = new MethodGen(lambda, className, cpg);
				return StreamSupport.stream(mg.getInstructionList().spliterator(), false)
					.anyMatch(instruction -> callsEntry(instruction.getInstruction(), bootstrapsCallingEntry));
			}

			return false;
		}

		private boolean callsEntry(Instruction instruction, Set<BootstrapMethod> bootstrapsCallingEntry) {
			if (instruction instanceof INVOKEDYNAMIC) {
				INVOKEDYNAMIC invokedynamic = (INVOKEDYNAMIC) instruction;
				ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());
				return bootstrapsCallingEntry.contains(bootstrapMethods[cid.getBootstrapMethodAttrIndex()]);
			}
			else if (instruction instanceof INVOKESPECIAL || instruction instanceof INVOKEVIRTUAL || instruction instanceof INVOKEINTERFACE) {
				InvokeInstruction invoke = (InvokeInstruction) instruction;
				return isEntryPossiblyAlreadyInstrumented
					(invoke.getClassName(cpg), invoke.getMethodName(cpg), invoke.getSignature(cpg));
			}

			return false;
		}

		/**
		 * Determines if the given bootstrap method calls an entry as target code.
		 * 
		 * @param bootstrap the bootstrap method
		 * @return true if and only if that condition holds
		 */
		private boolean lambdaIsEntry(BootstrapMethod bootstrap) {
			if (bootstrap.getNumBootstrapArguments() == 3) {
				Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
				if (constant instanceof ConstantMethodHandle) {
					ConstantMethodHandle mh = (ConstantMethodHandle) constant;
					Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
					if (constant2 instanceof ConstantMethodref) {
						ConstantMethodref mr = (ConstantMethodref) constant2;
						int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
						String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
						ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
						String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
						String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

						return isEntryPossiblyAlreadyInstrumented(className, methodName, methodSignature);
					}
				}
			};

			return false;
		}

		private void instrumentBootstrapCallingEntry(BootstrapMethod bootstrap) {
			if (lambdaIsEntry(bootstrap))
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
				Optional<Method> old = Stream.of(classGen.getMethods())
						.filter(method -> method.getName().equals(methodName) &&
								method.getSignature().equals(methodSignature) &&
								method.isPrivate())
						.findAny();
				old.ifPresent(method -> {
					// we can modify the method handle since the lambda is becoming an instance method
					// and all calls must be made through invokespecial
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
				for (InstructionHandle ih: _new.getInstructionList()) {
					Instruction ins = ih.getInstruction();
					if (ins instanceof LocalVariableInstruction) {
						int index = ((LocalVariableInstruction) ins).getIndex();
						if (ins instanceof IINC)
							ih.setInstruction(new IINC(index + 1, ((IINC) ins).getIncrement()));
						else if (ins instanceof LoadInstruction)
							ih.setInstruction(InstructionFactory.createLoad(((LoadInstruction) ins).getType(cpg), index + 1));
						else if (ins instanceof StoreInstruction)
							ih.setInstruction(InstructionFactory.createStore(((LoadInstruction) ins).getType(cpg), index + 1));
					}
				}

			StackMapReplacer.replace(_new);
			classGen.replaceMethod(old, _new.getMethod());
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
			String implementedInterfaceMethosSignature = ((ConstantUtf8) cpg.getConstant(((ConstantMethodType) cpg.getConstant(args[2])).getDescriptorIndex())).getBytes();
			Type lambdaReturnType = Type.getReturnType(implementedInterfaceMethosSignature);

			// we replace the target code: it was an invokeX C.entry(pars):r and we transform it
			// into invokespecial className.lambda(C, pars):r where the name "lambda" is
			// not used in className. The extra parameter className is not added for
			// constructor references, since they create the new object themselves
			String lambdaName = getNewNameForPrivateMethod();

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
			args[1] = addMethodHandleToConstantPool(new ConstantMethodHandle(Const.REF_invokeSpecial, cpg.addMethodref(className, lambdaName, lambdaSignature)));

			// we create the target code: it is a new private synthetic instance method inside className,
			// called lambdaName and with signature lambdaSignature; its code loads all its explicit parameters
			// on the stack then calls the entry and returns its value (if any)
			InstructionList il = new InstructionList();
			if (invokeKind == Const.REF_newInvokeSpecial) {
				il.append(factory.createNew(entryClassName));
				if (lambdaReturnType != Type.VOID)
					il.append(InstructionConst.DUP);
			}

			int local = 1;
			for (Type arg: lambdaArgs) {
				il.append(InstructionFactory.createLoad(arg, local));
				local += arg.getSize();
			}

			short invoke;
			if (invokeKind == Const.REF_invokeVirtual)
				invoke = Const.INVOKEVIRTUAL;
			else if (invokeKind == Const.REF_invokeSpecial)
				invoke = Const.INVOKESPECIAL;
			else if (invokeKind == Const.REF_invokeInterface)
				invoke = Const.INVOKEINTERFACE;
			else if (invokeKind == Const.REF_newInvokeSpecial)
				invoke = Const.INVOKESPECIAL;
			else
				throw new IllegalStateException("Unexpected lambda invocation kind " + invokeKind);

			il.append(factory.createInvoke(entryClassName, entryName, entryReturnType, entryArgs, invoke));
			il.append(InstructionFactory.createReturn(lambdaReturnType));

			MethodGen addedLambda = new MethodGen(PRIVATE_SYNTHETIC, lambdaReturnType, lambdaArgs, null, lambdaName, className, il, cpg);
			il.setPositions();
			addedLambda.setMaxLocals();
			addedLambda.setMaxStack();
			classGen.addMethod(addedLambda.getMethod());
			bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
		}

		/**
		 * BCEL does not (yet?) provide a method to add a method handle constant into
		 * a constant pool. Hence we have to rely to a trick: first we add a new
		 * integer constant to the constant pool; then we replace it with
		 * the method handle constant. Ugly, but it currently seem to be the only way.
		 * 
		 * @param mh the constant to add
		 * @return the index at which the constant has been added
		 */
		private int addMethodHandleToConstantPool(ConstantMethodHandle mh) {
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
		 * BCEL does not (yet?) provide a method to add an invokedynamic constant into
		 * a constant pool. Hence we have to rely to a trick: first we add a new
		 * integer constant to the constant pool; then we replace it with
		 * the invokedynamic constant. Ugly, but it currently seem to be the only way.
		 * 
		 * @param cid the constant to add
		 * @return the index at which the constant has been added
		 */
		private int addInvokeDynamicToConstantPool(ConstantInvokeDynamic cid) {
			// first we check if an equal constant method handle was already in the constant pool
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
			}
			while (cpg.getSize() == size);

			// and then replace the integer constant with the method handle constant
			cpg.setConstant(index, cid);

			return index;
		}

		private String getNewNameForPrivateMethod() {
			int counter = 0;
			String newName;
			Method[] methods = classGen.getMethods();

			do {
				newName = EXTRA_LAMBDA_PREFIX + counter++;
			}
			while (Stream.of(methods).map(Method::getName).anyMatch(newName::equals));

			return newName;
		}

		/**
		 * Performs method-level instrumentations.
		 */
		private void localInstrumentation() {
			Method[] methods = classGen.getMethods();

			// the replacement of each method
			List<Method> instrumentedMethods =
				Stream.of(methods)
					.map(this::instrument)
					.collect(Collectors.toList());

			// replacing old with new methods
			int pos = 0;
			for (Method instrumented: instrumentedMethods)
				classGen.replaceMethod(methods[pos++], instrumented);
		}

		/**
		 * Instruments a single method of the class.
		 * 
		 * @param method the method to instrument
		 * @return the result of the instrumentation
		 */
		private Method instrument(Method method) {
			MethodGen methodGen = new MethodGen(method, className, cpg);
			replaceFieldAccessesWithAccessors(methodGen);
			addContractToCallsToEntries(methodGen);

			Class<?> callerContract;
			if (isContract && (callerContract = isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType())) != null)
				instrumentEntry(methodGen, callerContract, getAnnotation(className, method.getName(), method.getArgumentTypes(), method.getReturnType(), Payable.class) != null);

			addGasUpdates(methodGen);

			methodGen.setMaxLocals();
			methodGen.setMaxStack();
			if (!methodGen.isAbstract()) {
				methodGen.getInstructionList().setPositions();
				StackMapReplacer.replace(methodGen);
			}

			return methodGen.getMethod();
		}

		/**
		 * Adds a gas decrease at the beginning of each basic block of code.
		 * 
		 * @param method the method that gets instrumented
		 */
		private void addGasUpdates(MethodGen method) {
			SortedSet<InstructionHandle> dominators = computeDominators(method);
			dominators.stream().forEachOrdered(dominator -> addGasUpdate(dominator, method.getInstructionList(), dominators));
		}

		private void addGasUpdate(InstructionHandle dominator, InstructionList il, SortedSet<InstructionHandle> dominators) {
			long cost = gasCostOf(dominator, dominators);
			InstructionHandle newTarget;

			// up to this value, there is a special compact method for charging gas
			if (cost <= Takamaka.MAX_COMPACT)
				newTarget = il.insert(dominator, factory.createInvoke(TAKAMAKA_CLASS_NAME, "charge" + cost, Type.VOID, Type.NO_ARGS, Const.INVOKESTATIC));
			else {
				InstructionHandle pushCost;
				// up to 5, there is special, compact methods
				if (cost < Integer.MAX_VALUE)
					pushCost = il.insert(dominator, factory.createConstant((int) cost));
				else
					pushCost = il.insert(dominator, factory.createConstant(cost));

				newTarget = pushCost;

				il.insert(dominator, factory.createInvoke(TAKAMAKA_CLASS_NAME, "charge", Type.VOID,
					cost < Integer.MAX_VALUE ? ONE_INT_ARGS :ONE_LONG_ARGS, Const.INVOKESTATIC));
			}

			il.redirectBranches(dominator, newTarget);
		}

		private long gasCostOf(InstructionHandle dominator, SortedSet<InstructionHandle> dominators) {
			long cost = 0L;

			InstructionHandle cursor = dominator;
			do {
				cost += GasCosts.costOf(cursor.getInstruction());
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
			if (!method.isAbstract())
				return StreamSupport.stream(method.getInstructionList().spliterator(), false)
					.filter(this::isDominator)
					.collect(Collectors.toCollection(() -> new TreeSet<InstructionHandle>(Comparator.comparing(InstructionHandle::getPosition))));
			else
				return Collections.emptySortedSet();
		}

		private boolean isDominator(InstructionHandle ih) {
			InstructionHandle prev = ih.getPrev();
			// the first instruction is a dominator
			return prev == null || prev.getInstruction() instanceof BranchInstruction || prev.getInstruction() instanceof ExceptionThrower
				|| Stream.of(ih.getTargeters()).anyMatch(targeter -> targeter instanceof BranchInstruction || targeter instanceof CodeExceptionGen);
		}

		/**
		 * Passes the trailing implicit parameters to calls to entries. They are
		 * the contract where the entry is called and {@code null} (for the dummy argument).
		 * 
		 * @param method the method
		 */
		private void addContractToCallsToEntries(MethodGen method) {
			if (!method.isAbstract()) {
				InstructionList il = method.getInstructionList();
				List<InstructionHandle> callsToEntries =
					StreamSupport.stream(il.spliterator(), false)
						.filter(ih -> isCallToEntry(ih.getInstruction()))
						.collect(Collectors.toList());

				for (InstructionHandle ih: callsToEntries)
					passContractToCallToEntry(il, ih);
			}
		}

		/**
		 * Passes the trailing implicit parameters to the given call to an entry. They are
		 * the contract where the entry is called and {@code null} (for the dummy argument).
		 * 
		 * @param il the instructions of the method being instrumented
		 * @param ih the call to the entry
		 */
		private void passContractToCallToEntry(InstructionList il, InstructionHandle ih) {
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
				ConstantInvokeDynamic expandedCid = new ConstantInvokeDynamic(cid.getBootstrapMethodAttrIndex(), cpg.addNameAndType(methodName, Type.getMethodSignature(invoke.getReturnType(cpg), expandedArgs)));
				int index = addInvokeDynamicToConstantPool(expandedCid);
				INVOKEDYNAMIC copied = (INVOKEDYNAMIC) invokedynamic.copy();
				copied.setIndex(index);
				ih.setInstruction(copied);

				int slots = Stream.of(args).mapToInt(Type::getSize).sum();
				forEachPusher(ih, slots, where -> {
					il.append(where, where.getInstruction());
					where.setInstruction(InstructionConst.ALOAD_0);
				});
			}
			else {
				Type[] args = invoke.getArgumentTypes(cpg);
				Type[] expandedArgs = new Type[args.length + 2];
				System.arraycopy(args, 0, expandedArgs, 0, args.length);
				expandedArgs[args.length] = CONTRACT_OT;
				expandedArgs[args.length + 1] = DUMMY_OT;

				ih.setInstruction(InstructionConst.ALOAD_0); // the call must be inside a contract "this"
				il.append(ih, factory.createInvoke
						(invoke.getClassName(cpg), invoke.getMethodName(cpg),
								invoke.getReturnType(cpg), expandedArgs, invoke.getOpcode()));
				il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
			}
		}

		/**
		 * Finds the closest instructions whose stack height, at their beginning,
		 * is equal to the height of the stack at {@code ih} minus {@code slots}.
		 * 
		 * @param ih the start instruction of the look up
		 * @param slots the difference in stack height
		 */
		private void forEachPusher(InstructionHandle ih, int slots, Consumer<InstructionHandle> what) {
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
						if (!(previousIns instanceof ReturnInstruction) && !(previousIns instanceof ATHROW) && !(previousIns instanceof GotoInstruction)) {
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
						throw new IllegalStateException("Cannot find stack pushers for " + start);

					Stream.of(targeters)
						.filter(targeter -> targeter instanceof BranchInstruction)
						.map(targeter -> (BranchInstruction) targeter)
						.forEach(branch -> {
							int stackHeightBefore = current.stackHeightBeforeBytecode;
							stackHeightBefore -= branch.produceStack(cpg);
							stackHeightBefore += branch.consumeStack(cpg);

							HeightAtBytecode added = new HeightAtBytecode(previous, stackHeightBefore);
							if (seen.add(added))
								workingSet.add(added);
						});
				}
			}
			while (!workingSet.isEmpty());
		}

		/**
		 * Determines if the given instruction calls an entry.
		 * 
		 * @param instruction the instruction
		 * @return true if and only if that condition holds
		 */
		private boolean isCallToEntry(Instruction instruction) {
			if (instruction instanceof INVOKEDYNAMIC) {
				INVOKEDYNAMIC invokedynamic = (INVOKEDYNAMIC) instruction;
				ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());
				BootstrapMethod bootstrap = bootstrapMethods[cid.getBootstrapMethodAttrIndex()];
				if (bootstrapMethodsThatWillRequireExtraThis.contains(bootstrap))
					return true;
			}
			else if (instruction instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) instruction;
				ReferenceType receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType)
					return isEntryPossiblyAlreadyInstrumented(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg));
			}

			return false;
		}

		/**
		 * Determines if a method is an entry, possibly already instrumented.
		 * 
		 * @param className the name of the class defining the method
		 * @param methodName the name of the method
		 * @param signature the signature of the method
		 * @return true if and only if that condition holds
		 */
		private boolean isEntryPossiblyAlreadyInstrumented(String className, String methodName, String signature) {
			Type[] formals = Type.getArgumentTypes(signature);
			Type returnType = Type.getReturnType(signature);
			if (isEntry(className, methodName, formals, returnType) != null)
				return true;

			// the method might have been already instrumented, since it comes from
			// a jar already installed in blockchain; hence we try with the extra parameters added by instrumentation
			Type[] formalsExpanded = new Type[formals.length + 2];
			System.arraycopy(formals, 0, formalsExpanded, 0, formals.length);
			formalsExpanded[formals.length] = CONTRACT_OT;
			formalsExpanded[formals.length + 1] = DUMMY_OT;
			return isEntry(className, methodName, formalsExpanded, returnType) != null;
		}

		/**
		 * Determines if the given constructor method is annotated as entry.
		 * Yields the argument of the annotation.
		 * 
		 * @param className the class of the constructor or method
		 * @param methodName the name of the constructor or method
		 * @param formals the types of the formal arguments of the method
		 * @param returnType the return type of the method
		 * @return the value of the annotation, if it is a contract. For instance, for {@code @@Entry(PayableContract.class)}
		 *         this return value will be {@code takamaka.lang.PayableContract.class}
		 */
		private Class<?> isEntry(String className, String methodName, Type[] formals, Type returnType) {
			Annotation annotation = getAnnotation(className, methodName, formals, returnType, Entry.class);
			if (annotation != null) {
				Class<?> contractClass = ((Entry) annotation).value();
				return contractClass != Object.class ? contractClass : this.contractClass;
			}

			return null;
		}

		/**
		 * Gets the given annotation from the given constructor or method.
		 * 
		 * @param className the class of the constructor or method
		 * @param methodName the name of the constructor or method
		 * @param formals the types of the formal arguments of the method or constructor
		 * @param returnType the return type of the method or constructor
		 * @param annotation the class token of the annotation
		 * @return the annotation, if any. Yields {@code null} if the method or constructor has no such annotation
		 */
		private Annotation getAnnotation(String className, String methodName, Type[] formals, Type returnType, Class<? extends Annotation> annotation) {
			if (methodName.equals(Const.CONSTRUCTOR_NAME))
				return getAnnotationOfConstructor(className, formals, annotation);
			else
				return getAnnotationOfMethod(className, methodName, formals, returnType, annotation);
		}

		private Annotation getAnnotationOfConstructor(String className, Type[] formals, Class<? extends Annotation> annotation) {
			Class<?>[] formalsClass = Stream.of(formals).map(this::bcelToClass).toArray(Class[]::new);

			try {
				Class<?> clazz = classLoader.loadClass(className);
				Optional<java.lang.reflect.Constructor<?>> definition = Stream.of(clazz.getDeclaredConstructors())
					.filter(c -> Arrays.equals(c.getParameterTypes(), formalsClass))
					.findFirst();

				if (definition.isPresent())
					return definition.get().getAnnotation(annotation);
				else
					return null;
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		}

		private Annotation getAnnotationOfMethod(String className, String methodName, Type[] formals, Type returnType, Class<? extends Annotation> annotation) {
			Class<?> returnTypeClass = bcelToClass(returnType);
			Class<?>[] formalsClass = Stream.of(formals).map(this::bcelToClass).toArray(Class[]::new);

			try {
				Class<?> clazz = classLoader.loadClass(className);
				Optional<java.lang.reflect.Method> definition = Stream.of(clazz.getDeclaredMethods())
					.filter(m -> m.getName().equals(methodName) && m.getReturnType() == returnTypeClass && Arrays.equals(m.getParameterTypes(), formalsClass))
					.findFirst();

				if (definition.isPresent()) {
					Annotation result = definition.get().getAnnotation(annotation);
					if (result != null)
						return result;

					if (Modifier.isPrivate(definition.get().getModifiers()))
						return null;
				}

				Class<?> superclass = clazz.getSuperclass();
				if (superclass == null)
					return null;
				else
					return getAnnotationOfMethod(superclass.getName(), methodName, formals, returnType, annotation);
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		}

		/**
		 * Instruments an entry, by setting the caller and transferring funds
		 * for payable entries.
		 * 
		 * @param method the entry
		 * @param callerContract the class of the caller contract
		 * @param isPayable true if and only if the entry is payable
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
		 * Instruments an entry by calling the contract method that sets caller and balance.
		 * 
		 * @param method the entry
		 * @param callerContract the class of the caller contract
		 * @param slotForCaller the local variable for the caller implicit argument
		 * @param isPayable true if and only if the entry is payable
		 */
		private void setCallerAndBalance(MethodGen method, Class<?> callerContract, int slotForCaller, boolean isPayable) {
			InstructionList il = method.getInstructionList();

			// the call to the method that sets caller and balance cannot be put at the
			// beginning of the method, always: for constructors, Java bytecode requires
			// that their code starts with a call to a constructor of the superclass
			InstructionHandle where = determineWhereToSetCallerAndBalance(il, method, slotForCaller);
			InstructionHandle start = il.getStart();

			il.insert(start, InstructionFactory.createThis());
			il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
			if (callerContract != contractClass)
				il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
			if (isPayable) {
				// a payable entry method can have a first argument of type int/long/BigInteger
				Type amountType = method.getArgumentType(0);
				il.insert(start, InstructionFactory.createLoad(amountType, 1));
				Type[] paybleEntryArgs = new Type[] { CONTRACT_OT, amountType };
				il.insert(where, factory.createInvoke(className, PAYABLE_ENTRY, Type.VOID, paybleEntryArgs, Const.INVOKESPECIAL));
			}
			else
				il.insert(where, factory.createInvoke(className, ENTRY, Type.VOID, ENTRY_ARGS, Const.INVOKESPECIAL));
		}

		/**
		 * Entries call {@link takamaka.lang.Contract#entry(Contract)} or {@link takamaka.lang.Contract#payableEntry(Contract,BigInteger)}
		 * at their beginning, to set the caller and
		 * the balance of the called entry. In general, such call can be placed at the very beginning of the
		 * code. The only problem is related to constructors, that require their code to start with a call
		 * to a constructor of their superclass. In that case, this method finds the place where that
		 * contractor of the superclass is called: after which, we can add the call that sets caller and balance.
		 * 
		 * @param il the list of instructions of the entry
		 * @param method the entry
		 * @param slotForCaller the local where the caller contract is passed to the entry
		 * @return the instruction before which the code that sets caller and balance can be placed
		 */
		private InstructionHandle determineWhereToSetCallerAndBalance(InstructionList il, MethodGen method, int slotForCaller) {
			InstructionHandle start = il.getStart();

			if (method.getName().equals(Const.CONSTRUCTOR_NAME)) {
				// we have to identify the call to the constructor of the superclass:
				// the code of a constructor normally starts with an aload_0 whose value is consumed
				// by a call to a constructor of the superclass. In the middle, slotForCaller is not expected
				// to be modified. Note that this is the normal situation, as results from a normal
				// Java compiler. In principle, the Java bytecode might instead do very weird things,
				// including calling two constructors of the superclass at different places. In all such cases
				// this method fails and rejects the code: such non-standard code is not supported by Takamaka
				Instruction startInstruction = start.getInstruction();
				if (startInstruction.getOpcode() == Const.ALOAD_0 ||
						(startInstruction.getOpcode() == Const.ALOAD && ((LoadInstruction) startInstruction).getIndex() == 0)) {
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
								throw new IllegalStateException("Unexpected modification of local " + slotForCaller + " before initialization of " + className);
						}

						if (bytecode instanceof StackProducer)
							stackHeightAfterBytecode += ((StackProducer) bytecode).produceStack(cpg);
						if (bytecode instanceof StackConsumer)
							stackHeightAfterBytecode -= ((StackConsumer) bytecode).consumeStack(cpg);

						if (stackHeightAfterBytecode == 0) {
							// found a consumer of the aload_0: is it really a call to a constructor of the superclass?
							if (bytecode instanceof INVOKESPECIAL && ((INVOKESPECIAL) bytecode).getClassName(cpg).equals(classGen.getSuperclassName())
									&& ((INVOKESPECIAL) bytecode).getMethodName(cpg).equals(Const.CONSTRUCTOR_NAME))
								callsToConstructorsOfSuperclass.add(current.ih);
							else
								throw new IllegalStateException("Unexpected consumer of local 0 " + bytecode + " before initialization of " + className);
						}
						else if (bytecode instanceof GotoInstruction) {
							HeightAtBytecode added = new HeightAtBytecode(((GotoInstruction) bytecode).getTarget(), stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
						}
						else if (bytecode instanceof IfInstruction) {
							HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(), stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
							added = new HeightAtBytecode(((IfInstruction) bytecode).getTarget(), stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
						}
						else if (bytecode instanceof BranchInstruction || bytecode instanceof ATHROW || bytecode instanceof RETURN || bytecode instanceof RET)
							throw new IllegalStateException("Unexpected instruction " + bytecode + " before initialization of " + className);
						else {
							HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(), stackHeightAfterBytecode);
							if (seen.add(added))
								workingSet.add(added);
						}
					}
					while (!workingSet.isEmpty());

					if (callsToConstructorsOfSuperclass.size() == 1)
						return callsToConstructorsOfSuperclass.iterator().next().getNext();
					else
						throw new IllegalStateException("Cannot identify single call to constructor of superclass inside a constructor ot " + className);
				}
				else
					throw new IllegalStateException("Constructor of " + className + " does not start with aload 0");
			}
			else
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
			for (Type arg: method.getArgumentTypes()) {
				args.add(arg);
				slotsForParameters += arg.getSize();
			}
			args.add(CONTRACT_OT);
			args.add(DUMMY_OT); // to avoid name clashes after the addition
			method.setArgumentTypes(args.toArray(Type.NO_ARGS));

			String[] names = method.getArgumentNames();
			if (names != null) {
				List<String> namesAsList = new ArrayList<>();
				for (String name: names)
					namesAsList.add(name);
				namesAsList.add("caller");
				namesAsList.add("unused");
				method.setArgumentNames(namesAsList.toArray(new String[namesAsList.size()]));
			}

			return slotsForParameters + 1;
		}

		/**
		 * Replaces accesses to fields of storage classes with calls to accessor methods.
		 * 
		 * @param method the method where the replacement occurs
		 */
		private void replaceFieldAccessesWithAccessors(MethodGen method) {
			if (!method.isAbstract()) {
				InstructionList il = method.getInstructionList();
				StreamSupport.stream(il.spliterator(), false)
					.filter(this::isAccessToLazilyLoadedFieldInStorageClass)
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
				return factory.createInvoke(referencedClass.getClassName(), getterNameFor(referencedClass.getClassName(), fieldName), fieldType, Type.NO_ARGS, Const.INVOKEVIRTUAL);
			else // PUTFIELD
				return factory.createInvoke(referencedClass.getClassName(), setterNameFor(referencedClass.getClassName(), fieldName), Type.VOID, new Type[] { fieldType }, Const.INVOKEVIRTUAL);
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
				return isStorage(receiverClassName) && isLazilyLoaded(fieldType = bcelToClass(fi.getFieldType(cpg))) && !isTransient(receiverClassName, fi.getFieldName(cpg), fieldType);
			}
			else if (instruction instanceof PUTFIELD) {
				FieldInstruction fi = (FieldInstruction) instruction;
				ObjectType receiverType = (ObjectType) fi.getReferenceType(cpg);
				String receiverClassName = receiverType.getClassName();
				Class<?> fieldType;
				return isStorage(receiverClassName) && isLazilyLoaded(fieldType = bcelToClass(fi.getFieldType(cpg))) && !isTransientOrFinal(receiverClassName, fi.getFieldName(cpg), fieldType);
			}
			else
				return false;
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
			il.append(factory.createInvoke(classGen.getSuperclassName(), EXTRACT_UPDATES, Type.VOID, EXTRACT_UPDATES_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(STORAGE_CLASS_NAME, IN_STORAGE_NAME, Type.BOOLEAN));
			il.append(InstructionFactory.createStore(Type.BOOLEAN, 4));

			InstructionHandle end = il.append(InstructionConst.RETURN);

			for (Field field: eagerNonTransientInstanceFields.getLast())
				end = addUpdateExtractionForEagerField(field, il, end);

			for (Field field: lazyNonTransientInstanceFields)
				end = addUpdateExtractionForLazyField(field, il, end);

			MethodGen extractUpdates = new MethodGen(PROTECTED_SYNTHETIC, Type.VOID, EXTRACT_UPDATES_ARGS, null, EXTRACT_UPDATES, className, il, cpg);
			il.setPositions();
			extractUpdates.setMaxLocals();
			extractUpdates.setMaxStack();
			StackMapReplacer.replace(extractUpdates);
			classGen.addMethod(extractUpdates.getMethod());
		}

		/**
		 * Adds the code that check if a given lazy field has been updated since the beginning
		 * of a transaction and, in such a case, adds the corresponding update.
		 * 
		 * @param field the field
		 * @param il the instruction list where the code must be added
		 * @param end the instruction before which the extra code must be added
		 * @return the beginning of the added code
		 */
		private InstructionHandle addUpdateExtractionForLazyField(Field field, InstructionList il, InstructionHandle end) {
			Type type = Type.getType(field.getType());

			List<Type> args = new ArrayList<>();
			for (Type arg: ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			args.add(SET_OT);
			args.add(LIST_OT);
			args.add(ObjectType.STRING);
			args.add(ObjectType.OBJECT);

			InstructionHandle recursiveExtract;
			// we deal with special cases where the call to a recursive extract is useless: this is just an optimization
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
				il.insert(end, factory.createInvoke(STORAGE_CLASS_NAME, RECURSIVE_EXTRACT, Type.VOID, RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
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
			il.insert(recursiveExtract, factory.createInvoke(STORAGE_CLASS_NAME, ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

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
		 * Adds the code that check if a given eager field has been updated since the beginning
		 * of a transaction and, in such a case, adds the corresponding update.
		 * 
		 * @param field the field
		 * @param il the instruction list where the code must be added
		 * @param end the instruction before which the extra code must be added
		 * @return the beginning of the added code
		 */
		private InstructionHandle addUpdateExtractionForEagerField(Field field, InstructionList il, InstructionHandle end) {
			Class<?> fieldType = field.getType();
			Type type = Type.getType(fieldType);
			boolean isEnum = fieldType.isEnum();

			List<Type> args = new ArrayList<>();
			for (Type arg: ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			if (isEnum) {
				args.add(ObjectType.STRING);
				args.add(ENUM_OT);
			}
			else
				args.add(type);

			InstructionHandle addUpdatesFor = il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createConstant(className));
			il.insert(end, factory.createConstant(field.getName()));
			il.insert(end, InstructionConst.ALOAD_1);
			if (isEnum)
				il.insert(end, factory.createConstant(fieldType.getName()));
			il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createGetField(className, field.getName(), type));
			il.insert(end, factory.createInvoke(STORAGE_CLASS_NAME, ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			if (fieldType == double.class) {
				il.insert(addUpdatesFor, InstructionConst.DCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (fieldType == float.class) {
				il.insert(addUpdatesFor, InstructionConst.FCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (fieldType == long.class) {
				il.insert(addUpdatesFor, InstructionConst.LCMP);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (fieldType == String.class || fieldType == BigInteger.class) {
				// comparing strings or BigInteger with their previous value is done by checking if they
				// are equals rather than ==. This is just an optimization, to avoid storing an equivalent value
				// as an update. It is relevant for the balance fields of contracts, that might reach 0 at the
				// end of a transaction, as it was at the beginning, but has fluctuated during the
				// transaction: it is useless to add an update for it
				il.insert(addUpdatesFor, factory.createInvoke("java.util.Objects", "equals", Type.BOOLEAN, TWO_OBJECTS_ARGS, Const.INVOKESTATIC));
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFNE, end));
			}
			else if (!fieldType.isPrimitive())
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
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
			il.append(InstructionConst.ALOAD_1);
			il.append(factory.createPutField(className, field.getName(), type));
			il.append(InstructionConst.RETURN);

			MethodGen setter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, BasicType.VOID, new Type[] { type }, null, setterNameFor(className, field.getName()), className, il, cpg);
			setter.setMaxLocals();
			setter.setMaxStack();
			classGen.addMethod(setter.getMethod());
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
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(className, field.getName(), type));
			il.append(InstructionFactory.createReturn(type));

			MethodGen getter = new MethodGen(PUBLIC_SYNTHETIC_FINAL, type, Type.NO_ARGS, null, getterNameFor(className, field.getName()), className, il, cpg);
			getter.setMaxLocals();
			getter.setMaxStack();
			classGen.addMethod(getter.getMethod());
		}

		private String getterNameFor(String className, String fieldName) {
			// we use the class name as well, in order to disambiguate fields with the same name
			// in sub and superclass
			return GETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
		}

		private String setterNameFor(String className, String fieldName) {
			// we use the class name as well, in order to disambiguate fields with the same name
			// in sub and superclass
			return SETTER_PREFIX + className.replace('.', '_') + '_' + fieldName;
		}

		/**
		 * Adds the ensure loaded methods for the lazy fields of the class being instrumented.
		 */
		private void addEnsureLoadedMethods() {
			lazyNonTransientInstanceFields.forEach(this::addEnsureLoadedMethodFor);
		}

		/**
		 * Adds the ensure loaded method for the given lazy field.
		 */
		private void addEnsureLoadedMethodFor(Field field) {
			// final fields cannot remain as such, since the ensureMethod will update them
			// and it is not a constructor. Java < 9 will not check this constraint but
			// newer versions of Java would reject the code without this change
			if (Modifier.isFinal(field.getModifiers())) {
				org.apache.bcel.classfile.Field oldField = Stream.of(classGen.getFields())
					.filter(f -> f.getName().equals(field.getName()) && f.getType().equals(Type.getType(field.getType())))
					.findFirst()
					.get();
				FieldGen newField = new FieldGen(oldField, cpg);
				newField.setAccessFlags(oldField.getAccessFlags() ^ Const.ACC_FINAL);
				classGen.replaceField(oldField, newField.getField());
			}

			Type type = Type.getType(field.getType());
			InstructionList il = new InstructionList();
			InstructionHandle _return = il.append(InstructionConst.RETURN);
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(STORAGE_CLASS_NAME, IN_STORAGE_NAME, BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFEQ, _return));
			il.insert(_return, InstructionFactory.createThis());
			String fieldName = field.getName();
			il.insert(_return, factory.createGetField(className, IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFNE, _return));
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.ICONST_1);
			il.insert(_return, factory.createPutField(className, IF_ALREADY_LOADED_PREFIX + fieldName, BasicType.BOOLEAN));
			il.insert(_return, factory.createConstant(className));
			il.insert(_return, factory.createConstant(fieldName));
			il.insert(_return, factory.createConstant(field.getType().getName()));
			il.insert(_return, factory.createInvoke(className, DESERIALIZE_LAST_UPDATE_FOR, ObjectType.OBJECT, THREE_STRINGS_ARGS, Const.INVOKEVIRTUAL));
			il.insert(_return, factory.createCast(ObjectType.OBJECT, type));
			il.insert(_return, InstructionConst.DUP2);
			il.insert(_return, factory.createPutField(className, fieldName, type));
			il.insert(_return, factory.createPutField(className, OLD_PREFIX + fieldName, type));
			il.setPositions();

			MethodGen ensureLoaded = new MethodGen(PRIVATE_SYNTHETIC, BasicType.VOID, Type.NO_ARGS, null, ENSURE_LOADED_PREFIX + fieldName, className, il, cpg);
			ensureLoaded.setMaxLocals();
			ensureLoaded.setMaxStack();
			StackMapReplacer.replace(ensureLoaded);
			classGen.addMethod(ensureLoaded.getMethod());
		}

		/**
		 * Adds fields for the old value and the loading state of the fields of a storage class.
		 */
		private void addOldAndIfAlreadyLoadedFields() {
			eagerNonTransientInstanceFields.getLast().forEach(this::addOldFieldFor);

			for (Field field: lazyNonTransientInstanceFields) {
				addOldFieldFor(field);
				addIfAlreadyLoadedFieldFor(field);
			}
		}

		/**
		 * Adds the field for the loading state of the fields of a storage class.
		 */
		private void addIfAlreadyLoadedFieldFor(Field field) {
			classGen.addField(new FieldGen(PRIVATE_SYNTHETIC, BasicType.BOOLEAN, IF_ALREADY_LOADED_PREFIX + field.getName(), cpg).getField());
		}

		/**
		 * Adds the field for the old value of the fields of a storage class.
		 */
		private void addOldFieldFor(Field field) {
			classGen.addField(new FieldGen(PRIVATE_SYNTHETIC, Type.getType(field.getType()), OLD_PREFIX + field.getName(), cpg).getField());
		}

		/**
		 * Adds a constructor that deserializes an object of storage type.
		 * This constructor receives the values of the eager fields, ordered
		 * by putting first the fields of the superclasses, then those of the
		 * same class being constructed, ordered by name and then by {@code toString()}
		 * of their type.
		 */
		private void addConstructorForDeserializationFromBlockchain() {
			List<Type> args = new ArrayList<>();

			// the parameters of the constructor start with a storage reference
			// to the object being deserialized
			args.add(new ObjectType(StorageReferenceAlreadyInBlockchain.class.getName()));

			// then there are the fields of the class and superclasses, with superclasses first
			eagerNonTransientInstanceFields.stream()
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.map(Type::getType)
				.forEachOrdered(args::add);

			InstructionList il = new InstructionList();
			int nextLocal = addCallToSuper(il);
			addInitializationOfEagerFields(il, nextLocal);
			il.append(InstructionConst.RETURN);

			MethodGen constructor = new MethodGen(PUBLIC_SYNTHETIC, BasicType.VOID, args.toArray(Type.NO_ARGS), null, Const.CONSTRUCTOR_NAME, className, il, cpg);
			constructor.setMaxLocals();
			constructor.setMaxStack();
			classGen.addMethod(constructor.getMethod());
		}

		/**
		 * Adds a call from the deserialization constructor of a storage class
		 * to the deserialization constructor of the superclass.
		 * 
		 * @param il the instructions where the call must be added
		 * @return the number of local variables used to accomodate the
		 *         arguments passed to the constructor of the superclass
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
			};
		
			PushLoad pushLoad = new PushLoad();
			eagerNonTransientInstanceFields.stream()
				.limit(eagerNonTransientInstanceFields.size() - 1)
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.map(Type::getType)
				.forEachOrdered(pushLoad);
		
			il.append(factory.createInvoke(classGen.getSuperclassName(), Const.CONSTRUCTOR_NAME, BasicType.VOID, argsForSuperclasses.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));
		
			return pushLoad.local;
		}

		/**
		 * Adds code that initializes the eager fields of the storage class
		 * being instrumented.
		 * 
		 * @param il the instructions where the code must be added
		 * @param nextLocal the local variables where the parameters start, that must be stored in the fields
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
			if (clazz != storageClass) {
				// we put at the beginning the fields of the superclasses
				collectNonTransientInstanceFieldsOf(clazz.getSuperclass(), false);

				// then the eager fields of className, in order
				eagerNonTransientInstanceFields.add(Stream.of(clazz.getDeclaredFields())
					.filter(field -> !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()) && !isAddedByTakamaka(field) && !isLazilyLoaded(field.getType()))
					.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

				// we collect lazy fields as well, but only for the class being instrumented
				if (firstCall)
					Stream.of(clazz.getDeclaredFields())
						.filter(field -> !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()) && !isAddedByTakamaka(field) && isLazilyLoaded(field.getType()))
						.forEach(lazyNonTransientInstanceFields::add);
			}
		}

		/**
		 * Determines if the given field has been added by this instrumenter.
		 * 
		 * @param field the field
		 * @return true if and only if that condition holds
		 */
		private boolean isAddedByTakamaka(Field field) {
			return field.getName().startsWith("§");
		}

		/**
		 * Determines if a field of a storage class, having the given field, is lazily loaded.
		 * 
		 * @param type the type
		 * @return true if and only if that condition holds
		 */
		private boolean isLazilyLoaded(Class<?> type) {
			return !type.isPrimitive() && type != String.class && type != BigInteger.class && !type.isEnum();
		}

		/**
		 * Determines if a class is a storage class.
		 * 
		 * @param className the name of the class
		 * @return true if and only if that class extends {@link takamaka.lang.Storage}
		 */
		private boolean isStorage(String className) {
			try {
				return storageClass.isAssignableFrom(classLoader.loadClass(className));
			} catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
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
			try {
				Class<?> clazz = classLoader.loadClass(className);
				
				do {
					Optional<Field> match = Stream.of(clazz.getDeclaredFields())
						.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
						.findFirst();

					if (match.isPresent())
						return Modifier.isTransient(match.get().getModifiers());
				}
				while (clazz != storageClass && clazz != contractClass);
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}

			return false;
		}

		/**
		 * Computes the Java token class for the given BCEL type.
		 * 
		 * @param type the BCEL type
		 * @return type the class token corresponding to {@code type}
		 */
		private Class<?> bcelToClass(Type type) {
			if (type == BasicType.BOOLEAN)
				return boolean.class;
			else if (type == BasicType.BYTE)
				return byte.class;
			else if (type == BasicType.CHAR)
				return char.class;
			else if (type == BasicType.DOUBLE)
				return double.class;
			else if (type == BasicType.FLOAT)
				return float.class;
			else if (type == BasicType.INT)
				return int.class;
			else if (type == BasicType.LONG)
				return long.class;
			else if (type == BasicType.SHORT)
				return short.class;
			else if (type == BasicType.VOID)
				return void.class;
			else if (type instanceof ObjectType)
				try {
					return classLoader.loadClass(type.toString()); //getSignature().replace('/', '.'));
				}
				catch (ClassNotFoundException e) {
					throw new IncompleteClasspathError(e);
				}
			else { // array
				Class<?> elementsClass = bcelToClass(((ArrayType) type).getElementType());
				// trick: we build an array of 0 elements just to access its class token
				return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
			}
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
			try {
				Class<?> clazz = classLoader.loadClass(className);
				
				do {
					Optional<Field> match = Stream.of(clazz.getDeclaredFields())
						.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
						.findFirst();

					if (match.isPresent()) {
						int modifiers = match.get().getModifiers();
						return Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers);
					}
				}
				while (clazz != storageClass && clazz != contractClass);
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}

			return false;
		}

		/**
		 * Checks if a class is a contract or subclass of contract.
		 * 
		 * @param className the name of the class
		 * @return true if and only if that condition holds
		 */
		private boolean isContract(String className) {
			try {
				return contractClass.isAssignableFrom(classLoader.loadClass(className));
			} catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
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