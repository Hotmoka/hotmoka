package io.takamaka.code.verification.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.Annotations;
import io.takamaka.code.verification.BcelToClass;
import io.takamaka.code.verification.Bootstraps;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.VerifiedJar;
import io.takamaka.code.verification.internal.checksOnClass.BootstrapsAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnClass.EntriesAreOnlyCalledFromContractsCheck;
import io.takamaka.code.verification.internal.checksOnClass.NamesDontStartWithForbiddenPrefix;
import io.takamaka.code.verification.internal.checksOnClass.PackagesAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnClass.RedPayableIsOnlyCalledFromRedGreenContractsCheck;
import io.takamaka.code.verification.internal.checksOnClass.StorageClassesHaveFieldsOfStorageTypeCheck;
import io.takamaka.code.verification.internal.checksOnMethods.BytecodesAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnMethods.CallerIsUsedOnThisAndInEntryCheck;
import io.takamaka.code.verification.internal.checksOnMethods.EntryCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.EntryCodeIsInstanceAndInContractsCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ExceptionHandlersAreForCheckedExceptionsCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotFinalizerCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotNativeCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotStaticInitializerCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotSynchronizedCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsEntryCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsNotRedPayableCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeReceivesAmountCheck;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeIsEntryCheck;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeIsInRedGreenContract;
import io.takamaka.code.verification.internal.checksOnMethods.RedPayableCodeReceivesAmountCheck;
import io.takamaka.code.verification.internal.checksOnMethods.SelfChargedCodeIsInstancePublicMethodOfContractCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ThrowsExceptionsCodeIsPublicCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ThrowsExceptionsIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.UsedCodeIsWhiteListedCheck;
import io.takamaka.code.verification.issues.Issue;
import io.takamaka.code.whitelisting.WhiteListingProofObligation;

/**
 * A class that passed the static Takamaka verification tests.
 */
public class VerifiedClassImpl implements VerifiedClass {

	/**
	 * The class generator used to generate this object.
	 */
	private final ClassGen clazz;

	/**
	 * The jar this class belongs to.
	 */
	public final VerifiedJarImpl jar;

	/**
	 * The utility object that knows about the lambda bootstraps contained in this class.
	 */
	public final BootstrapsImpl bootstraps;

	/**
	 * The utility that can be used to resolve targets of calls and field accesses in this class.
	 */
	public final ResolverImpl resolver;

	/**
	 * Builds and verifies a class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param jar the jar this class belongs to
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is built during blockchain initialization
	 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
	 * @throws VefificationException if the class could not be verified
	 */
	VerifiedClassImpl(JavaClass clazz, VerifiedJarImpl jar, Consumer<Issue> issueHandler, boolean duringInitialization, boolean allowSelfCharged) throws VerificationException {
		this.clazz = new ClassGen(clazz);
		this.jar = jar;
		ConstantPoolGen cpg = getConstantPool();
		MethodGen[] methods = Stream.of(clazz.getMethods()).map(method -> new MethodGen(method, getClassName(), cpg)).toArray(MethodGen[]::new);
		this.bootstraps = new BootstrapsImpl(this, methods);
		this.resolver = new ResolverImpl(this);

		new Builder(issueHandler, methods, duringInitialization, allowSelfCharged);
	}

	@Override
	public String getClassName() {
		return clazz.getClassName();
	}

	@Override
	public int compareTo(VerifiedClass other) {
		return getClassName().compareTo(other.getClassName());
	}

	@Override
	public Field whiteListingModelOf(FieldInstruction fi) {
		return jar.classLoader.getWhiteListingWizard().whiteListingModelOf(resolver.resolvedFieldFor(fi).get()).get();
	}

	@Override
	public Executable whiteListingModelOf(InvokeInstruction invoke) {
		return whiteListingModelOf(resolver.resolvedExecutableFor(invoke).get(), invoke).get();
	}

	@Override
	public VerifiedJar getJar() {
		return jar;
	}

	@Override
	public Bootstraps getBootstraps() {
		return new BootstrapsImpl(bootstraps);
	}

	@Override
	public JavaClass toJavaClass() {
		return clazz.getJavaClass();
	}

	/**
	 * Yields the constant pool of this class.
	 * 
	 * @return the constant pool
	 */
	ConstantPoolGen getConstantPool() {
		return clazz.getConstantPool();
	}

