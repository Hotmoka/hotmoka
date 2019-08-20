package takamaka.verifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.JsrInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import takamaka.lang.MustBeFalse;
import takamaka.lang.MustRedefineHashCode;
import takamaka.lang.MustRedefineHashCodeOrToString;
import takamaka.lang.WhiteListed;
import takamaka.translator.Dummy;
import takamaka.translator.IncompleteClasspathError;
import takamaka.translator.TakamakaClassLoader;
import takamaka.whitelisted.Anchor;

/**
 * A BCEL class generator, specialized in order to verify some constraints required by Takamaka.
 */
public class VerifiedClassGen extends ClassGen implements Comparable<VerifiedClassGen> {

	/**
	 * The class loader used to load the class under verification and the other classes of the program
	 * it belongs to.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * The bootstrap methods of this class.
	 */
	private final BootstrapMethod[] bootstrapMethods;

	/**
	 * The bootstrap methods of this class that lead to an entry, possibly indirectly.
	 */
	private final Set<BootstrapMethod> bootstrapMethodsLeadingToEntries = new HashSet<>();

	/**
	 * A map from each instruction, in the class under verification, that accesses a field,
	 * to the proof obligation that must be verified in order of being white-listed.
	 * If there is nothing to verify for the access, then no proof obligation is contained for
	 * the instruction.
	 */
	private final Map<Instruction, Field> whiteListingProofObligationForAccessedField = new HashMap<>();

	/**
	 * A map from each instruction, in the class under verification, that calls a constructor,
	 * to the proof obligation that must be verified in order of being white-listed.
	 * If there is nothing to verify for the call, then no proof obligation is contained for
	 * the instruction.
	 */
	private final Map<Instruction, Constructor<?>> whiteListingProofObligationForCalledConstructor = new HashMap<>();

	/**
	 * A map from each instruction, in the class under verification, that calls a method,
	 * to the proof obligation that must be verified in order of being white-listed.
	 * If there is nothing to verify for the call, then no proof obligation is contained for
	 * the instruction.
	 */
	private final Map<Instruction, java.lang.reflect.Method> whiteListingProofObligationForCalledMethod = new HashMap<>();

	private final static String CONTRACT_CLASS_NAME = "takamaka.lang.Contract";

	/**
	 * Builds and verify a BCEL class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param classLoader the Takamaka class loader for the context of the class
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is built during blockchain initialization
	 * @throws VefificationException if the class could not be verified
	 */
	public VerifiedClassGen(JavaClass clazz, TakamakaClassLoader classLoader, Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
		super(clazz);

		this.classLoader = classLoader;
		this.bootstrapMethods = computeBootstraps();
		collectBootstrapsLeadingToEntries();

		new ClassVerification(issueHandler, duringInitialization);
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	/**
	 * Yields the bootstrap methods in this class.
	 * 
	 * @return the bootstrap methods
	 */
	public Stream<BootstrapMethod> getBootstraps() {
		return Stream.of(bootstrapMethods);
	}

	/**
	 * Yields the subset of the bootstrap methods of this class that lead to an entry,
	 * possibly indirectly.
	 * 
	 * @return the bootstrap methods that lead to an entry
	 */
	public Stream<BootstrapMethod> getBootstrapsLeadingToEntries() {
		return bootstrapMethodsLeadingToEntries.stream();
	}

	/**
	 * Yields the bootstrap method associated with the given instruction.
	 * 
	 * @param invokedynamic the instruction
	 * @return the bootstrap method
	 */
	public BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic) {
		ConstantInvokeDynamic cid = (ConstantInvokeDynamic) getConstantPool().getConstant(invokedynamic.getIndex());
		return bootstrapMethods[cid.getBootstrapMethodAttrIndex()];
	}

