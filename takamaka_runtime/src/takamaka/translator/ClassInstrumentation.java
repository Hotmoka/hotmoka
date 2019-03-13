package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
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
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.StackMapType;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
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

import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.Storage;

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
	private final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastUpdateFor";
	private final static String ENTRY_CLASS_NAME_JB = 'L' + Entry.class.getName().replace('.', '/') + ';';
	private final static String PAYABLE_CLASS_NAME_JB = 'L' + Payable.class.getName().replace('.', '/') + ';';
	private final static String CONTRACT_CLASS_NAME = Contract.class.getName();
	private final static String STORAGE_CLASS_NAME = Storage.class.getName();
	private final static short PUBLIC_SYNTHETIC = Const.ACC_PUBLIC | Const.ACC_SYNTHETIC;
	private final static short PROTECTED_SYNTHETIC = Const.ACC_PROTECTED | Const.ACC_SYNTHETIC;
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;

	/**
	 * The order used for generating the parameters of the instrumented constructors.
	 */
	private final static Comparator<Field> fieldOrder = Comparator.comparing(Field::getName).thenComparing(field -> field.getType().toString());

	private final static ObjectType CONTRACT_OT = new ObjectType(CONTRACT_CLASS_NAME);
	private final static ObjectType SET_OT = new ObjectType(Set.class.getName());
	private final static ObjectType LIST_OT = new ObjectType(List.class.getName());
	private final static Type[] THREE_STRINGS = new Type[] { ObjectType.STRING, ObjectType.STRING, ObjectType.STRING };
	private final static Type[] EXTRACT_UPDATES_ARGS = new Type[] { SET_OT, SET_OT, LIST_OT };
	private final static Type[] ADD_UPDATES_FOR_ARGS = new Type[] { ObjectType.STRING, ObjectType.STRING, SET_OT };
	private final static Type[] RECURSIVE_EXTRACT_ARGS = new Type[] { ObjectType.OBJECT, SET_OT, SET_OT, LIST_OT };
	private final static Type[] ENTRY_ARGS = new Type[] { CONTRACT_OT };
	private final static Type[] PAYABLE_ENTRY_ARGS = new Type[] { CONTRACT_OT, Type.INT };

	public ClassInstrumentation(InputStream input, String className, JarOutputStream instrumentedJar, Program program) throws ClassFormatException, IOException {
		LOGGER.fine(() -> "Instrumenting " + className);
		ClassGen classGen = new ClassGen(new ClassParser(input, className).parse());
		new Initializer(classGen, program);
		classGen.getJavaClass().dump(instrumentedJar);
	}

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
		 * True if and only if the class being instrumented is a storage class.
		 */
		private final boolean isStorage;

		/**
		 * True if and only if the class being instrumented is a contract class.
		 */
		private final boolean isContract;

		/**
		 * The non-transient instance fields of primitive type defined in the class being instrumented
		 * and in its superclasses up to Storage (excluded). This is non-empty for storage classes only.
		 */
		private final LinkedList<SortedSet<Field>> primitiveNonTransientInstanceFields = new LinkedList<>();

		/**
		 * The non-transient instance fields of reference type defined in the class being instrumented
		 * (superclasses are not considered). This is non-empty for storage classes only.
		 */
		private final SortedSet<Field> referenceNonTransientInstanceFields = new TreeSet<>(fieldOrder);

		/**
		 * The program that collects the classes under instrumentation and those of the
		 * supporting libraries.
		 */
		private final Program program;

		private Initializer(ClassGen classGen, Program program) {
			this.classGen = classGen;
			this.className = classGen.getClassName();
			this.cpg = classGen.getConstantPool();
			this.factory = new InstructionFactory(cpg);
			this.program = program;
			this.isStorage = isStorage(className);
			this.isContract = isContract(className);

			if (isStorage)
				collectPrimitiveNonTransientInstanceFieldsOf(className);

			instrumentClass();
		}

		private void instrumentClass() {
			localInstrumentation();
			globalInstrumentation();
		}

		private void globalInstrumentation() {
			if (isStorage) {
				addOldAndIfAlreadyLoadedFields();
				addConstructorForDeserializationFromBlockchain();
				addEnsureLoadedMethods();
				addAccessorMethods();
				addExtractUpdates();
			}
		}

		private void localInstrumentation() {
			Method[] methods = classGen.getMethods();

			List<Method> instrumentedMethods =
				Stream.of(methods)
					.map(this::instrument)
					.collect(Collectors.toList());

			int pos = 0;
			for (Method instrumented: instrumentedMethods)
				classGen.replaceMethod(methods[pos++], instrumented);
		}

		/**
		 * Instrument a single method of the class.
		 * 
		 * @param method the method to instrument
		 * @return the result of the instrumentation
		 */
		private Method instrument(Method method) {
			MethodGen methodGen = new MethodGen(method, className, cpg);
			replaceFieldAccessesWithAccessors(methodGen);
			addContractToCallsToEntries(methodGen);
			if (isContract && isEntry(className, method.getName(), method.getSignature()))
				instrumentEntry(methodGen, isPayable(className, method.getName(), method.getSignature()));

			methodGen.setMaxLocals();
			methodGen.setMaxStack();
			return methodGen.getMethod();
		}

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

		private void passContractToCallToEntry(InstructionList il, InstructionHandle ih) {
			il.insert(ih, InstructionConst.ALOAD_0); // the calling contract must be inside "this"
			InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
			Type[] args = invoke.getArgumentTypes(cpg);
			Type[] argsWithContract = new Type[args.length + 1];
			System.arraycopy(args, 0, argsWithContract, 0, args.length);
			argsWithContract[args.length] = CONTRACT_OT;
			InvokeInstruction replacement = factory.createInvoke
					(invoke.getClassName(cpg), invoke.getMethodName(cpg),
					invoke.getReturnType(cpg),
					argsWithContract,
					invoke.getOpcode());
			ih.setInstruction(replacement);
		}

		private boolean isCallToEntry(Instruction instruction) {
			if (instruction instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) instruction;
				ReferenceType receiver = invoke.getReferenceType(cpg);
				return receiver instanceof ObjectType &&
					isEntry(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg));
			}
			else
				return false;
		}

		private boolean isEntry(String className, String methodName, String methodSignature) {
			return hasAnnotation(className, methodName, methodSignature, ENTRY_CLASS_NAME_JB);
		}

		private boolean hasAnnotation(String className, String methodName, String methodSignature, String annotationNameJB) {
			if (className.equals(OBJECT_CLASS_NAME))
				return false;

			JavaClass clazz = program.get(className);
			if (clazz == null)
				return false;

			Optional<Method> definition = Stream.of(clazz.getMethods())
				.filter(m -> m.getName().equals(methodName))
				.filter(m -> m.getSignature().equals(methodSignature))
				.findAny();

			if (definition.isPresent()) {
				if (Stream.of(definition.get().getAnnotationEntries())
					.map(AnnotationEntry::getAnnotationType)
					.anyMatch(annotationNameJB::equals))
					return true;

				if (definition.get().isPrivate())
					return false;
			}

			return !methodName.equals(Const.CONSTRUCTOR_NAME) &&
				(hasAnnotation(clazz.getSuperclassName(), methodName, methodSignature, annotationNameJB) ||
				 Stream.of(clazz.getInterfaceNames())
					.anyMatch(_interface -> hasAnnotation(_interface, methodName, methodSignature, annotationNameJB)));
		}

		private boolean isPayable(String className, String methodName, String methodSignature) {
			return hasAnnotation(className, methodName, methodSignature, PAYABLE_CLASS_NAME_JB);
		}

		private void instrumentEntry(MethodGen method, boolean isPayable) {
			// slotForCaller is the local variable used for the extra "caller" parameter;
			// there is no need to shift the local variables one slot up, since the use
			// of caller is limited to the prolog of the synthetic code
			int slotForCaller = addCallerParameter(method);
			if (!method.isAbstract())
				setPayerAndBalance(method, slotForCaller, isPayable);
		}

		private void setPayerAndBalance(MethodGen method, int slotForCaller, boolean isPayable) {
			InstructionList il = method.getInstructionList();
			InstructionHandle where = determineWhereToSetPayerAndBalance(il, method, slotForCaller);

			il.insert(where, InstructionFactory.createThis());
			il.insert(where, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));

			if (isPayable) {
				il.insert(where, InstructionConst.ILOAD_1);
				il.insert(where, factory.createInvoke(className, PAYABLE_ENTRY, Type.VOID, PAYABLE_ENTRY_ARGS, Const.INVOKESPECIAL));
			}
			else
				il.insert(where, factory.createInvoke(className, ENTRY, Type.VOID, ENTRY_ARGS, Const.INVOKESPECIAL));
		}

		/**
		 * Entries call {@code Contract.entry} or {@code Contract.payableEntry} at their beginning, to set the caller and
		 * the balance of the called contract. In general, such call can be placed at the very beginning of the
		 * code. The only problem is related to constructors, that require their code to start with a call
		 * to a constructor of their superclass. In that case, this method finds the place where that
		 * contractor of the superclass is called: after that, the call to {@code Contract.entry} or
		 * {@code Contract.payableEntry} can be added.
		 * 
		 * @param il the list of instructions of the method
		 * @param method the method
		 * @param slotForCaller the local where the caller contract is passed to the method or constructor
		 * @return the instruction before which the call to {@code Contract.entry} or {@code Contract.payableEntry} must be placed
		 */
		private InstructionHandle determineWhereToSetPayerAndBalance(InstructionList il, MethodGen method, int slotForCaller) {
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
								throw new RuntimeException("Unexpected modification of local " + slotForCaller + " before initialization of " + className);
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
								throw new RuntimeException("Unexpected consumer of local 0 " + bytecode + " before initialization of " + className);
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
							throw new RuntimeException("Unexpected instruction " + bytecode + " before initialization of " + className);
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
						throw new RuntimeException("Cannot identify single call to constructor of superclass inside a constructor ot " + className);
				}
				else
					throw new RuntimeException("Constructor of " + className + " does not start with aload 0");
			}
			else
				return start;
		}

		/**
		 * Adds an extra {@code caller} parameter to the given method.
		 * 
		 * @param method the method
		 * @return the local variable used for the extra parameter
		 */
		private int addCallerParameter(MethodGen method) {
			List<Type> args = new ArrayList<>();
			int slotsForParameters = 0;
			for (Type arg: method.getArgumentTypes()) {
				args.add(arg);
				slotsForParameters += arg.getSize();
			}
			args.add(CONTRACT_OT);
			method.setArgumentTypes(args.toArray(Type.NO_ARGS));

			String[] names = method.getArgumentNames();
			if (names != null) {
				List<String> namesAsList = new ArrayList<>();
				for (String name: names)
					namesAsList.add(name);
				namesAsList.add("caller");
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
					.filter(ih -> isAccessToReferenceFieldInStorageClass(ih.getInstruction()))
					.forEach(ih -> ih.setInstruction(accessorCorrespondingTo((FieldInstruction) ih.getInstruction())));
			}
		}

		private Instruction accessorCorrespondingTo(FieldInstruction fieldInstruction) {
			ObjectType referencedClass = (ObjectType) fieldInstruction.getReferenceType(cpg);
			Type fieldType = fieldInstruction.getFieldType(cpg);
			String fieldName = fieldInstruction.getFieldName(cpg);

			if (fieldInstruction instanceof GETFIELD)
				// it is important to use an invokespecial, since fields cannot be redefined in Java
				return factory.createInvoke(referencedClass.getClassName(), GETTER_PREFIX + fieldName, fieldType, Type.NO_ARGS, Const.INVOKESPECIAL);
			else // PUTFIELD
				// it is important to use an invokespecial, since fields cannot be redefined in Java
				return factory.createInvoke(referencedClass.getClassName(), SETTER_PREFIX + fieldName, Type.VOID, new Type[] { fieldType }, Const.INVOKESPECIAL);
		}

		private boolean isAccessToReferenceFieldInStorageClass(Instruction instruction) {
			return (instruction instanceof GETFIELD || instruction instanceof PUTFIELD)
				&& isStorage(((ObjectType) ((FieldInstruction) instruction).getReferenceType(cpg)).getClassName())
				&& ((FieldInstruction) instruction).getFieldType(cpg) instanceof ReferenceType;
		}

		private void addExtractUpdates() {
			if (primitiveNonTransientInstanceFields.getLast().isEmpty() && referenceNonTransientInstanceFields.isEmpty())
				return;

			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(InstructionConst.ALOAD_1);
			il.append(InstructionConst.ALOAD_2);
			il.append(InstructionFactory.createLoad(LIST_OT, 3));
			il.append(factory.createInvoke(classGen.getSuperclassName(), EXTRACT_UPDATES, Type.VOID, EXTRACT_UPDATES_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(Storage.class.getName(), IN_STORAGE_NAME, Type.BOOLEAN));
			il.append(InstructionFactory.createStore(Type.BOOLEAN, 4));

			InstructionHandle end = il.append(InstructionConst.RETURN);
			LinkedList<InstructionHandle> stackMapPositions = new LinkedList<>();

			for (Field field: primitiveNonTransientInstanceFields.getLast())
				end = addUpdateExtractionForPrimitiveField(field, il, end, stackMapPositions);

			for (Field field: referenceNonTransientInstanceFields)
				end = addUpdateExtractionForReferenceField(field, il, end, stackMapPositions);

			MethodGen extractUpdates = new MethodGen(PROTECTED_SYNTHETIC, Type.VOID, EXTRACT_UPDATES_ARGS, null, EXTRACT_UPDATES, className, il, cpg);
			il.setPositions();
			extractUpdates.setMaxLocals();
			extractUpdates.setMaxStack();

			List<StackMapEntry> stackMapEntries = new ArrayList<>();
			int lastPosition = -1;
			for (InstructionHandle ih: stackMapPositions) {
				if (lastPosition < 0)
					stackMapEntries.add(mkSameStackMapEntryWithExtraIntLocal(ih.getPosition() - lastPosition - 1));
				else
					stackMapEntries.add(mkSameStackMapEntry(ih.getPosition() - lastPosition - 1));

				lastPosition = ih.getPosition();
			}

			extractUpdates.addCodeAttribute(mkStackMap(4 + (stackMapEntries.size() - 1), stackMapEntries.toArray(new StackMapEntry[stackMapEntries.size()])));
			classGen.addMethod(extractUpdates.getMethod());
		}

		private InstructionHandle addUpdateExtractionForReferenceField(Field field, InstructionList il, InstructionHandle end, LinkedList<InstructionHandle> stackMapPositions) {
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
			if (type.equals(ObjectType.STRING) || type.getClassName().equals(BigInteger.class.getName()))
				recursiveExtract = end;
			else {
				recursiveExtract = il.insert(end, InstructionFactory.createThis());
				il.insert(end, InstructionConst.DUP);
				il.insert(end, factory.createGetField(className, OLD_PREFIX + field.getName(), type));
				il.insert(end, InstructionConst.ALOAD_1);
				il.insert(end, InstructionConst.ALOAD_2);
				il.insert(end, InstructionFactory.createLoad(LIST_OT, 3));
				il.insert(end, factory.createInvoke(Storage.class.getName(), RECURSIVE_EXTRACT, Type.VOID, RECURSIVE_EXTRACT_ARGS, Const.INVOKESPECIAL));
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
			il.insert(recursiveExtract, factory.createInvoke(Storage.class.getName(), ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ACMPEQ, recursiveExtract));

			stackMapPositions.addFirst(recursiveExtract);
			stackMapPositions.addFirst(addUpdatesFor);

			return start;
		}

		private InstructionHandle addUpdateExtractionForPrimitiveField(Field field, InstructionList il, InstructionHandle end, LinkedList<InstructionHandle> stackMapPositions) {
			Type type = field.getType();

			List<Type> args = new ArrayList<>();
			for (Type arg: ADD_UPDATES_FOR_ARGS)
				args.add(arg);
			args.add(type);

			InstructionHandle addUpdatesFor = il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createConstant(className));
			il.insert(end, factory.createConstant(field.getName()));
			il.insert(end, InstructionConst.ALOAD_1);
			il.insert(end, InstructionFactory.createThis());
			il.insert(end, factory.createGetField(className, field.getName(), type));
			il.insert(end, factory.createInvoke(Storage.class.getName(), ADD_UPDATE_FOR, Type.VOID, args.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));

			InstructionHandle start = il.insert(addUpdatesFor, InstructionFactory.createLoad(Type.BOOLEAN, 4));
			il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, addUpdatesFor));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, field.getName(), type));
			il.insert(addUpdatesFor, InstructionFactory.createThis());
			il.insert(addUpdatesFor, factory.createGetField(className, OLD_PREFIX + field.getName(), type));

			if (field.getType().equals(Type.DOUBLE)) {
				il.insert(addUpdatesFor, InstructionConst.DCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (field.getType().equals(Type.FLOAT)) {
				il.insert(addUpdatesFor, InstructionConst.FCMPL);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else if (field.getType().equals(Type.LONG)) {
				il.insert(addUpdatesFor, InstructionConst.LCMP);
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IFEQ, end));
			}
			else
				il.insert(addUpdatesFor, InstructionFactory.createBranchInstruction(Const.IF_ICMPEQ, end));

			stackMapPositions.addFirst(end);
			stackMapPositions.addFirst(addUpdatesFor);

			return start;
		}

		private void addAccessorMethods() {
			referenceNonTransientInstanceFields.forEach(this::addAccessorMethodsFor);
		}

		private void addAccessorMethodsFor(Field field) {
			addGetterFor(field);
			addSetterFor(field);
		}

		private void addSetterFor(Field field) {
			Type type = field.getType();
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
			il.append(InstructionConst.ALOAD_1);
			il.append(factory.createPutField(className, field.getName(), type));
			il.append(InstructionConst.RETURN);

			MethodGen setter = new MethodGen(modifiersFrom(field), BasicType.VOID, new Type[] { type }, null, SETTER_PREFIX + field.getName(), className, il, cpg);
			setter.setMaxLocals();
			setter.setMaxStack();
			classGen.addMethod(setter.getMethod());
		}

		private short modifiersFrom(Field field) {
			short modifiers = Const.ACC_SYNTHETIC;
			if (field.isPrivate())
				modifiers |= Const.ACC_PRIVATE;
			else if (field.isProtected())
				modifiers |= Const.ACC_PROTECTED;
			else if (field.isPublic())
				modifiers |= Const.ACC_PUBLIC;
			return modifiers;
		}

		private void addGetterFor(Field field) {
			Type type = field.getType();
			InstructionList il = new InstructionList();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.DUP);
			il.append(factory.createInvoke(className, ENSURE_LOADED_PREFIX + field.getName(), BasicType.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
			il.append(factory.createGetField(className, field.getName(), type));
			il.append(InstructionFactory.createReturn(type));

			MethodGen getter = new MethodGen(modifiersFrom(field), type, Type.NO_ARGS, null, GETTER_PREFIX + field.getName(), className, il, cpg);
			getter.setMaxLocals();
			getter.setMaxStack();
			classGen.addMethod(getter.getMethod());
		}

		private void addEnsureLoadedMethods() {
			referenceNonTransientInstanceFields.forEach(this::addEnsureLoadedMethodFor);
		}

		private void addEnsureLoadedMethodFor(Field field) {
			InstructionList il = new InstructionList();
			InstructionHandle _return = il.append(InstructionConst.RETURN);
			il.insert(_return, InstructionFactory.createThis());
			il.insert(_return, factory.createGetField(Storage.class.getName(), IN_STORAGE_NAME, BasicType.BOOLEAN));
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
			il.insert(_return, factory.createInvoke(className, DESERIALIZE_LAST_UPDATE_FOR, ObjectType.OBJECT, THREE_STRINGS, Const.INVOKEVIRTUAL));
			il.insert(_return, factory.createCast(ObjectType.OBJECT, field.getType()));
			il.insert(_return, factory.createPutField(className, OLD_PREFIX + field.getName(), field.getType()));
			il.setPositions();

			MethodGen ensureLoaded = new MethodGen(PRIVATE_SYNTHETIC, BasicType.VOID, Type.NO_ARGS, null, ENSURE_LOADED_PREFIX + field.getName(), className, il, cpg);
			ensureLoaded.setMaxLocals();
			ensureLoaded.setMaxStack();
			StackMap stackMap = mkStackMap(1, new StackMapEntry[] { mkSameStackMapEntry(_return.getPosition()) });
			ensureLoaded.addCodeAttribute(stackMap);
			classGen.addMethod(ensureLoaded.getMethod());
		}

		private StackMap mkStackMap(int totalLength, StackMapEntry[] entries) {
			int attribute_name_index = cpg.addUtf8("StackMapTable");
			int attribute_length = 2 + totalLength;

			return new StackMap(attribute_name_index, attribute_length, entries, cpg.getConstantPool());
		}

		private StackMapEntry mkSameStackMapEntry(int offset) {
			if (offset >= Const.SAME_FRAME && offset <= Const.SAME_FRAME_MAX)
				return new StackMapEntry(offset, offset, null, null, cpg.getConstantPool());
			else
				return new StackMapEntry(Const.SAME_FRAME_EXTENDED, offset, null, null, cpg.getConstantPool());
		}

		private StackMapEntry mkSameStackMapEntryWithExtraIntLocal(int offset) {
			ConstantPool cp = cpg.getConstantPool();
			return new StackMapEntry(Const.APPEND_FRAME, offset, new StackMapType[] { new StackMapType(Const.ITEM_Integer, -1, cp) }, null, cp);
		}

		private void addOldAndIfAlreadyLoadedFields() {
			for (Field field: primitiveNonTransientInstanceFields.getLast())
				addOldFieldFor(field);

			for (Field field: referenceNonTransientInstanceFields) {
				addOldFieldFor(field);
				addIfAlreadyLoadedFieldFor(field);
			}
		}

		private void addIfAlreadyLoadedFieldFor(Field field) {
			FieldGen ifAlreadyLoaded = new FieldGen(PRIVATE_SYNTHETIC, BasicType.BOOLEAN, IF_ALREADY_LOADED_PREFIX + field.getName(), cpg);
			classGen.addField(ifAlreadyLoaded.getField());
		}

		private void addOldFieldFor(Field field) {
			FieldGen copy = new FieldGen(field, cpg);
			copy.setName(OLD_PREFIX + field.getName());
			copy.setAccessFlags(PRIVATE_SYNTHETIC);
			classGen.addField(copy.getField());
		}

		private void addConstructorForDeserializationFromBlockchain() {
			List<Type> args = new ArrayList<>();

			// the parameters of the constructor start with a storage reference
			// to the object being deserialized
			args.add(new ObjectType(StorageReference.class.getName()));

			// then there are the fields of the class and superclasses, with superclasses first
			primitiveNonTransientInstanceFields.stream()
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.forEachOrdered(args::add);

			InstructionList il = new InstructionList();
			int nextLocal = addCallToSuper(il);
			addInitializationOfPrimitiveFields(il, nextLocal);
			il.append(InstructionConst.RETURN);

			MethodGen constructor = new MethodGen(PUBLIC_SYNTHETIC, BasicType.VOID, args.toArray(Type.NO_ARGS), null, Const.CONSTRUCTOR_NAME, className, il, cpg);
			constructor.setMaxLocals();
			constructor.setMaxStack();
			classGen.addMethod(constructor.getMethod());
		}

		private int addCallToSuper(InstructionList il) {
			List<Type> argsForSuperclasses = new ArrayList<>();
			il.append(InstructionFactory.createThis());
			il.append(InstructionConst.ALOAD_1);
			argsForSuperclasses.add(new ObjectType(StorageReference.class.getName()));
		
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
			primitiveNonTransientInstanceFields.stream()
				.limit(primitiveNonTransientInstanceFields.size() - 1)
				.flatMap(SortedSet::stream)
				.map(Field::getType)
				.forEachOrdered(pushLoad);
		
			il.append(factory.createInvoke(classGen.getSuperclassName(), Const.CONSTRUCTOR_NAME, BasicType.VOID, argsForSuperclasses.toArray(Type.NO_ARGS), Const.INVOKESPECIAL));
		
			return pushLoad.local;
		}

		private void addInitializationOfPrimitiveFields(InstructionList il, int nextLocal) {
			Consumer<Field> pushField = new Consumer<Field>() {
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
			
			primitiveNonTransientInstanceFields.getLast().forEach(pushField);
		}

		private void collectPrimitiveNonTransientInstanceFieldsOf(String className) {
			if (!className.equals(Storage.class.getName())) {
				JavaClass clazz = program.get(className);
				if (clazz != null) {
					// we put at the beginning the fields of the superclasses
					collectPrimitiveNonTransientInstanceFieldsOf(clazz.getSuperclassName());

					// then the fields of className, in order
					primitiveNonTransientInstanceFields.add(Stream.of(clazz.getFields())
						.filter(field -> !field.isStatic() && !field.isTransient() && field.getType() instanceof BasicType)
						.collect(Collectors.toCollection(() -> new TreeSet<>(fieldOrder))));

					// we collect reference fields as well, but only for the class being instrumented
					if (className.equals(this.className))
						Stream.of(clazz.getFields())
							.filter(field -> !field.isStatic() && !field.isTransient() && field.getType() instanceof ReferenceType)
							.forEach(referenceNonTransientInstanceFields::add);
				}
			}
		}

		private boolean isStorage(String className) {
			if (className.equals(STORAGE_CLASS_NAME))
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