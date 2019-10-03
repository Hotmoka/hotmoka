package takamaka.verifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
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
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import takamaka.translator.Dummy;
import takamaka.translator.IncompleteClasspathError;
import takamaka.translator.TakamakaClassLoader;
import takamaka.verifier.checks.onClass.BootstrapsAreLegalCheck;
import takamaka.verifier.checks.onClass.PackagesAreLegalCheck;
import takamaka.verifier.checks.onClass.StorageClassesHaveFieldsOfStorageTypeCheck;
import takamaka.verifier.checks.onMethod.BytecodesAreLegalCheck;
import takamaka.verifier.checks.onMethod.CallerIsUsedOnThisAndInEntryCheck;
import takamaka.verifier.checks.onMethod.EntriesAreOnlyCalledFromContractsCheck;
import takamaka.verifier.checks.onMethod.EntryCodeIsConsistentWithClassHierarchyCheck;
import takamaka.verifier.checks.onMethod.EntryCodeIsInstanceAndInContractsCheck;
import takamaka.verifier.checks.onMethod.ExceptionHandlersAreForCheckedExceptionsCheck;
import takamaka.verifier.checks.onMethod.IsNotNativeCheck;
import takamaka.verifier.checks.onMethod.IsNotStaticInitializerCheck;
import takamaka.verifier.checks.onMethod.IsNotSynchronizedCheck;
import takamaka.verifier.checks.onMethod.PayableCodeIsConsistentWithClassHierarchyCheck;
import takamaka.verifier.checks.onMethod.PayableCodeIsEntryCheck;
import takamaka.verifier.checks.onMethod.PayableCodeReceivesAmountCheck;
import takamaka.verifier.checks.onMethod.ThrowsExceptionsCodeIsPublicCheck;
import takamaka.verifier.checks.onMethod.ThrowsExceptionsIsConsistentWithClassHierarchyCheck;
import takamaka.verifier.checks.onMethod.UsedCodeIsWhiteListedCheck;
import takamaka.whitelisted.MustRedefineHashCode;
import takamaka.whitelisted.MustRedefineHashCodeOrToString;

/**
 * A BCEL class, that passed the static Takamaka verification tests.
 */
public class VerifiedClassGen extends ClassGen implements Comparable<VerifiedClassGen> {