	/**
	 * Determines if the given bootstrap method calls an entry as target code.
	 * 
	 * @param bootstrap the bootstrap method
	 * @return true if and only if that condition holds
	 */
	public boolean lambdaIsEntry(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			ConstantPoolGen cpg = getConstantPool();

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

					return classLoader.isEntryPossiblyAlreadyInstrumented(className, methodName, methodSignature);
				}
			}
		};

		return false;
	}

	/**
	 * Yields the proof obligation for a field accessed by the given instruction.
	 * This means that that instruction accesses a field but that access is white-listed
	 * only if the resulting proof obligation is verified.
	 * 
	 * @param ins the instruction that accesses the field
	 * @return the proof obligation, if any. This is {@code null} is {@code ins} does not
	 *         access any field or if the access is white-listed, without any proof obligation
	 */
	public Field whiteListingProofObligationForAccessedFieldBy(Instruction ins) {
		return whiteListingProofObligationForAccessedField.get(ins);
	}

	/**
	 * Yields the proof obligation for a constructor called by the given instruction.
	 * This means that that instruction calls a constructor but that call is white-listed
	 * only if the resulting proof obligation is verified.
	 * 
	 * @param ins the instruction that calls the constructor
	 * @return the proof obligation, if any. This is {@code null} is {@code ins} does not
	 *         call any constructor or if the call is white-listed, without any proof obligation
	 */
	public Constructor<?> whiteListingProofObligationForCalledConstructorBy(Instruction ins) {
		return whiteListingProofObligationForCalledConstructor.get(ins);
	}

	/**
	 * Yields the proof obligation for a method called by the given instruction.
	 * This means that that instruction calls a method but that call is white-listed
	 * only if the resulting proof obligation is verified.
	 * 
	 * @param ins the instruction that calls the method
	 * @return the proof obligation, if any. This is {@code null} is {@code ins} does not
	 *         call any method or if the call is white-listed, without any proof obligation
	 */
	public java.lang.reflect.Method whiteListingProofObligationForCalledMethodBy(Instruction ins) {
		return whiteListingProofObligationForCalledMethod.get(ins);
	}

	private BootstrapMethod[] computeBootstraps() {
		Optional<BootstrapMethods> bootstraps = Stream.of(getAttributes())
			.filter(attribute -> attribute instanceof BootstrapMethods)
			.map(attribute -> (BootstrapMethods) attribute)
			.findAny();
	
		if (bootstraps.isPresent())
			return bootstraps.get().getBootstrapMethods();
		else
			return new BootstrapMethod[0];
	}

	private void collectBootstrapsLeadingToEntries() {
		ConstantPoolGen cpg = getConstantPool();

		int initialSize;
		do {
			initialSize = bootstrapMethodsLeadingToEntries.size();
			getBootstraps()
				.filter(bootstrap -> lambdaIsEntry(bootstrap) || lambdaCallsEntry(bootstrap, cpg))
				.forEach(bootstrapMethodsLeadingToEntries::add);
		}
		while (bootstrapMethodsLeadingToEntries.size() > initialSize);
	}

	/**
	 * Yields the lambda bridge method called by the given bootstrap.
	 * It must belong to the same class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the lambda bridge method
	 */
	private Optional<Method> getLambdaFor(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			ConstantPoolGen cpg = getConstantPool();
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
					if (className.equals(getClassName()))
						return Stream.of(getMethods())
							.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
							.findAny();
				}
			}
		}

		return Optional.empty();
	}

	private boolean lambdaCallsEntry(BootstrapMethod bootstrap, ConstantPoolGen cpg) {
		Optional<Method> lambda = getLambdaFor(bootstrap);
		return lambda.isPresent() && callsEntry(lambda.get());
	}

	/**
	 * Determines if the given lambda method calls an entry, possibly indirectly.
	 * 
	 * @param lambda the lambda method
	 * @return true if that condition holds
	 */
	private boolean callsEntry(Method lambda) {
		if (!lambda.isAbstract()) {
			MethodGen mg = new MethodGen(lambda, getClassName(), getConstantPool());
			return StreamSupport.stream(mg.getInstructionList().spliterator(), false)
				.anyMatch(ih -> callsEntry(ih, true));
		}

		return false;
	}

	/**
	 * Determines if the given instruction calls an @Entry, possibly indirectly.
	 * 
	 * @param ih the instruction
	 * @param alsoIndirectly true if the call might also occur indirectly
	 * @return true if and only if that condition holds
	 */
	private boolean callsEntry(InstructionHandle ih, boolean alsoIndirectly) {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof INVOKEDYNAMIC) {
			BootstrapMethod bootstrap = getBootstrapFor((INVOKEDYNAMIC) instruction);
			if (alsoIndirectly)
				return bootstrapMethodsLeadingToEntries.contains(bootstrap);
			else
				return lambdaIsEntry(bootstrap);
		}
		else if (instruction instanceof INVOKESPECIAL || instruction instanceof INVOKEVIRTUAL || instruction instanceof INVOKEINTERFACE) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ConstantPoolGen cpg = getConstantPool();
			ReferenceType receiver = invoke.getReferenceType(cpg);
			if (receiver instanceof ObjectType)
			return classLoader.isEntryPossiblyAlreadyInstrumented
				(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg));
		}

		return false;
	}

	/**
	 * Yields the name of the entry that is directly called by the givuen instruction.
	 * 
	 * @param ih the instruction
	 * @return the name of the entry
	 */
	private String nameOfEntryCalledDirectly(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();
		ConstantPoolGen cpg = getConstantPool();

		if (instruction instanceof INVOKEDYNAMIC) {
			BootstrapMethod bootstrap = getBootstrapFor((INVOKEDYNAMIC) instruction);
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			ConstantMethodHandle mh = (ConstantMethodHandle) constant;
			Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
			ConstantMethodref mr = (ConstantMethodref) constant2;
			ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
			return ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
		}
		else
			return ((InvokeInstruction) instruction).getMethodName(cpg);
	}

	/**
	 * The Java bytecode signature of the {@code caller()} method of {@link #takamaka.lang.Contract}.
	 */
	private final static String TAKAMAKA_CALLER_SIG = "()Ltakamaka/lang/Contract;";

	/**
	 * The BCEL type for BigInteger.
	 */
	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	/**
	 * The algorithms that perform the verification of the BCEL class.
	 */
	private class ClassVerification {

		/**
		 * The name of the class under verification.
		 */
		private final String className;

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		private final Consumer<Issue> issueHandler;

		/**
		 * The constant pool of the class being verified.
		 */
		private final ConstantPoolGen cpg;

		/**
		 * An utility that knows about white-listed methods.
		 */
		private final WhiteListWizard whiteListWizard = new WhiteListWizard();

		/**
		 * The set of lambda methods that might be reachable from a static method
		 * that is not a lambda itself: they cannot call entries.
		 */
		private final Set<Method> lambdasReachableFromStaticMethods = new HashSet<>();

		/**
		 * The set of lambda that are unreachable from static methods that are not lambdas themselves:
		 * they can call entries.
		 */
		private final Set<Method> lambdasUnreachableFromStaticMethods = new HashSet<>();

		/**
		 * True if and only if the code verification occurs during blockchain initialization.
		 */

		private final boolean duringInitialization;

		/**
		 * The class of {@link takamaka.lang.Contract}.
		 */
		private final Class<?> contractClass;

		/**
		 * True if and only if at least an error was issued during verification.
		 */
		private boolean hasErrors;

		/**
		 * Performs the static verification of this class.
		 * 
		 * @param issueHandler the handler to call when an issue is found
		 * @param duringInitialization true if and only if verification is performed during blockchain initialization
		 * @throws VerificationException
		 */
		private ClassVerification(Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
			this.className = getClassName();
			this.issueHandler = issueHandler;
			this.duringInitialization = duringInitialization;
			this.cpg = getConstantPool();

			try {
				contractClass = classLoader.loadClass(CONTRACT_CLASS_NAME);
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}

			if (classLoader.isContract(className))
				computeLambdasUnreachableFromStaticMethods();

			packagesAreLegal();
			bookstrapsAreLegal();
			entryIsOnlyAppliedToInstanceCodeOfContracts();
			entryIsConsistentAlongSubclasses();
			payableIsOnlyAppliedToEntries();
			payableIsConsistentAlongSubclasses();
			payableMethodsReceiveAmount();
			throwsExceptionsIsOnlyAppliedToPublic();
			throwsExceptionsIsConsistentAlongSubclasses();
			storageClassesHaveFieldsOfStorageType();

			Stream.of(getMethods())
				.forEach(MethodVerification::new);

			if (hasErrors)
				throw new VerificationException();
		}

		/**
		 * Checks that only standard bootstrap methods invoking methods are used. For instance,
		 * no bootstraps reading a field are used. Moreover, only standard call-site
		 * resolvers are used for the bootstrap methods. In theory, Java compilers should not
		 * generate the latter.
		 */
		private void bookstrapsAreLegal() {
			for (BootstrapMethod bootstrap: bootstrapMethods) {
				Optional<? extends Executable> target = getTargetOf(bootstrap);
				if (!target.isPresent())
					issue(new IllegalBootstrapMethodError(VerifiedClassGen.this));
			}
		}

		/**
		 * Yields the target method or constructor called by the given bootstrap. It can also be outside
		 * the class that we are processing.
		 * 
		 * @param bootstrap the bootstrap
		 * @return the target called method or constructor
		 */
		private Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) {
			Constant constant = cpg.getConstant(bootstrap.getBootstrapMethodRef());
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

					return getTargetOfCallSite(bootstrap, className, methodName, methodSignature);
				}
			}

			return Optional.empty();
		}

		private Optional<? extends Executable> getTargetOfCallSite(BootstrapMethod bootstrap, String className, String methodName, String methodSignature) {
			if ("java.lang.invoke.LambdaMetafactory".equals(className) &&
					"metafactory".equals(methodName) &&
					"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {

				// this is the standard factory used to create call sites
				Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
				if (constant instanceof ConstantMethodHandle) {
					ConstantMethodHandle mh = (ConstantMethodHandle) constant;
					Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
					if (constant2 instanceof ConstantMethodref) {
						ConstantMethodref mr = (ConstantMethodref) constant2;
						int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
						String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
						ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
						String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
						String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
						Class<?>[] args = classLoader.bcelToClass(Type.getArgumentTypes(methodSignature2));
						Class<?> returnType = classLoader.bcelToClass(Type.getReturnType(methodSignature2));

						if (Const.CONSTRUCTOR_NAME.equals(methodName2))
							return resolveConstructor(className2, args);
						else {
							Optional<java.lang.reflect.Method> result = resolveMethod(className2, methodName2, args, returnType);
							if (result.isPresent())
								return result;

							// we try to add the instrumentation arguments. This is important when
							// a bootstrap calls an entry of a jar already installed (and instrumented)
							// in blockchain. In that case, it will find the target only with these
							// extra arguments added during instrumentation
							Class<?>[] expandedArgs = new Class<?>[args.length + 2];
							System.arraycopy(args, 0, expandedArgs, 0, args.length);
							expandedArgs[args.length] = contractClass;
							expandedArgs[args.length + 1] = Dummy.class;
							return resolveMethod(className2, methodName2, expandedArgs, returnType);
						}
					}
					else if (constant2 instanceof ConstantInterfaceMethodref) {
						ConstantInterfaceMethodref mr = (ConstantInterfaceMethodref) constant2;
						int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
						String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
						ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
						String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
						String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
						Class<?>[] args = classLoader.bcelToClass(Type.getArgumentTypes(methodSignature2));
						Class<?> returnType = classLoader.bcelToClass(Type.getReturnType(methodSignature2));

						Optional<java.lang.reflect.Method> result = resolveInterfaceMethod(className2, methodName2, args, returnType);
						if (result.isPresent())
							return result;

						// we try to add the instrumentation arguments. This is important when
						// a bootstrap calls an entry of a jar already installed (and instrumented)
						// in blockchain. In that case, it will find the target only with these
						// extra arguments added during instrumentation
						Class<?>[] expandedArgs = new Class<?>[args.length + 2];
						System.arraycopy(args, 0, expandedArgs, 0, args.length);
						expandedArgs[args.length] = contractClass;
						expandedArgs[args.length + 1] = Dummy.class;
						return resolveInterfaceMethod(className2, methodName2, expandedArgs, returnType);
					}
				}
			}
			else if ("java.lang.invoke.StringConcatFactory".equals(className) &&
					"makeConcatWithConstants".equals(methodName) &&
					"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {

				// this factory is used to create call sites that lead to string concatenation of every
				// possible argument type. Generically, we yield the string concatenation against Object
				try {
					return Optional.of(String.class.getMethod("concat", String.class));
				}
				catch (NoSuchMethodException | SecurityException e) {
					throw new IncompleteClasspathError(new ClassNotFoundException("java.lang.String"));
				}
			}
			else
				issue(new IllegalCallSiteResolverError(VerifiedClassGen.this, className, methodName));

			return Optional.empty();
		}

		private void packagesAreLegal() {
			if (className.startsWith("java.") || className.startsWith("javax."))
				issue(new IllegalPackageNameError(VerifiedClassGen.this));

			if (!duringInitialization && className.startsWith("takamaka.") && !className.startsWith("takamaka.tests"))
				issue(new IllegalPackageNameError(VerifiedClassGen.this));
		}

		private void computeLambdasUnreachableFromStaticMethods() {
			// we initially compute the set of all lambdas
			Set<Method> lambdas = getBootstraps()
				.map(VerifiedClassGen.this::getLambdaFor)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());

			// then we consider all lambdas that might be called, directly, from a static method
			// that is not a lambda: they must be considered as reachable from a static method
			Stream.of(getMethods())
				.filter(Method::isStatic)
				.filter(method -> !lambdas.contains(method))
				.forEach(this::addCalledLambdasAsReachableFromStatic);

			// then we iterate on the same lambdas that have been found to be reachable from
			// the static methods and process them, recursively
			int initialSize;
			do {
				initialSize = lambdasReachableFromStaticMethods.size();
				new HashSet<>(lambdasReachableFromStaticMethods).stream().forEach(this::addCalledLambdasAsReachableFromStatic);
			}
			while (lambdasReachableFromStaticMethods.size() > initialSize);

			lambdasUnreachableFromStaticMethods.addAll(lambdas);
			lambdasUnreachableFromStaticMethods.removeAll(lambdasReachableFromStaticMethods);
		}

		private void addCalledLambdasAsReachableFromStatic(Method method) {
			MethodGen methodGen = new MethodGen(method, className, cpg);
			InstructionList instructions = methodGen.getInstructionList();
			if (instructions != null) {
				StreamSupport.stream(instructions.spliterator(), false)
					.map(InstructionHandle::getInstruction)
					.filter(instruction -> instruction instanceof INVOKEDYNAMIC)
					.map(instruction -> (INVOKEDYNAMIC) instruction)
					.map(VerifiedClassGen.this::getBootstrapFor)
					.map(VerifiedClassGen.this::getLambdaFor)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.forEach(lambdasReachableFromStaticMethods::add);
			}
		}

		private void issue(Issue issue) {
			issueHandler.accept(issue);
			hasErrors |= issue instanceof Error;
		}

		/**
		 * Checks that {@code @@Entry} is applied only to instance methods or constructors of contracts.
		 */
		private void entryIsOnlyAppliedToInstanceCodeOfContracts() {
			boolean isContract = classLoader.isContract(className);

			for (Method method: getMethods()) {
				Class<?> isEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
				if (isEntry != null) {
					if (!classLoader.contractClass.isAssignableFrom(isEntry))
						issue(new IllegalEntryArgumentError(VerifiedClassGen.this, method));
					if (method.isStatic() || !isContract)
						issue(new IllegalEntryMethodError(VerifiedClassGen.this, method));
				}
			}
		}

		/**
		 * Checks that {@code @@Entry} methods only redefine {@code @@Entry} methods and that
		 * {@code @@Entry} methods are only redefined by {@code @@Entry} methods. Moreover,
		 * the kind of contract allowed in entries can only be enlarged in subclasses.
		 */
		private void entryIsConsistentAlongSubclasses() {
			for (Method method: getMethods())
				if (!method.getName().equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
					Class<?> contractTypeForEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType());

					try {
						isIdenticallyEntryInSupertypesOf(classLoader.loadClass(className), method, contractTypeForEntry);
					}
					catch (ClassNotFoundException e) {
						throw new IncompleteClasspathError(e);
					}
				}
		}

		private void isIdenticallyEntryInSupertypesOf(Class<?> clazz, Method method, Class<?> contractTypeForEntry) {
			String name = method.getName();
			Type returnType = method.getReturnType();
			Type[] args = method.getArgumentTypes();

			if (Stream.of(clazz.getDeclaredMethods())
					.filter(m -> !Modifier.isPrivate(m.getModifiers())
							&& m.getName().equals(name) && m.getReturnType() == classLoader.bcelToClass(returnType)
							&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(args)))
					.anyMatch(m -> !compatibleEntries(contractTypeForEntry, classLoader.isEntry(clazz.getName(), name, args, returnType))))
				issue(new InconsistentEntryError(VerifiedClassGen.this, method, clazz.getName()));

			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null)
				isIdenticallyEntryInSupertypesOf(superclass, method, contractTypeForEntry);

			for (Class<?> interf: clazz.getInterfaces())
				isIdenticallyEntryInSupertypesOf(interf, method, contractTypeForEntry);
		}

		/**
		 * Determines if an entry annotation for a given method in a subclass is compatible with the entry annotation
		 * for a method overridden in a superclass by that method.
		 * 
		 * @param contractTypeInSubclass the type of contracts allowed by the annotation in the subclass
		 * @param contractTypeInSuperclass the type of contracts allowed by the annotation in the superclass
		 * @return true if and only both types are {@code null} or (both are non-{@code null} and
		 *         {@code contractTypeInSubclass} is a non-strict superclass of {@code contractTypeInSuperclass})
		 */
		private boolean compatibleEntries(Class<?> contractTypeInSubclass, Class<?> contractTypeInSuperclass) {
			if (contractTypeInSubclass == null && contractTypeInSuperclass == null)
				return true;
			else
				return contractTypeInSubclass != null && contractTypeInSuperclass != null && contractTypeInSubclass.isAssignableFrom(contractTypeInSuperclass);
		}

		/**
		 * Checks that {@code @@Payable} methods are also annotated as {@code @@Entry}.
		 */
		private void payableIsOnlyAppliedToEntries() {
			for (Method method: getMethods())
				if (classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType())
						&& classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType()) == null)
					issue(new PayableWithoutEntryError(VerifiedClassGen.this, method));
		}

		/**
		 * Checks that {@code @@ThrowsExceptions} methods are public.
		 */
		private void throwsExceptionsIsOnlyAppliedToPublic() {
			for (Method method: getMethods())
				if (!method.isPublic() && classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
					issue(new ThrowsExceptionsOnNonPublicError(VerifiedClassGen.this, method));
		}

		/**
		 * Checks that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
		 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
		 */
		private void payableIsConsistentAlongSubclasses() {
			for (Method method: getMethods())
				if (!method.getName().equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
					boolean wasPayable = classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType());

					try {
						isIdenticallyPayableInSupertypesOf(classLoader.loadClass(className), method, wasPayable);
					}
					catch (ClassNotFoundException e) {
						throw new IncompleteClasspathError(e);
					}
				}
		}

		/**
		 * Checks that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
		 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
		 */
		private void throwsExceptionsIsConsistentAlongSubclasses() {
			for (Method method: getMethods())
				if (!method.getName().equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
					boolean wasThrowsExceptions = classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType());

					try {
						isIdenticallyThrowsExceptionsInSupertypesOf(classLoader.loadClass(className), method, wasThrowsExceptions);
					}
					catch (ClassNotFoundException e) {
						throw new IncompleteClasspathError(e);
					}
				}
		}

		private void isIdenticallyThrowsExceptionsInSupertypesOf(Class<?> clazz, Method method, boolean wasThrowsExceptions) {
			String name = method.getName();
			Type returnType = method.getReturnType();
			Type[] args = method.getArgumentTypes();
		
			if (Stream.of(clazz.getDeclaredMethods())
					.filter(m -> !Modifier.isPrivate(m.getModifiers())
							&& m.getName().equals(name) && m.getReturnType() == classLoader.bcelToClass(returnType)
							&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(args)))
					.anyMatch(m -> wasThrowsExceptions != classLoader.isThrowsExceptions(clazz.getName(), name, args, returnType)))
				issue(new InconsistentThrowsExceptionsError(VerifiedClassGen.this, method, clazz.getName()));
		
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null)
				isIdenticallyThrowsExceptionsInSupertypesOf(superclass, method, wasThrowsExceptions);
		
			for (Class<?> interf: clazz.getInterfaces())
				isIdenticallyThrowsExceptionsInSupertypesOf(interf, method, wasThrowsExceptions);
		}

		private void payableMethodsReceiveAmount() {
			Stream.of(getMethods())
				.filter(method -> classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
				.filter(method -> !startsWithAmount(method))
				.map(method -> new PayableWithoutAmountError(VerifiedClassGen.this, method))
				.forEach(this::issue);
		}

		private boolean startsWithAmount(Method method) {
			Type[] args = method.getArgumentTypes();
			return args.length > 0 && (args[0] == Type.INT || args[0] == Type.LONG || BIG_INTEGER_OT.equals(args[0]));
		}

		private void isIdenticallyPayableInSupertypesOf(Class<?> clazz, Method method, boolean wasPayable) {
			String name = method.getName();
			Type returnType = method.getReturnType();
			Type[] args = method.getArgumentTypes();
		
			if (Stream.of(clazz.getDeclaredMethods())
					.filter(m -> !Modifier.isPrivate(m.getModifiers())
							&& m.getName().equals(name) && m.getReturnType() == classLoader.bcelToClass(returnType)
							&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(args)))
					.anyMatch(m -> wasPayable != classLoader.isPayable(clazz.getName(), name, args, returnType)))
				issue(new InconsistentPayableError(VerifiedClassGen.this, method, clazz.getName()));
		
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null)
				isIdenticallyPayableInSupertypesOf(superclass, method, wasPayable);
		
			for (Class<?> interf: clazz.getInterfaces())
				isIdenticallyPayableInSupertypesOf(interf, method, wasPayable);
		}

		private void storageClassesHaveFieldsOfStorageType() {
			if (classLoader.isStorage(className)) {
				try {
					Class<?> clazz = classLoader.loadClass(className);
					Stream.of(clazz.getDeclaredFields())
						.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
						.filter(field -> !isTypeAllowedForStorageFields(field.getType()))
						.map(field -> new IllegalTypeForStorageFieldError(VerifiedClassGen.this, field))
						.forEach(this::issue);
				}
				catch (ClassNotFoundException e) {
					throw new IncompleteClasspathError(e);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private boolean isTypeAllowedForStorageFields(Class<?> type) {
			return type.isPrimitive() || type == String.class || type == BigInteger.class
				|| (type.isEnum() && !hasInstanceFields((Class<? extends Enum<?>>) type))
				|| (!type.isArray() && classLoader.isStorage(type.getName()))
				// we allow Object since it can be the erasure of a generic type: the runtime of Takamaka
				// will check later if the actual type of the object in this field is allowed
				|| type == Object.class;
		}

		/**
		 * Determines if the given enumeration type has at least an instance, non-transient field.
		 * 
		 * @param clazz the class
		 * @return true only if that condition holds
		 */
		private boolean hasInstanceFields(Class<? extends Enum<?>> clazz) {
			return Stream.of(clazz.getDeclaredFields())
				.map(Field::getModifiers)
				.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
		}

		private Optional<Field> resolveField(String className, String name, Class<?> type) {
			try {
				for (Class<?> clazz = classLoader.loadClass(className); clazz != null; clazz = clazz.getSuperclass()) {
					Optional<Field> result = Stream.of(clazz.getDeclaredFields())
						.filter(field -> field.getType() == type && field.getName().equals(name))
						.findFirst();
		
					if (result.isPresent())
						return result;
				}
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		
			return Optional.empty();
		}

		private Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) {
			try {
				return Stream.of(classLoader.loadClass(className).getDeclaredConstructors())
					.filter(constructor -> Arrays.equals(constructor.getParameterTypes(), args))
					.findFirst();
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		}

		private Optional<java.lang.reflect.Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) {
			try {
				for (Class<?> clazz = classLoader.loadClass(className); clazz != null; clazz = clazz.getSuperclass()) {
					Optional<java.lang.reflect.Method> result = Stream.of(clazz.getDeclaredMethods())
						.filter(method -> method.getReturnType() == returnType && method.getName().equals(methodName)
								&& Arrays.equals(method.getParameterTypes(), args))
						.findFirst();
		
					if (result.isPresent())
						return result;
				}
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		
			return Optional.empty();
		}

		private Optional<java.lang.reflect.Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) {
			try {
				Class<?> clazz = classLoader.loadClass(className);
				Optional<java.lang.reflect.Method> result = Stream.of(clazz.getDeclaredMethods())
					.filter(method -> method.getReturnType() == returnType && method.getName().equals(methodName)
							&& Arrays.equals(method.getParameterTypes(), args))
					.findFirst();
		
					if (result.isPresent())
						return result;

					for (Class<?> interf: clazz.getInterfaces()) {
						result = resolveInterfaceMethod(interf.getName(), methodName, args, returnType);
						if (result.isPresent())
							return result;
					}
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		
			return Optional.empty();
		}

		private class WhiteListWizard {

			/**
			 * Looks for a white-listing model of the given method. That is a method declaration
			 * that justifies why the method is white-listed. It can be the method itself, if it
			 * belongs to a class installed in blockchain, or otherwise a method of a white-listing
			 * class, if it belongs to some Java run-time support class.
			 * 
			 * @param method the method whose model is looked for
			 * @return the model of its white-listing, if it exists
			 */
			private Optional<java.lang.reflect.Method> whiteListingModelOf(java.lang.reflect.Method method) {
				Class<?> declaringClass = method.getDeclaringClass();
			
				// if the class defining the method has been loaded by the blockchain class loader,
				// then it comes from blockchain and the method is white-listed
				if (declaringClass.getClassLoader() == classLoader)
					return Optional.of(method);
				// otherwise, if the method is explicitly annotated as white-listed, it is white-listed
				else if (method.isAnnotationPresent(WhiteListed.class))
					return Optional.of(method);
				else {
					Optional<java.lang.reflect.Method> inLibrary = methodInWhiteListedLibraryFor(method);
					if (inLibrary.isPresent())
						return inLibrary;
				}
			
				// a method might not be explicitly white-listed, but it might override a method
				// of a superclass that is white-listed. Hence we check that possibility
				if (!Modifier.isStatic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())) {
					Class<?> superclass = declaringClass.getSuperclass();
					if (superclass != null) {
						Optional<java.lang.reflect.Method> overridden = resolveMethod(superclass.getName(), method.getName(), method.getParameterTypes(), method.getReturnType());
						if (overridden.isPresent()) {
							Optional<java.lang.reflect.Method> inSuperclass = whiteListingModelOf(overridden.get());
							if (inSuperclass.isPresent())
								return inSuperclass;
						}
					}
			
					for (Class<?> superinterface: declaringClass.getInterfaces()) {
						Optional<java.lang.reflect.Method> overridden = resolveMethod(superinterface.getName(), method.getName(), method.getParameterTypes(), method.getReturnType());
						if (overridden.isPresent()) {
							Optional<java.lang.reflect.Method> inInterface = whiteListingModelOf(overridden.get());
							if (inInterface.isPresent())
								return inInterface;
						}
					}
				}
			
				return Optional.empty();
			}

			private Optional<java.lang.reflect.Method> methodInWhiteListedLibraryFor(java.lang.reflect.Method method) {
				String expandedClassName = Anchor.WHITE_LISTED_ROOT + "." + method.getDeclaringClass().getName();
				Class<?> classInWhiteListedLibrary;
			
				try {
					classInWhiteListedLibrary = Class.forName(expandedClassName);
				}
				catch (ClassNotFoundException e) {
					// the method is not in the library of white-listed code
					return Optional.empty();
				}
			
				Optional<java.lang.reflect.Method> methodInWhiteListedLibrary = Stream.of(classInWhiteListedLibrary.getDeclaredMethods())
					.filter(method2 -> method2.getReturnType() == method.getReturnType() && method2.getName().equals(method.getName())
								&& Arrays.equals(method2.getParameterTypes(), method.getParameterTypes()))
					.findFirst();
			
				return methodInWhiteListedLibrary;
			}

			private Optional<java.lang.reflect.Method> calledNonWhiteListedMethodBy(Instruction ins) {
				if (ins instanceof INVOKEINTERFACE) {
					InvokeInstruction invoke = (InvokeInstruction) ins;
					ReferenceType staticReceiver = invoke.getReferenceType(cpg);
					String methodName = invoke.getMethodName(cpg);
					Class<?>[] args = classLoader.bcelToClass(invoke.getArgumentTypes(cpg));
					Class<?> returnType = classLoader.bcelToClass(invoke.getReturnType(cpg));
			
					Optional<java.lang.reflect.Method> method = resolveInterfaceMethod
						(((ObjectType) staticReceiver).getClassName(), methodName, args, returnType);
			
					if (method.isPresent()) {
						Optional<java.lang.reflect.Method> model = whiteListingModelOf(method.get());
						if (!model.isPresent())
							return method;
						else if (containsProofObligations(model.get()))
							whiteListingProofObligationForCalledMethod.put(ins, model.get());
					}
				}
				else if (ins instanceof INVOKEDYNAMIC) {
					// invokedynamic could can call a target that is an optimized reference to
					// a method: in that case, the method must be white-listed
					BootstrapMethod bootstrap = getBootstrapFor((INVOKEDYNAMIC) ins);
					Executable target = getTargetOf(bootstrap).get(); // we have already verified that it exists

					if (target instanceof java.lang.reflect.Method) {
						Optional<java.lang.reflect.Method> model = whiteListingModelOf((java.lang.reflect.Method) target);
						if (!model.isPresent())
							return Optional.of((java.lang.reflect.Method) target);
						else if (containsProofObligations(model.get()))
							whiteListingProofObligationForCalledMethod.put(ins, model.get());
					}
				}
				else if (ins instanceof InvokeInstruction) {
					InvokeInstruction invoke = (InvokeInstruction) ins;
					ReferenceType staticReceiver = invoke.getReferenceType(cpg);
					String methodName = invoke.getMethodName(cpg);
					Class<?>[] args = classLoader.bcelToClass(invoke.getArgumentTypes(cpg));
					Class<?> returnType = classLoader.bcelToClass(invoke.getReturnType(cpg));
			
					if (staticReceiver instanceof ObjectType) {
						Optional<java.lang.reflect.Method> method = resolveMethod
							(((ObjectType) staticReceiver).getClassName(), methodName, args, returnType);
			
						if (method.isPresent()) {
							Optional<java.lang.reflect.Method> model = whiteListingModelOf(method.get());
							if (!model.isPresent())
								return method;
							else if (containsProofObligations(model.get()))
								whiteListingProofObligationForCalledMethod.put(ins, model.get());
						}
					}
					else {
						// it is possible to call a method on an array: in that case, the callee
						// is a method of java.lang.Object: a couple of them are considered white-listed for arrays
						Optional<java.lang.reflect.Method> method = resolveMethod("java.lang.Object", methodName, args, returnType);
						if (method.isPresent() && !"equals".equals(methodName) && !"clone".equals(methodName))
							return method;
					}
				}
			
				return Optional.empty();
			}

			private boolean containsProofObligations(Executable method) {
				return
					Stream.of(method.getAnnotations())
						.map(Annotation::annotationType)
						.anyMatch(this::isProofObligation)
					
					||
					
					Stream.of(method.getParameterAnnotations())
						.flatMap(Stream::of)
						.map(Annotation::annotationType)
						.anyMatch(this::isProofObligation);
			}

			private boolean containsProofObligations(Field field) {
				return
					Stream.of(field.getAnnotations())
						.map(Annotation::annotationType)
						.anyMatch(this::isProofObligation);
			}


			private boolean isProofObligation(Class<? extends Annotation> annotation) {
				return annotation == MustBeFalse.class ||
					annotation == MustRedefineHashCode.class ||
					annotation == MustRedefineHashCodeOrToString.class;
			}

			/**
			 * Looks for a white-listing model of the given constructor. That is a constructor declaration
			 * that justifies why the constructor is white-listed. It can be the constructor itself, if it
			 * belongs to a class installed in blockchain, or otherwise a constructor of a white-listing
			 * class, if it belongs to some Java run-time support class.
			 * 
			 * @param constructor the constructor whose model is looked for
			 * @return the model of its white-listing, if it exists
			 */
			private Optional<Constructor<?>> whiteListingModelOf(Constructor<?> constructor) {
				// if the class defining the constructor has been loaded by the blockchain class loader,
				// then it comes from blockchain and the constructor is white-listed
				if (constructor.getDeclaringClass().getClassLoader() == classLoader)
					return Optional.of(constructor);
				// otherwise, since constructors cannot be redefined in Java, either is the constructor explicitly
				// annotated as white-listed, or it is not white-listed
				else if (constructor.isAnnotationPresent(WhiteListed.class))
					return Optional.of(constructor);
				else
					return constructorInWhiteListedLibraryFor(constructor);
			}

			private Optional<Constructor<?>> constructorInWhiteListedLibraryFor(Constructor<?> constructor) {
				String expandedClassName = Anchor.WHITE_LISTED_ROOT + "." + constructor.getDeclaringClass().getName();
				Class<?> classInWhiteListedLibrary;
			
				try {
					classInWhiteListedLibrary = Class.forName(expandedClassName);
				}
				catch (ClassNotFoundException e) {
					// the constructor is not in the library of white-listed code
					return Optional.empty();
				}
			
				try {
					// if the constructor has been reported in the white-listed library, then it is automatically white-listed
					return Optional.of(classInWhiteListedLibrary.getDeclaredConstructor(constructor.getParameterTypes()));
				}
				catch (NoSuchMethodException e) {
					return Optional.empty();
				}
			}

			private Optional<Constructor<?>> calledNonWhiteListedConstructorBy(Instruction ins) {
				if (ins instanceof INVOKESPECIAL) {
					INVOKESPECIAL invokespecial = (INVOKESPECIAL) ins;
					if (Const.CONSTRUCTOR_NAME.equals(invokespecial.getMethodName(cpg))) {
						// the type of the receiver of a call to a constructor can only be a class name
						ObjectType receiver = (ObjectType) invokespecial.getReferenceType(cpg);
						Class<?>[] args = classLoader.bcelToClass(invokespecial.getArgumentTypes(cpg));
						Optional<Constructor<?>> constructor = resolveConstructor(receiver.getClassName(), args);

						if (constructor.isPresent()) {
							Optional<Constructor<?>> model = whiteListingModelOf(constructor.get());
							if (!model.isPresent())
								return constructor;
							else if (containsProofObligations(model.get()))
								whiteListingProofObligationForCalledConstructor.put(ins, model.get());
						}
					}
				}
				else if (ins instanceof INVOKEDYNAMIC) {
					// invokedynamic could can call a target that is an optimized reference to
					// a constructor: in that case, the constructor must be white-listed
					BootstrapMethod bootstrap = getBootstrapFor((INVOKEDYNAMIC) ins);
					Executable target = getTargetOf(bootstrap).get(); // we have already verified that it exists

					if (target instanceof Constructor<?>) {
						Optional<Constructor<?>> model = whiteListingModelOf((Constructor<?>) target);
						if (!model.isPresent())
							return Optional.of((Constructor<?>) target);
						else if (containsProofObligations(model.get()))
							whiteListingProofObligationForCalledConstructor.put(ins, model.get());
					}
				}
			
				return Optional.empty();
			}

			/**
			 * Looks for a white-listing model of the given field. That is a field declaration
			 * that justifies why the field is white-listed. It can be the field itself, if it
			 * belongs to a class installed in blockchain, or otherwise a field of a white-listing
			 * class, if it belongs to some Java run-time support class.
			 * 
			 * @param field the field whose model is looked for
			 * @return the model of its white-listing, if it exists
			 */
			private Optional<Field> whiteListingModelOf(Field field) {
				// if the class defining the field has been loaded by the blockchain class loader,
				// then it comes from blockchain and the field is white-listed
				if (field.getDeclaringClass().getClassLoader() == classLoader)
					return Optional.of(field);
				// otherwise, since fields cannot be redefined in Java, either is the field explicitly
				// annotated as white-listed, or it is not white-listed
				else if (field.isAnnotationPresent(WhiteListed.class))
					return Optional.of(field);
				else
					return fieldInWhiteListedLibraryFor(field);
			}

			private Optional<Field> fieldInWhiteListedLibraryFor(Field field) {
				String expandedClassName = Anchor.WHITE_LISTED_ROOT + "." + field.getDeclaringClass().getName();
				Class<?> classInWhiteListedLibrary;
			
				try {
					classInWhiteListedLibrary = Class.forName(expandedClassName);
				}
				catch (ClassNotFoundException e) {
					// the field is not in the library of white-listed code
					return Optional.empty();
				}
			
				return Stream.of(classInWhiteListedLibrary.getDeclaredFields())
					.filter(field2 -> field2.getType() == field.getType() && field2.getName().equals(field.getName()))
					.findFirst();
			}

			private Optional<Field> accessedNonWhiteListedFieldBy(Instruction ins) {
				if (ins instanceof FieldInstruction) {
					FieldInstruction fi = ((FieldInstruction) ins);
					ReferenceType holder = fi.getReferenceType(cpg);
					if (holder instanceof ObjectType) {
						String name = fi.getFieldName(cpg);
						Class<?> type = classLoader.bcelToClass(fi.getFieldType(cpg));
						Optional<Field> field = resolveField(((ObjectType) holder).getClassName(), name, type);
						
						if (field.isPresent()) {
							Optional<Field> model = whiteListingModelOf(field.get());
							if (!model.isPresent())
								return field;
							else if (containsProofObligations(model.get()))
								whiteListingProofObligationForAccessedField.put(ins, model.get());
						}
					}
				}
			
				return Optional.empty();
			}
		}

		private class MethodVerification {
			private final Method method;
			private final InstructionList instructions;
			private final LineNumberTable lines;

			private MethodVerification(Method method) {
				this.method = method;
				MethodGen methodGen = new MethodGen(method, className, cpg);
				this.instructions = methodGen.getInstructionList();
				this.lines = methodGen.getLineNumberTable(cpg);

				isNotStaticInitializer();
				thereAreNoUnusualBytecodes();
				callerOccursOnThisInEntries();
				entriesAreOnlyCalledFromInstanceCodeOfContracts();
				exceptionHandlersAreForCheckedExceptionsOnly();
				onlyWhiteListedCodeIsUsed();
			}

			private void isNotStaticInitializer() {
				if (!method.isAbstract() && method.getName().equals(Const.STATIC_INITIALIZER_NAME))
					if (isEnum() || isSynthetic()) {
						// checks that the static fields of {@code enum}'s or synthetic classes
						// with a static initializer
						// are either {@code synthetic} or {@code enum} elements or final static fields with
						// an explicit constant initializer. This check is necessary
						// since we cannot forbid static initializers in such classes, hence we do at least
						// avoid the existence of extra static fields
						Class<?> clazz;
						try {
							clazz = classLoader.loadClass(className);
							Stream.of(clazz.getDeclaredFields())
								.filter(field -> Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && !field.isEnumConstant()
										&& !(Modifier.isFinal(field.getModifiers()) && hasExplicitConstantValue(field)))
								.findAny()
								.ifPresent(field -> issue(new IllegalStaticInitializationError(VerifiedClassGen.this, method, lineOf(instructions.getStart()))));
						}
						catch (ClassNotFoundException e) {
							throw new IncompleteClasspathError(e);
						}
					}
					else
						issue(new IllegalStaticInitializationError(VerifiedClassGen.this, method, lineOf(instructions.getStart())));
			}

			private boolean hasExplicitConstantValue(Field field) {
				return Stream.of(getFields())
					.filter(f -> f.isStatic() && f.getName().equals(field.getName()) && classLoader.bcelToClass(f.getType()) == field.getType())
					.allMatch(f -> f.getConstantValue() != null);
			}

			/**
			 * Checks that the method has no unusual bytecodes, such as {@code jsr}, {@code ret}
			 * or updates of local 0 in instance methods. Such bytecodes are allowed in
			 * Java bytecode, although they are never generated by modern compilers. Takamaka forbids them
			 * since they make code verification more difficult.
			 */
			private void thereAreNoUnusualBytecodes() {
				if (!method.isAbstract())
					instructions().forEach(this::checkIfItIsIllegal);
			}

			private void checkIfItIsIllegal(InstructionHandle ih) {
				Instruction ins = ih.getInstruction();

				if (ins instanceof PUTSTATIC) {
					// static field updates are allowed inside the synthetic methods or static initializer,
					// for instance in an enumeration
					if (!method.isSynthetic() && !method.getName().equals(Const.STATIC_INITIALIZER_NAME))
						issue(new IllegalPutstaticInstructionError(VerifiedClassGen.this, method, lineOf(ih)));
				}
				else if (ins instanceof JsrInstruction)
					issue(new IllegalJsrInstructionError(VerifiedClassGen.this, method, lineOf(ih)));
				else if (ins instanceof RET)
					issue(new IllegalRetInstructionError(VerifiedClassGen.this, method, lineOf(ih)));
				else if (!method.isStatic() && ins instanceof StoreInstruction && ((StoreInstruction) ins).getIndex() == 0)
					issue(new IllegalUpdateOfLocal0Error(VerifiedClassGen.this, method, lineOf(ih)));					
			}

			/**
			 * Yields the source line number from which the given instruction was compiled.
			 * 
			 * @param ih the instruction
			 * @return the line number, or -1 if not available
			 */
			private int lineOf(InstructionHandle ih) {
				return lines != null ? lines.getSourceLine(ih.getPosition()) : -1;
			}

			private Stream<InstructionHandle> instructions() {
				if (instructions == null)
					return Stream.empty();
				else
					return StreamSupport.stream(instructions.spliterator(), false);
			}

			/**
			 * Checks that {@code caller()}, inside the given method of the class being verified,
			 * is only used with {@code this} as receiver and inside an {@code @@Entry} method or constructor.
			 */
			private void callerOccursOnThisInEntries() {
				boolean isEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType()) != null;

				instructions()
					.filter(this::isCallToContractCaller)
					.forEach(ih -> {
						if (!isEntry)
							issue(new CallerOutsideEntryError(VerifiedClassGen.this, method, lineOf(ih)));
	
						if (!previousIsLoad0(ih))
							issue(new CallerNotOnThisError(VerifiedClassGen.this, method, lineOf(ih)));
					});
			}

			private boolean previousIsLoad0(InstructionHandle ih) {
				// we skip NOPs
				for (ih = ih.getPrev(); ih != null && ih.getInstruction() instanceof NOP; ih = ih.getPrev());

				if (ih != null) {
					Instruction ins = ih.getInstruction();
					return ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0;
				}
				else
					return false;
			}

			private boolean isCallToContractCaller(InstructionHandle ih) {
				Instruction ins = ih.getInstruction();
				if (ins instanceof InvokeInstruction) {
					InvokeInstruction invoke = (InvokeInstruction) ins;
					ReferenceType receiver;
			
					return "caller".equals(invoke.getMethodName(cpg))
						&& TAKAMAKA_CALLER_SIG.equals(invoke.getSignature(cpg))
						&& (receiver = invoke.getReferenceType(cpg)) instanceof ObjectType
						&& classLoader.isContract(((ObjectType) receiver).getClassName());
				}
				else
					return false;
			}

			private void entriesAreOnlyCalledFromInstanceCodeOfContracts() {
				if (!classLoader.isContract(className) || (method.isStatic() && !lambdasUnreachableFromStaticMethods.contains(method)))
					instructions()
						.filter(ih -> callsEntry(ih, false))
						.map(ih -> new IllegalCallToEntryError(VerifiedClassGen.this, method, nameOfEntryCalledDirectly(ih), lineOf(ih)))
						.forEach(ClassVerification.this::issue);
			}

			private void exceptionHandlersAreForCheckedExceptionsOnly() {
				if (!method.isAbstract()) {
					CodeException[] excs = method.getCode().getExceptionTable();
					if (excs != null)
						for (CodeException exc: excs) {
							int classIndex = exc.getCatchType();
							String exceptionName = classIndex == 0 ?
								"java.lang.Throwable" :
								((ConstantClass) cpg.getConstant(classIndex)).getBytes(cpg.getConstantPool()).replace('/', '.');

							// enum's are sometimes compiled with synthetic methods that catch NoSuchFieldError
							if (((isEnum() && method.isSynthetic())
									|| (method.getName().equals(Const.STATIC_INITIALIZER_NAME) && isSynthetic()))
								&& exceptionName.equals("java.lang.NoSuchFieldError"))
								continue;

							if (canCatchUncheckedExceptions(exceptionName))
								issue(new CatchForUncheckedExceptionError(VerifiedClassGen.this, method, lines != null ? lines.getSourceLine(exc.getHandlerPC()) : -1, exceptionName));
						}
				}
			}

			private boolean canCatchUncheckedExceptions(String exceptionName) {
				try {
					Class<?> clazz = classLoader.loadClass(exceptionName);
					return RuntimeException.class.isAssignableFrom(clazz) ||
						clazz.isAssignableFrom(RuntimeException.class) ||
						java.lang.Error.class.isAssignableFrom(clazz) ||
						clazz.isAssignableFrom(java.lang.Error.class);
				}
				catch (ClassNotFoundException e) {
					throw new IncompleteClasspathError(e);
				}
			}

			private void onlyWhiteListedCodeIsUsed() {
				if (instructions != null) {
					for (InstructionHandle ih: instructions) {
						Instruction ins = ih.getInstruction();
						Optional<Field> field = whiteListWizard.accessedNonWhiteListedFieldBy(ins);
						if (field.isPresent())
							issue(new IllegalAccessToNonWhiteListedFieldError(VerifiedClassGen.this, method, lineOf(ih), field.get()));

						Optional<Constructor<?>> constructor = whiteListWizard.calledNonWhiteListedConstructorBy(ins);
						if (constructor.isPresent())
							issue(new IllegalCallToNonWhiteListedConstructorError(VerifiedClassGen.this, method, lineOf(ih), constructor.get()));

						Optional<java.lang.reflect.Method> method = whiteListWizard.calledNonWhiteListedMethodBy(ins);
						if (method.isPresent())
							issue(new IllegalCallToNonWhiteListedMethodError(VerifiedClassGen.this, this.method, lineOf(ih), method.get()));
					}
				}
				//TODO: check @MustBeFalse
				//TODO: check @MustRedefineHashcode
			}
		}
	}
}