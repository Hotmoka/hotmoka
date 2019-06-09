package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ATHROW;
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
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.ReferenceType;
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
	private final static Logger LOGGER = Logger.getLogger(ClassInstrumentation.class.getName());
	private final static String OLD_PREFIX = "§old_";
	private final static String IF_ALREADY_LOADED_PREFIX = "§ifAlreadyLoaded_";
	private final static String ENSURE_LOADED_PREFIX = "§ensureLoaded_";
	private final static String GETTER_PREFIX = "§get_";
	private final static String SETTER_PREFIX = "§set_";
	private final static String EXTRACT_UPDATES = "extractUpdates";
	private final static String RECURSIVE_EXTRACT = "recursiveExtract";
	private final static String ADD_UPDATE_FOR = "addUpdateFor";
	private final static String PAYABLE_ENTRY = "payableEntry";
	private final static String ENTRY = "entry";
	private final static String IN_STORAGE_NAME = "inStorage";
	private final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastLazyUpdateFor";
	private final static String ENTRY_CLASS_NAME_JB = 'L' + Entry.class.getName().replace('.', '/') + ';';
	private final static String PAYABLE_CLASS_NAME_JB = 'L' + Payable.class.getName().replace('.', '/') + ';';
	private final static String CONTRACT_CLASS_NAME = "takamaka.lang.Contract";
	private final static String CONTRACT_CLASS_NAME_JB = 'L' + CONTRACT_CLASS_NAME.replace('.', '/') + ';';
	private final static String DUMMY_CLASS_NAME = Dummy.class.getName();
	private final static String DUMMY_CLASS_NAME_JB = 'L' + DUMMY_CLASS_NAME.replace('.', '/') + ';';
	private final static String TAKAMAKA_CLASS_NAME = Takamaka.class.getName();
	private final static String STORAGE_CLASS_NAME = Storage.class.getName();
	private final static String ENUM_CLASS_NAME = Enum.class.getName();
	private final static short PUBLIC_SYNTHETIC = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC;
	private final static short PUBLIC_SYNTHETIC_FINAL = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC | Const.ACC_FINAL;
	private final static short PROTECTED_SYNTHETIC = Const.ACC_PROTECTED | Const.ACC_SYNTHETIC;
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName).thenComparing(field -> field.getType().toString());

	private final static ObjectType CONTRACT_OT = new ObjectType(CONTRACT_CLASS_NAME);
	private final static ObjectType BIGINTEGER_OT = new ObjectType(BigInteger.class.getName());
	private final static ObjectType ENUM_OT = new ObjectType(ENUM_CLASS_NAME);
	private final static ObjectType SET_OT = new ObjectType(Set.class.getName());
	private final static ObjectType LIST_OT = new ObjectType(List.class.getName());
	private final static ObjectType DUMMY_OT = new ObjectType(DUMMY_CLASS_NAME);
	private final static Type[] THREE_STRINGS_ARGS = new Type[] { Type.STRING, Type.STRING, Type.STRING };
	private final static Type[] EXTRACT_UPDATES_ARGS = new Type[] { SET_OT, SET_OT, LIST_OT };
	private final static Type[] ADD_UPDATES_FOR_ARGS = new Type[] { Type.STRING, Type.STRING, SET_OT };
	private final static Type[] RECURSIVE_EXTRACT_ARGS = new Type[] { Type.OBJECT, SET_OT, SET_OT, LIST_OT };
	private final static Type[] ENTRY_ARGS = new Type[] { CONTRACT_OT };
	private final static Type[] ONE_INT_ARGS = new Type[] { Type.INT };
	private final static Type[] ONE_LONG_ARGS = new Type[] { Type.LONG };
	private final static Type[] TWO_OBJECTS_ARGS = new Type[] { Type.OBJECT, Type.OBJECT };

	/**
	 * Performs the instrumentation of a single class file.
	 * 
	 * @param input the input stream containing the class to instrument
	 * @param className the name of the class
	 * @param instrumentedJar the jar where the instrumented class will be added
	 * @param program the collection of the classes under instrumentation of of their dependent libraries
	 * @throws ClassFormatException if some class file is not legal
	 * @throws IOException if there is an error accessing the disk
	 */
	public ClassInstrumentation(InputStream input, String className, JarOutputStream instrumentedJar, Program program) throws ClassFormatException, IOException {
		LOGGER.fine(() -> "Instrumenting " + className);

		// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
		ClassGen classGen = new ClassGen(new ClassParser(input, className).parse());

		// performs instrumentation on that image
		new Initializer(classGen, program);

		// dump the image on disk
		classGen.getJavaClass().dump(instrumentedJar);
	}

	/**
	 * Local scope for the instrumentation of a single class.
	 */
	private class Initializer {

		private static final String OBJECT_CLASS_NAME = "java.lang.Object";

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
		 * The program that collects the classes under instrumentation and those of the
		 * supporting libraries.
		 */
		private final Program program;

		/**
		 * Performs the instrumentation of a single class.
		 * 
		 * @param classGen the class to instrument
		 * @param program the collection of all classes under instrumentation and those of the
		 *                supporting libraries
		 */
		private Initializer(ClassGen classGen, Program program) {
			this.classGen = classGen;
			this.className = classGen.getClassName();
			this.cpg = classGen.getConstantPool();
			this.factory = new InstructionFactory(cpg);
			this.program = program;
			this.isStorage = !className.equals(STORAGE_CLASS_NAME) && isStorage(className);
			this.isContract = isContract(className);

			// the fields of the class are relevant only for storage classes
			if (isStorage)
				collectNonTransientInstanceFieldsOf(className);

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
				// storage classes need all the serialization machinery
				addOldAndIfAlreadyLoadedFields();
				addConstructorForDeserializationFromBlockchain();
				addAccessorMethods();
				addEnsureLoadedMethods();
				addExtractUpdates();
			}
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

			String callerContract;
			if (isContract && (callerContract = isEntry(className, method.getName(), method.getSignature())) != null)
				instrumentEntry(methodGen, callerContract, isPayable(method.getName(), method.getSignature()));

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
			Type[] args = invoke.getArgumentTypes(cpg);
			Type[] argsWithContract = new Type[args.length + 2];
			System.arraycopy(args, 0, argsWithContract, 0, args.length);
			argsWithContract[args.length] = CONTRACT_OT;
			argsWithContract[args.length + 1] = DUMMY_OT;

			ih.setInstruction(InstructionConst.ALOAD_0); // the call must be inside a contract "this"
			il.append(ih, factory.createInvoke
				(invoke.getClassName(cpg), invoke.getMethodName(cpg),
				invoke.getReturnType(cpg), argsWithContract, invoke.getOpcode()));
			il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
		}

		/**
		 * Determines if the given instruction calls an entry.
		 * 
		 * @param instruction the instruction
		 * @return true if and only if that condition holds
		 */
		private boolean isCallToEntry(Instruction instruction) {
			if (instruction instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) instruction;
				ReferenceType receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType) {
					String sig = invoke.getSignature(cpg);
					if (isEntry(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg)) != null)
						return true;

					// the callee might have been already instrumented, since it comes from
					// a jar already installed in blockchain; hence we try with the extra parameters added by instrumentation
					int whereToAddParameters = sig.lastIndexOf(')');
					if (whereToAddParameters > 0) {
						sig = sig.substring(0, whereToAddParameters) + CONTRACT_CLASS_NAME_JB + DUMMY_CLASS_NAME_JB + sig.substring(whereToAddParameters);
						if (isEntry(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), sig) != null)
							return true;
					}
				}
			}

			return false;
		}

		/**
		 * Determines if the given constructor method is annotated as entry.
		 * Yields the argument of the annotation.
		 * 
		 * @param className the class of the constructor or method
		 * @param methodName the name of the constructor or method
		 * @param methodSignature the signature of the constructor or method
		 * @return the value of the annotation, if it is a contract. For instance, for {@code @@Entry(PayableContract.class)}
		 *         this return value will be the string {@code takamaka.lang.PayableContract}
		 */
		private String isEntry(String className, String methodName, String methodSignature) {
			AnnotationEntry annotation = getAnnotation(className, methodName, methodSignature, ENTRY_CLASS_NAME_JB);
			if (annotation != null) {
				ElementValuePair[] pairs = annotation.getElementValuePairs();
				if (pairs.length == 1) {
					String callerContract = Utility.signatureToString(pairs[0].getValue().stringifyValue());
					if (isContract(callerContract))
						return callerContract;
				}

				// default
				return CONTRACT_CLASS_NAME;
			}

			return null;
		}

		/**
		 * Gets the given annotation from the given constructor or method.
		 * 
		 * @param className the class of the constructor or method
		 * @param methodName the name of the constructor or method
		 * @param methodSignature the signature of the constructor or method
		 * @param annotationNameJB the name of the annotation, in Java bytecode style (such as {@code Ljava/lang/Object;})
		 * @return the annotation, if any. Yields {@code null} if the method or constructor has no such annotation
		 */
		private AnnotationEntry getAnnotation(String className, String methodName, String methodSignature, String annotationNameJB) {
			if (className.equals(OBJECT_CLASS_NAME))
				return null;

			JavaClass clazz = program.get(className);
			if (clazz == null)
				return null;

			Optional<Method> definition = Stream.of(clazz.getMethods())
				.filter(m -> m.getName().equals(methodName) && m.getSignature().equals(methodSignature))
				.findAny();

			if (definition.isPresent()) {
				List<AnnotationEntry> annotations = Stream.of(definition.get().getAnnotationEntries())
					.filter(annotation -> annotation.getAnnotationType().equals(annotationNameJB))
					.collect(Collectors.toList());

				if (annotations.size() == 1)
					return annotations.get(0);

				if (definition.get().isPrivate())
					return null;
			}

			if (methodName.equals(Const.CONSTRUCTOR_NAME))
				return null;

			AnnotationEntry annotation = getAnnotation(clazz.getSuperclassName(), methodName, methodSignature, annotationNameJB);
			if (annotation != null)
				return annotation;

			for (String _interface: clazz.getInterfaceNames()) {
				annotation = getAnnotation(_interface, methodName, methodSignature, annotationNameJB);
				if (annotation != null)
					return annotation;
			}

			return null;
		}

		/**
		 * Determines if the given constructor method is annotated as payable.
		 * 
		 * @param methodName the name of the constructor or method
		 * @param methodSignature the signature of the constructor or method
		 * @return true if and only if that condition holds
		 */
		private boolean isPayable(String methodName, String methodSignature) {
			return getAnnotation(className, methodName, methodSignature, PAYABLE_CLASS_NAME_JB) != null;
		}

		/**
		 * Instruments an entry, by setting the caller and transferring funds
		 * for payable entries.
		 * 
		 * @param method the entry
		 * @param callerContract the name of the caller class. This is a contract
		 * @param isPayable true if and only if the entry is payable
		 */
		private void instrumentEntry(MethodGen method, String callerContract, boolean isPayable) {
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
		 * @param callerContract the name of the caller class. This is a contract
		 * @param slotForCaller the local variable for the caller implicit argument
		 * @param isPayable true if and only if the entry is payable
		 */
		private void setCallerAndBalance(MethodGen method, String callerContract, int slotForCaller, boolean isPayable) {
			InstructionList il = method.getInstructionList();

			// the call to the method that sets caller and balance cannot be put at the
			// beginning of the method, always: for constructors, Java bytecode requires
			// that their code starts with a call to a constructor of the superclass
			InstructionHandle where = determineWhereToSetCallerAndBalance(il, method, slotForCaller);
			InstructionHandle start = il.getStart();

			boolean needsTemp = where != start;
			if (needsTemp) {
				//TODO: avoid use of this static field temp. Stack maps make this a bit difficult
				il.insert(factory.createPutStatic(CONTRACT_CLASS_NAME, "temp", CONTRACT_OT));
				il.insert(InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
			}

			il.insert(where, InstructionFactory.createThis());
			if (needsTemp)
				il.insert(where, factory.createGetStatic(CONTRACT_CLASS_NAME, "temp", CONTRACT_OT));
			else
				il.insert(where, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));

			if (!callerContract.equals(CONTRACT_CLASS_NAME)) {
				il.insert(where, InstructionConst.DUP);
				il.insert(where, factory.createCast(CONTRACT_OT, new ObjectType(callerContract)));
				il.insert(where, InstructionConst.POP);
			}

			if (isPayable) {
				// a payable entry method can have a first argument of type int/long/BigInteger
				Type amountType = method.getArgumentType(0);
				il.insert(where, InstructionFactory.createLoad(amountType, 1));
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

					class HeightAtBytecode {
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
				return isStorage(receiverClassName) && isLazilyLoaded(fi.getFieldType(cpg)) && !isTransient(receiverClassName, fi.getFieldName(cpg), fi.getFieldType(cpg));
			}
			else if (instruction instanceof PUTFIELD) {
				FieldInstruction fi = (FieldInstruction) instruction;

				ObjectType receiverType = (ObjectType) fi.getReferenceType(cpg);
				String receiverClassName = receiverType.getClassName();
				return isStorage(receiverClassName) && isLazilyLoaded(fi.getFieldType(cpg)) && !isTransientOrFinal(receiverClassName, fi.getFieldName(cpg), fi.getFieldType(cpg));
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
			ObjectType type = (ObjectType) field.getType();

			List<Type> args = new ArrayList<>();
			for (Type arg: ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			args.add(SET_OT);
			args.add(LIST_OT);
			args.add(ObjectType.STRING);
			args.add(ObjectType.OBJECT);

			InstructionHandle recursiveExtract;
			// we deal with special cases where the call to a recursive extract is useless: this is just an optimization
			if (type.equals(ObjectType.STRING) || type.equals(BIGINTEGER_OT))
				recursiveExtract = end;
			else {
				recursiveExtract = il.insert(end, InstructionFactory.createThis());
				il.insert(end, InstructionConst.DUP);
				il.insert(end, factory.createGetField(className, OLD_PREFIX + field.getName(), type));
				il.insert(end, InstructionConst.ALOAD_1);
				il.insert(end, InstructionConst.ALOAD_2);
				il.insert(end, InstructionFactory.createLoad(LIST_OT, 3));
				il.insert(end, factory.createInvoke(STORAGE_CLASS_NAME, RECURSIVE_EXTRACT, Type.VOID, RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
			}

			InstructionHandle addUpdatesFor = il.insert(recursiveExtract, InstructionFactory.createThis());
			il.insert(recursiveExtract, factory.createConstant(className));
			il.insert(recursiveExtract, factory.createConstant(field.getName()));
			il.insert(recursiveExtract, InstructionConst.ALOAD_1);
			il.insert(recursiveExtract, InstructionConst.ALOAD_2);
			il.insert(recursiveExtract, InstructionFactory.createLoad(LIST_OT, 3));
			il.insert(recursiveExtract, factory.createConstant(type.getClassName()));
			il.insert(recursiveExtract, InstructionFactory.createThis());
			il.insert(recursiveExtract, factory.createGetField(className, field.getName(), type));
			il.insert(recursiveExtract, factory.createInvoke(STORAGE_CLASS_NAME, ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

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
			Type type = field.getType();
			boolean isEnum = type instanceof ObjectType && isEnum(((ObjectType) type).getClassName());

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
				il.insert(end, factory.createConstant(((ObjectType) type).getClassName()));
			il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createGetField(className, field.getName(), type));
			il.insert(end, factory.createInvoke(STORAGE_CLASS_NAME, ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			if (type.equals(Type.DOUBLE)) {
				il.insert(addUpdatesFor, InstructionConst.DCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (type.equals(Type.FLOAT)) {
				il.insert(addUpdatesFor, InstructionConst.FCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (type.equals(Type.LONG)) {
				il.insert(addUpdatesFor, InstructionConst.LCMP);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (type.equals(ObjectType.STRING) || type.equals(BIGINTEGER_OT)) {
				// comparing strings or BigInteger with their previous value is done by checking if they
				// are equals rather than ==. This is just an optimization, to avoid storing an equivalent value
				// as an update. It is relevant for the balance fields of contracts, that might reach 0 at the
				// end of a transaction, as it was at the beginning, but has fluctuated during the
				// transaction: it is useless to add an update for it
				il.insert(addUpdatesFor, factory.createInvoke("java.util.Objects", "equals", Type.BOOLEAN, TWO_OBJECTS_ARGS, Const.INVOKESTATIC));
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFNE, end));
			}
			else if (type instanceof ReferenceType)
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
			
			if (!field.isFinal())
				addSetterFor(field);
		}

		/**
		 * Adds a setter method for the given field.
		 * 
		 * @param field the field
		 */
		private void addSetterFor(Field field) {
			Type type = field.getType();
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
		 * Yields the modifiers for the accessors added for the given field.
		 * 
		 * @param field the field
		 * @return the visibility modifiers of the field, plus {@code synthetic}
		 */
		/*private short modifiersFrom(Field field) {
			short modifiers = Const.ACC_SYNTHETIC;
			if (field.isPrivate())
				modifiers |= Const.ACC_PRIVATE;
			else if (field.isProtected())
				modifiers |= Const.ACC_PROTECTED;
			else if (field.isPublic())
				modifiers |= Const.ACC_PUBLIC;
			return modifiers;
		}*/

		/**
		 * Adds a getter method for the given field.
		 * 
		 * @param field the field
		 */
		private void addGetterFor(Field field) {
			Type type = field.getType();
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
			if (field.isFinal()) {
				FieldGen newField = new FieldGen(field, cpg);
				newField.setAccessFlags(field.getAccessFlags() ^ Const.ACC_FINAL);
				classGen.replaceField(field, newField.getField());
			}

			InstructionList il = new InstructionList();
			InstructionHandle _return = il.append(InstructionConst.RETURN);
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(STORAGE_CLASS_NAME, IN_STORAGE_NAME, BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFEQ, _return));
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(className, IF_ALREADY_LOADED_PREFIX + field.getName(), BasicType.BOOLEAN));
			il.insert(_return, InstructionFactory.createBranchInstruction(Const.IFNE, _return));
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.DUP);
			il.insert(_return, InstructionConst.ICONST_1);
			il.insert(_return, factory.createPutField(className, IF_ALREADY_LOADED_PREFIX + field.getName(), BasicType.BOOLEAN));
			il.insert(_return, factory.createConstant(className));
			il.insert(_return, factory.createConstant(field.getName()));
			il.insert(_return, factory.createConstant(((ObjectType) field.getType()).getClassName()));
			il.insert(_return, factory.createInvoke(className, DESERIALIZE_LAST_UPDATE_FOR, ObjectType.OBJECT, THREE_STRINGS_ARGS, Const.INVOKEVIRTUAL));
			il.insert(_return, factory.createCast(ObjectType.OBJECT, field.getType()));
			il.insert(_return, InstructionConst.DUP2);
			il.insert(_return, factory.createPutField(className, field.getName(), field.getType()));
			il.insert(_return, factory.createPutField(className, OLD_PREFIX + field.getName(), field.getType()));
			il.setPositions();

			MethodGen ensureLoaded = new MethodGen(PRIVATE_SYNTHETIC, BasicType.VOID, Type.NO_ARGS, null, ENSURE_LOADED_PREFIX + field.getName(), className, il, cpg);
			ensureLoaded.setMaxLocals();
			ensureLoaded.setMaxStack();
			StackMapReplacer.replace(ensureLoaded);
			classGen.addMethod(ensureLoaded.getMethod());
		}

		/**
		 * Adds fields for the old value and the loading state of the fields of a storage class.
		 */
		private void addOldAndIfAlreadyLoadedFields() {
			for (Field field: eagerNonTransientInstanceFields.getLast())
				addOldFieldFor(field);

			for (Field field: lazyNonTransientInstanceFields) {
				addOldFieldFor(field);
				addIfAlreadyLoadedFieldFor(field);
			}
		}

		/**
		 * Adds the field for the loading state of the fields of a storage class.
		 */
		private void addIfAlreadyLoadedFieldFor(Field field) {
			FieldGen ifAlreadyLoaded = new FieldGen(PRIVATE_SYNTHETIC, BasicType.BOOLEAN, IF_ALREADY_LOADED_PREFIX + field.getName(), cpg);
			classGen.addField(ifAlreadyLoaded.getField());
		}

		/**
		 * Adds the field for the old value of the fields of a storage class.
		 */
		private void addOldFieldFor(Field field) {
			FieldGen copy = new FieldGen(field, cpg);
			copy.setName(OLD_PREFIX + field.getName());
			copy.setAccessFlags(PRIVATE_SYNTHETIC);
			classGen.addField(copy.getField());
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
					Type type = field.getType();
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

		/**
		 * Collects the eager and lazy instance fields of the given storage class and of
		 * its superclasses, up to {@link takamaka.lang.Storage} (excluded).
		 * 
		 * @param className the name of the class
		 */
		private void collectNonTransientInstanceFieldsOf(String className) {
			if (!className.equals(STORAGE_CLASS_NAME)) {
				JavaClass clazz = program.get(className);
				if (clazz != null) {
					// we put at the beginning the fields of the superclasses
					collectNonTransientInstanceFieldsOf(clazz.getSuperclassName());

					// then the eager fields of className, in order
					eagerNonTransientInstanceFields.add(Stream.of(clazz.getFields())
							.filter(field -> !field.isStatic() && !field.isTransient() && !isAddedByTakamaka(field) && !isLazilyLoaded(field.getType()))
							.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

					// we collect lazy fields as well, but only for the class being instrumented
					if (className.equals(this.className))
						Stream.of(clazz.getFields())
							.filter(field -> !field.isStatic() && !field.isTransient() && !isAddedByTakamaka(field) && isLazilyLoaded(field.getType()))
							.forEach(lazyNonTransientInstanceFields::add);
				}
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
		private boolean isLazilyLoaded(Type type) {
			return !(type instanceof BasicType || ObjectType.STRING.equals(type) || BIGINTEGER_OT.equals(type) ||
					(type instanceof ObjectType && isEnum(((ObjectType) type).getClassName())));
		}

		/**
		 * Determines if a class is a storage class.
		 * 
		 * @param className the name of the class
		 * @return true if and only if that class extends {@link takamaka.lang.Storage}
		 */
		private boolean isStorage(String className) {
			// we also consider Contract since it is normally not included in the class path of the Takamaka runtime
			if (className.equals(STORAGE_CLASS_NAME) || className.equals(CONTRACT_CLASS_NAME))
				return true;
			else {
				JavaClass clazz = program.get(className);
				if (clazz == null)
					return false;
				else {
					String superclassName = clazz.getSuperclassName();
					return superclassName != null && isStorage(superclassName);
				}
			}
		}

		/**
		 * Determines if a class is an {@code enum}.
		 * 
		 * @param className the name of the class
		 * @return true if and only if that class extends {@link java.lang.Enum}
		 */
		private boolean isEnum(String className) {
			// we also consider Contract since it is normally not included in the class path of the Takamaka runtime
			if (className.equals(ENUM_CLASS_NAME))
				return true;
			else {
				JavaClass clazz = program.get(className);
				if (clazz == null)
					return false;
				else {
					String superclassName = clazz.getSuperclassName();
					return superclassName != null && isEnum(superclassName);
				}
			}
		}

		/**
		 * Determines if a field is transient.
		 * 
		 * @param className the class from which the field must be looked-up
		 * @param fieldName the name of the field
		 * @param fieldType the type of the field
		 * @return true if and only if that condition holds
		 */
		private boolean isTransient(String className, String fieldName, Type fieldType) {
			JavaClass clazz = program.get(className);
			if (clazz == null)
				return false;

			for (Field field: clazz.getFields())
				if (field.getName().equals(fieldName) && field.getType().equals(fieldType))
					return field.isTransient();

			if (className.equals(STORAGE_CLASS_NAME) || className.equals(CONTRACT_CLASS_NAME))
				return false;
			else {
				String superclassName = clazz.getSuperclassName();
				return superclassName != null && isTransient(superclassName, fieldName, fieldType);
			}
		}

		/**
		 * Determines if a field is transient or final.
		 * 
		 * @param className the class from which the field must be looked-up
		 * @param fieldName the name of the field
		 * @param fieldType the type of the field
		 * @return true if and only if that condition holds
		 */
		private boolean isTransientOrFinal(String className, String fieldName, Type fieldType) {
			JavaClass clazz = program.get(className);
			if (clazz == null)
				return false;

			for (Field field: clazz.getFields())
				if (field.getName().equals(fieldName) && field.getType().equals(fieldType))
					return field.isTransient() || field.isFinal();

			if (className.equals(STORAGE_CLASS_NAME) || className.equals(CONTRACT_CLASS_NAME))
				return false;
			else {
				String superclassName = clazz.getSuperclassName();
				return superclassName != null && isTransientOrFinal(superclassName, fieldName, fieldType);
			}
		}

		/**
		 * Checks if a class is a contract or subclass of contract.
		 * 
		 * @param className the name of the class
		 * @return true if and only if that condition holds
		 */
		private boolean isContract(String className) {
			if (className.equals(CONTRACT_CLASS_NAME))
				return true;
			else {
				JavaClass clazz = program.get(className);
				if (clazz == null)
					return false;
				else {
					String superclassName = clazz.getSuperclassName();
					return superclassName != null && isContract(superclassName);
				}
			}
		}
	}
}