	/**
	 * The class loader used to load this class and the other classes of the program it belongs to.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * The object that provides utilities about the lambda bootstraps contained in this class.
	 */
	private final ClassBootstraps classBootstraps;

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
		this.classBootstraps = new ClassBootstraps(this);
		new Verifier(issueHandler, duringInitialization);
	}

	/**
	 * Yields the class loader used to load this class and the other classes of the program it belongs to.
	 * 
	 * @return the class loader
	 */
	public TakamakaClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Yields an object that provides utility methods about lambda bootstraps in this class.
	 * 
	 * @return the utility
	 */
	public ClassBootstraps getClassBootstraps() {
		return classBootstraps;
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	/**
	 * Yields the proof obligation for the field accessed by the given instruction.
	 * This means that that instruction accesses that field but that access is white-listed
	 * only if the resulting proof obligation is verified.
	 * 
	 * @param fi the instruction that accesses the field
	 * @return the proof obligation. This must exist, since the class is verified
	 *         and all accesses have been proved to be white-listed (up to possible proof obligations
	 *         contained in the model).
	 */
	public Field whiteListingModelOf(FieldInstruction fi) {
		// it has already been verified that the access is white-listed, hence it must exist
		return classLoader.whiteListingWizard.whiteListingModelOf(resolvedFieldFor(fi).get()).get();
	}

	/**
	 * Yields the proof obligation for the method called by the given instruction.
	 * This means that that instruction calls that method but that call is white-listed
	 * only if the resulting proof obligation is verified.
	 * 
	 * @param invoke the instruction that calls the method
	 * @return the proof obligation. This must exist, since the class is verified
	 *         and all calls have been proved to be white-listed (up to possible proof obligations
	 *         contained in the model).
	 */
	public Executable whiteListingModelOf(InvokeInstruction invoke) {
		// it has already been verified that the access is white-listed, hence it must exist
		return whiteListingModelOf(resolvedExecutableFor(invoke).get(), invoke).get();
	}

	private Optional<Field> resolvedFieldFor(FieldInstruction ins) {
		ConstantPoolGen cpg = getConstantPool();
	
		ReferenceType holder = ins.getReferenceType(cpg);
		if (holder instanceof ObjectType) {
			String name = ins.getFieldName(cpg);
			Class<?> type = classLoader.bcelToClass(ins.getFieldType(cpg));

			return IncompleteClasspathError.insteadOfClassNotFoundException
				(() -> classLoader.resolveField(((ObjectType) holder).getClassName(), name, type));
		}
	
		return Optional.empty();
	}

	private Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction ins) {
		ConstantPoolGen cpg = getConstantPool();

		if (ins instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(ins.getMethodName(cpg))) {
			// the type of the receiver of a call to a constructor can only be a class
			ObjectType receiver = (ObjectType) ins.getReferenceType(cpg);
			Class<?>[] args = classLoader.bcelToClass(ins.getArgumentTypes(cpg));
			return resolveConstructorWithPossiblyExpandedArgs(receiver.getClassName(), args);
		}
		else if (ins instanceof INVOKEDYNAMIC)
			// invokedynamic can call a target that is an optimized reference to an executable
			return getTargetOf(classBootstraps.getBootstrapFor((INVOKEDYNAMIC) ins));
		else if (ins instanceof INVOKEINTERFACE) {
			ReferenceType receiver = ins.getReferenceType(cpg);
			String methodName = ins.getMethodName(cpg);
			Class<?>[] args = classLoader.bcelToClass(ins.getArgumentTypes(cpg));
			Class<?> returnType = classLoader.bcelToClass(ins.getReturnType(cpg));
	
			return resolveInterfaceMethodWithPossiblyExpandedArgs(((ObjectType) receiver).getClassName(), methodName, args, returnType);
		}
		else {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			String methodName = invoke.getMethodName(cpg);
			Class<?>[] args = classLoader.bcelToClass(invoke.getArgumentTypes(cpg));
			Class<?> returnType = classLoader.bcelToClass(invoke.getReturnType(cpg));
	
			if (receiver instanceof ObjectType)
				return resolveMethodWithPossiblyExpandedArgs(((ObjectType) receiver).getClassName(), methodName, args, returnType);
			else
				// it is possible to call a method on an array: in that case, the callee
				// is a method of java.lang.Object: a couple of them are considered white-listed for arrays
				return resolveMethodWithPossiblyExpandedArgs("java.lang.Object", methodName, args, returnType);
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
		ConstantPoolGen cpg = getConstantPool();

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
		ConstantPoolGen cpg = getConstantPool();

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
						return resolveConstructorWithPossiblyExpandedArgs(className2, args);
					else
						return resolveMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
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
	
					return resolveInterfaceMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
			}
		}
		else if ("java.lang.invoke.StringConcatFactory".equals(className) &&
				"makeConcatWithConstants".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {
	
			// this factory is used to create call sites that lead to string concatenation of every
			// possible argument type. Generically, we yield the Objects.toString(Object) method, since
			// all parameters must be checked in order for the call to be white-listed
			try {
				return Optional.of(Objects.class.getMethod("toString", Object.class));
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new IncompleteClasspathError(new ClassNotFoundException("java.util.Objects"));
			}
		}
	
		return Optional.empty();
	}

	/**
	 * Looks for a white-listing model of the given method or constructor. That is a constructor declaration
	 * that justifies why the method or constructor is white-listed. It can be the method or constructor itself, if it
	 * belongs to a class installed in blockchain, or otherwise a method or constructor of a white-listing
	 * class, if it belongs to some Java run-time support class. If the instruction is a special call
	 * to a method of a superclass, it checks that white-listing annotations on the receiver are not fooled.
	 * 
	 * @param executable the method or constructor whose model is looked for
	 * @param invoke the call to the method or constructor
	 * @return the model of its white-listing, if it exists
	 */
	private Optional<? extends Executable> whiteListingModelOf(Executable executable, InvokeInstruction invoke) {
		return IncompleteClasspathError.insteadOfClassNotFoundException
			(() -> checkINVOKESPECIAL(invoke, classLoader.whiteListingWizard.whiteListingModelOf(executable)));
	}

	/**
	 * If the given invoke instruction is an {@code invokespecial} and the given model
	 * is annotated with {@link takamaka.lang.MustRedefineHashCode} or with
	 * {@link takamaka.lang.MustRedefineHashCodeOrToString}, checks if the model
	 * resolved target of the invoke is not in {@code java.lang.Object}. This check
	 * is important in order to forbid calls such as super.hashCode() to the hashCode()
	 * method of Object, that would be non-deterministic.
	 * 
	 * @param invoke the invoke instruction
	 * @param model the white-listing model of the invoke
	 * @return the optional containing the model, or the empty optional if the check fails
	 */
	private Optional<? extends Executable> checkINVOKESPECIAL(InvokeInstruction invoke, Optional<? extends Executable> model) {
		if (invoke instanceof INVOKESPECIAL &&
			model.isPresent() &&
			(model.get().isAnnotationPresent(MustRedefineHashCode.class) ||
			 model.get().isAnnotationPresent(MustRedefineHashCodeOrToString.class)) &&
			resolvedExecutableFor(invoke).get().getDeclaringClass() == Object.class)
			return Optional.empty();
		else
			return model;
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

	Optional<Constructor<?>> resolveConstructorWithPossiblyExpandedArgs(String className, Class<?>[] args) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<Constructor<?>> result = classLoader.resolveConstructor(className, args);
			// we try to add the instrumentation arguments. This is important when
			// a bootstrap calls an entry of a jar already installed (and instrumented)
			// in blockchain. In that case, it will find the target only with these
			// extra arguments added during instrumentation
			return result.isPresent() ? result : classLoader.resolveConstructor(className, expandArgsForEntry(args));
		});
	}

	Optional<java.lang.reflect.Method> resolveMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<java.lang.reflect.Method> result = classLoader.resolveMethod(className, methodName, args, returnType);
			return result.isPresent() ? result : classLoader.resolveMethod(className, methodName, expandArgsForEntry(args), returnType);
		});
	}

	Optional<java.lang.reflect.Method> resolveInterfaceMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<java.lang.reflect.Method> result = classLoader.resolveInterfaceMethod(className, methodName, args, returnType);
			return result.isPresent() ? result : classLoader.resolveInterfaceMethod(className, methodName, expandArgsForEntry(args), returnType);
		});
	}

	private Class<?>[] expandArgsForEntry(Class<?>[] args) throws ClassNotFoundException {
		Class<?>[] expandedArgs = new Class<?>[args.length + 2];
		System.arraycopy(args, 0, expandedArgs, 0, args.length);
		expandedArgs[args.length] = classLoader.contractClass;
		expandedArgs[args.length + 1] = Dummy.class;
		return expandedArgs;
	}

	/**
	 * The algorithms that perform the verification of the BCEL class.
	 */
	public class Verifier {

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		private final Consumer<Issue> issueHandler;

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
		 * True if and only if at least an error was issued during verification.
		 */
		private boolean hasErrors;

		/**
		 * Performs the static verification of this class.
		 * 
		 * @param issueHandler the handler to call when an issue is found
		 * @param duringInitialization true if and only if verification is performed during blockchain initialization
		 * @throws VerificationException if some verification error occurs
		 */
		private Verifier(Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
			this.issueHandler = issueHandler;
			this.duringInitialization = duringInitialization;

			if (classLoader.isContract(getClassName()))
				computeLambdasUnreachableFromStaticMethods();

			new PackagesAreLegalCheck(this);
			new BootstrapsAreLegalCheck(this);
			new StorageClassesHaveFieldsOfStorageTypeCheck(this);

			Stream.of(getMethods())
				.forEach(MethodVerifier::new);

			if (hasErrors)
				throw new VerificationException();
		}

		public abstract class Check {
			protected final ClassBootstraps classBootstraps = VerifiedClassGen.this.classBootstraps;
			protected final VerifiedClassGen clazz = VerifiedClassGen.this;
			protected final TakamakaClassLoader classLoader = clazz.classLoader;
			protected final boolean duringInitialization = Verifier.this.duringInitialization;
			protected final String className = getClassName();
			protected final ConstantPoolGen cpg = getConstantPool();
			protected final Set<Method> lambdasUnreachableFromStaticMethods = Verifier.this.lambdasUnreachableFromStaticMethods;

			/**
			 * Yields the target method or constructor called by the given bootstrap. It can also be outside
			 * the class that we are processing.
			 * 
			 * @param bootstrap the bootstrap
			 * @return the target called method or constructor
			 */
			protected final Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) {
				return VerifiedClassGen.this.getTargetOf(bootstrap);
			}

			protected final void issue(Issue issue) {
				issueHandler.accept(issue);
				hasErrors |= issue instanceof Error;
			}

			protected final Optional<? extends Executable> whiteListingModelOf(Executable executable, InvokeInstruction invoke) {
				return VerifiedClassGen.this.whiteListingModelOf(executable, invoke);
			}

			protected final Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction ins) {
				return VerifiedClassGen.this.resolvedExecutableFor(ins);
			}

			protected final Optional<Field> resolvedFieldFor(FieldInstruction ins)  {
				return VerifiedClassGen.this.resolvedFieldFor(ins);
			}
		}

		private void computeLambdasUnreachableFromStaticMethods() {
			// we initially compute the set of all lambdas
			Set<Method> lambdas = classBootstraps.getBootstraps()
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
			MethodGen methodGen = new MethodGen(method, getClassName(), getConstantPool());
			InstructionList instructions = methodGen.getInstructionList();
			if (instructions != null) {
				StreamSupport.stream(instructions.spliterator(), false)
					.map(InstructionHandle::getInstruction)
					.filter(instruction -> instruction instanceof INVOKEDYNAMIC)
					.map(instruction -> (INVOKEDYNAMIC) instruction)
					.map(classBootstraps::getBootstrapFor)
					.map(VerifiedClassGen.this::getLambdaFor)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.forEach(lambdasReachableFromStaticMethods::add);
			}
		}

		public class MethodVerifier {
			private final Method method;
			private final InstructionList instructions;
			private final LineNumberTable lines;

			public abstract class Check extends Verifier.Check {
				protected final Method method = MethodVerifier.this.method;

				/**
				 * Yields the instructions of the method.
				 * 
				 * @return the instructions
				 */
				protected final Stream<InstructionHandle> instructions() {
					return instructions == null ? Stream.empty() : StreamSupport.stream(instructions.spliterator(), false);
				}

				/**
				 * Yields the source line number from which the given instruction was compiled.
				 * 
				 * @param ih the instruction
				 * @return the line number, or -1 if not available
				 */
				protected final int lineOf(InstructionHandle ih) {
					return lineOf(ih.getPosition());
				}

				/**
				 * Yields the source line number for the instruction at the given program point.
				 * 
				 * @param pc the program point
				 * @return the line number, or -1 if not available
				 */
				protected final int lineOf(int pc) {
					return lines != null ? lines.getSourceLine(pc) : -1;
				}
			}

			private MethodVerifier(Method method) {
				this.method = method;
				MethodGen methodGen = new MethodGen(method, getClassName(), getConstantPool());
				this.instructions = methodGen.getInstructionList();
				this.lines = methodGen.getLineNumberTable(getConstantPool());

				new PayableCodeReceivesAmountCheck(this);
				new ThrowsExceptionsCodeIsPublicCheck(this);
				new PayableCodeIsEntryCheck(this);
				new EntryCodeIsInstanceAndInContractsCheck(this);
				new EntryCodeIsConsistentWithClassHierarchyCheck(this);
				new PayableCodeIsConsistentWithClassHierarchyCheck(this);
				new ThrowsExceptionsIsConsistentWithClassHierarchyCheck(this);
				new IsNotStaticInitializerCheck(this);
				new IsNotNativeCheck(this);
				new BytecodesAreLegalCheck(this);
				new IsNotSynchronizedCheck(this);
				new CallerIsUsedOnThisAndInEntryCheck(this);
				new EntriesAreOnlyCalledFromContractsCheck(this);
				new ExceptionHandlersAreForCheckedExceptionsCheck(this);
				new UsedCodeIsWhiteListedCheck(this);
			}
		}
	}
}