	/**
	 * Yields the attributes of this class.
	 * 
	 * @return the attributes
	 */
	Attribute[] getAttributes() {
		return clazz.getAttributes();
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
		if (executable instanceof Constructor<?>)
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException
				(() -> checkINVOKESPECIAL(invoke, jar.classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable)));
		else
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException
				(() -> checkINVOKESPECIAL(invoke, jar.classLoader.getWhiteListingWizard().whiteListingModelOf((Method) executable)));
	}

	/**
	 * If the given invoke instruction is an {@code invokespecial} and the given model
	 * has a white-listing annotation on its receiver, checks if the model
	 * of the resolved target of the invoke is still in the code installed in blockchain.
	 * This check is important in order to forbid calls such as super.hashCode() to the hashCode()
	 * method of Object, that would be non-deterministic.
	 * 
	 * @param invoke the invoke instruction
	 * @param model the white-listing model of the invoke
	 * @return the optional containing the model, or the empty optional if the check fails
	 */
	private Optional<? extends Executable> checkINVOKESPECIAL(InvokeInstruction invoke, Optional<? extends Executable> model) {
		if (invoke instanceof INVOKESPECIAL &&
			model.isPresent() &&
			hasWhiteListingProofObligationOnReceiver(model.get()) &&
			resolver.resolvedExecutableFor(invoke).get().getDeclaringClass() == Object.class)
			return Optional.empty();
		else
			return model;
	}

	/**
	 * Determines if the given method or constructor has a white-listing proof obligation
	 * on its receiver.
	 * 
	 * @param executable the method or constructor
	 * @return true if and only if that condition holds
	 */
	private static boolean hasWhiteListingProofObligationOnReceiver(Executable executable) {
		return Stream.of(executable.getAnnotations())
			.anyMatch(annotation -> annotation.annotationType().getAnnotation(WhiteListingProofObligation.class) != null);
	}

	/**
	 * The algorithms that perform the verification of the BCEL class.
	 */
	public class Builder {

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		private final Consumer<Issue> issueHandler;

		/**
		 * A map from each method to its line number table.
		 */
		private final Map<MethodGen, LineNumberTable> lines;

		/**
		 * True if and only if the code verification occurs during blockchain initialization.
		 */
		private final boolean duringInitialization;

		/**
		 * True if and only if {@code @SelfCharged} methods are allowed.
		 */
		private final boolean allowSelfCharged;

		/**
		 * The methods of the class under verification.
		 */
		private final MethodGen[] methods;

		/**
		 * True if and only if at least an error was issued during verification.
		 */
		private boolean hasErrors;

		/**
		 * Performs the static verification of this class.
		 * 
		 * @param issueHandler the handler to call when an issue is found
		 * @param duringInitialization true if and only if verification is performed during blockchain initialization
		 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
		 * @throws VerificationException if some verification error occurs
		 */
		private Builder(Consumer<Issue> issueHandler, MethodGen[] methods, boolean duringInitialization, boolean allowSelfCharged) throws VerificationException {
			this.issueHandler = issueHandler;
			ConstantPoolGen cpg = getConstantPool();
			this.methods = methods;
			this.lines = Stream.of(methods).collect(Collectors.toMap(method -> method, method -> method.getLineNumberTable(cpg)));
			this.duringInitialization = duringInitialization;
			this.allowSelfCharged = allowSelfCharged;

			new PackagesAreLegalCheck(this);
			new NamesDontStartWithForbiddenPrefix(this);
			new BootstrapsAreLegalCheck(this);
			new StorageClassesHaveFieldsOfStorageTypeCheck(this);
			new EntriesAreOnlyCalledFromContractsCheck(this);
			new RedPayableIsOnlyCalledFromRedGreenContractsCheck(this);

			Stream.of(methods).forEachOrdered(MethodVerification::new);

			if (hasErrors)
				throw new VerificationException();
		}

		public abstract class Check {
			protected final TakamakaClassLoader classLoader = jar.classLoader;
			protected final BootstrapsImpl bootstraps = VerifiedClassImpl.this.bootstraps;
			protected final ResolverImpl resolver = VerifiedClassImpl.this.resolver;
			protected final Annotations annotations = jar.annotations;
			protected final BcelToClass bcelToClass = jar.bcelToClass;
			protected final boolean duringInitialization = Builder.this.duringInitialization;
			protected final boolean allowSelfCharged = Builder.this.allowSelfCharged;
			protected final String className = getClassName();
			protected final ConstantPoolGen cpg = getConstantPool();
		
			protected final void issue(Issue issue) {
				issueHandler.accept(issue);
				hasErrors |= issue instanceof io.takamaka.code.verification.issues.Error;
			}

			protected final boolean hasWhiteListingModel(FieldInstruction fi) {
				Optional<Field> field = resolver.resolvedFieldFor(fi);
				return field.isPresent() && classLoader.getWhiteListingWizard().whiteListingModelOf(field.get()).isPresent();
			}

			protected final boolean hasWhiteListingModel(InvokeInstruction invoke) {
				Optional<? extends Executable> executable = resolver.resolvedExecutableFor(invoke);
				return executable.isPresent() && whiteListingModelOf(executable.get(), invoke).isPresent();
			}

			protected final LineNumberTable getLinesFor(MethodGen method) {
				return lines.get(method);
			}

			protected final Stream<InstructionHandle> instructionsOf(MethodGen method) {
				InstructionList instructions = method.getInstructionList();
				return instructions == null ? Stream.empty() : StreamSupport.stream(instructions.spliterator(), false);
			}

			/**
			 * Infers the source file name of the class being checked.
			 * If there is no debug information, the class name is returned.
			 * 
			 * @return the inferred source file name
			 */
			protected final String inferSourceFile() {
				String sourceFile = clazz.getFileName();
				String className = getClassName();
			
				if (sourceFile != null) {
					int lastDot = className.lastIndexOf('.');
					if (lastDot > 0)
						return className.substring(0, lastDot).replace('.', '/') + '/' + sourceFile;
					else
						return sourceFile;
				}
			
				return className;
			}

			/**
			 * Yields the source line number from which the given instruction of the given method was compiled.
			 * 
			 * @param method the method
			 * @param pc the program point of the instruction
			 * @return the line number, or -1 if not available
			 */
			protected final int lineOf(MethodGen method, int pc) {
				LineNumberTable lines = getLinesFor(method);
				return lines != null ? lines.getSourceLine(pc) : -1;
			}

			/**
			 * Yields the source line number from which the given instruction of the given method was compiled.
			 * 
			 * @param method the method
			 * @param ih the instruction
			 * @return the line number, or -1 if not available
			 */
			protected final int lineOf(MethodGen method, InstructionHandle ih) {
				return lineOf(method, ih.getPosition());
			}

			/**
			 * Determines if this class is an enumeration.
			 * 
			 * @return true if and only if that condition holds
			 */
			protected final boolean isEnum() {
				return clazz.isEnum();
			}

			/**
			 * Determines if this class is synthetic.
			 * 
			 * @return true if and only if that condition holds
			 */
			protected final boolean isSynthetic() {
				return clazz.isSynthetic();
			}

			/**
			 * Yields the lambda bridge method called by the given bootstrap.
			 * It must belong to the same class that we are processing.
			 * 
			 * @param bootstrap the bootstrap
			 * @return the lambda bridge method
			 */
			protected final Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap) {
				ConstantPoolGen cpg = getConstantPool();
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
							if (className.equals(getClassName()))
								return getMethods()
									.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
									.findFirst();
						}
					}
				}
			
				return Optional.empty();
			}

			/**
			 * Yields the fields in this class.
			 * 
			 * @return the fields
			 */
			protected final Stream<org.apache.bcel.classfile.Field> getFields() {
				return Stream.of(clazz.getFields());
			}

			/**
			 * Yields the methods inside the class being verified.
			 * 
			 * @return the methods
			 */
			protected final Stream<MethodGen> getMethods() {
				return Stream.of(methods);
			}
		}

		public class MethodVerification {
			private final MethodGen method;

			private MethodVerification(MethodGen method) {
				this.method = method;
			
				new PayableCodeReceivesAmountCheck(this);
				new RedPayableCodeReceivesAmountCheck(this);
				new ThrowsExceptionsCodeIsPublicCheck(this);
				new PayableCodeIsEntryCheck(this);
				new RedPayableCodeIsEntryCheck(this);
				new EntryCodeIsInstanceAndInContractsCheck(this);
				new EntryCodeIsConsistentWithClassHierarchyCheck(this);
				new PayableCodeIsConsistentWithClassHierarchyCheck(this);
				new RedPayableCodeIsConsistentWithClassHierarchyCheck(this);
				new RedPayableCodeIsInRedGreenContract(this);
				new PayableCodeIsNotRedPayableCheck(this);
				new ThrowsExceptionsIsConsistentWithClassHierarchyCheck(this);
				new IsNotStaticInitializerCheck(this);
				new IsNotNativeCheck(this);
				new IsNotFinalizerCheck(this);
				new BytecodesAreLegalCheck(this);
				new IsNotSynchronizedCheck(this);
				new CallerIsUsedOnThisAndInEntryCheck(this);
				new ExceptionHandlersAreForCheckedExceptionsCheck(this);
				new UsedCodeIsWhiteListedCheck(this);
				new SelfChargedCodeIsInstancePublicMethodOfContractCheck(this);
			}

			public abstract class Check extends Builder.Check {
				protected final MethodGen method = MethodVerification.this.method;
				protected final String methodName = method.getName();
				protected final Type[] methodArgs = method.getArgumentTypes();
				protected final Type methodReturnType = method.getReturnType();

				/**
				 * Yields the instructions of the method under verification.
				 * 
				 * @return the instructions
				 */
				protected final Stream<InstructionHandle> instructions() {
					return instructionsOf(method);
				}

				/**
				 * Yields the source line number from which the given instruction of the method under verification was compiled.
				 * 
				 * @param ih the instruction
				 * @return the line number, or -1 if not available
				 */
				protected final int lineOf(InstructionHandle ih) {
					return lineOf(method, ih);
				}

				/**
				 * Yields the source line number for the instruction at the given program point of the method under verification.
				 * 
				 * @param pc the program point
				 * @return the line number, or -1 if not available
				 */
				protected final int lineOf(int pc) {
					return lineOf(method, pc);
				}
			}
		}
	}
}