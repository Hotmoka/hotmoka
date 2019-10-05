package takamaka.verifier;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

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
	 * The utility that can be used to resolve targets of calls and field accesses in this class.
	 */
	private final Resolver resolver;

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
		this.resolver = new Resolver(this);

		new Verification(issueHandler, duringInitialization);
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

	/**
	 * Yields the utility that can be used to resolve the targets of calls and field access instructions in this class.
	 * 
	 * @return the utility
	 */
	public Resolver getClassResolver() {
		return resolver;
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	/**
	 * Yields the white-listing model for the field accessed by the given instruction.
	 * This means that that instruction accesses that field but that access is white-listed
	 * only if the resulting model is verified.
	 * 
	 * @param fi the instruction that accesses the field
	 * @return the model. This must exist, since the class is verified and all accesses have been proved
	 *         to be white-listed (up to possible proof obligations contained in the model).
	 */
	public Field whiteListingModelOf(FieldInstruction fi) {
		return classLoader.whiteListingWizard.whiteListingModelOf(resolver.resolvedFieldFor(fi).get()).get();
	}

	/**
	 * Yields the white-listing model for the method called by the given instruction.
	 * This means that that instruction calls that method but that call is white-listed
	 * only if the resulting model is verified.
	 * 
	 * @param invoke the instruction that calls the method
	 * @return the model. This must exist, since the class is verified and all calls have been proved
	 *         to be white-listed (up to possible proof obligations contained in the model).
	 */
	public Executable whiteListingModelOf(InvokeInstruction invoke) {
		return whiteListingModelOf(resolver.resolvedExecutableFor(invoke).get(), invoke).get();
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
			resolver.resolvedExecutableFor(invoke).get().getDeclaringClass() == Object.class)
			return Optional.empty();
		else
			return model;
	}

	/**
	 * The algorithms that perform the verification of the BCEL class.
	 */
	public class Verification {

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		private final Consumer<Issue> issueHandler;

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
		private Verification(Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
			this.issueHandler = issueHandler;
			this.duringInitialization = duringInitialization;

			if (classLoader.isContract(getClassName()))
				computeLambdasUnreachableFromStaticMethods();

			new PackagesAreLegalCheck(this);
			new BootstrapsAreLegalCheck(this);
			new StorageClassesHaveFieldsOfStorageTypeCheck(this);

			Stream.of(getMethods())
				.forEach(MethodVerification::new);

			if (hasErrors)
				throw new VerificationException();
		}

		private void computeLambdasUnreachableFromStaticMethods() {
			Set<Method> lambdasReachableFromStaticMethods = new HashSet<>();

			// we initially compute the set of all lambdas
			Set<Method> lambdas = classBootstraps.getBootstraps()
				.map(classBootstraps::getLambdaFor)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());

			// then we consider all lambdas that might be called, directly, from a static method
			// that is not a lambda: they must be considered as reachable from a static method
			Stream.of(getMethods())
				.filter(Method::isStatic)
				.filter(method -> !lambdas.contains(method))
				.forEach(method -> addLambdasReachableFromStatic(method, lambdasReachableFromStaticMethods));

			// then we iterate on the same lambdas that have been found to be reachable from
			// the static methods and process them, recursively
			int initialSize;
			do {
				initialSize = lambdasReachableFromStaticMethods.size();
				new HashSet<>(lambdasReachableFromStaticMethods).stream()
					.forEach(method -> addLambdasReachableFromStatic(method, lambdasReachableFromStaticMethods));
			}
			while (lambdasReachableFromStaticMethods.size() > initialSize);

			lambdasUnreachableFromStaticMethods.addAll(lambdas);
			lambdasUnreachableFromStaticMethods.removeAll(lambdasReachableFromStaticMethods);
		}

		private void addLambdasReachableFromStatic(Method method, Set<Method> lambdasReachableFromStaticMethods) {
			MethodGen methodGen = new MethodGen(method, getClassName(), getConstantPool());
			InstructionList instructions = methodGen.getInstructionList();
			if (instructions != null)
				StreamSupport.stream(instructions.spliterator(), false)
					.map(InstructionHandle::getInstruction)
					.filter(instruction -> instruction instanceof INVOKEDYNAMIC)
					.map(instruction -> (INVOKEDYNAMIC) instruction)
					.map(classBootstraps::getBootstrapFor)
					.map(classBootstraps::getLambdaFor)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.forEach(lambdasReachableFromStaticMethods::add);
		}

		public abstract class Check {
			protected final VerifiedClassGen clazz = VerifiedClassGen.this;
			protected final TakamakaClassLoader classLoader = clazz.classLoader;
			protected final boolean duringInitialization = Verification.this.duringInitialization;
			protected final String className = getClassName();
			protected final ConstantPoolGen cpg = getConstantPool();
		
			protected final void issue(Issue issue) {
				issueHandler.accept(issue);
				hasErrors |= issue instanceof Error;
			}

			protected final boolean hasWhiteListingModel(FieldInstruction fi) {
				Optional<Field> field = resolver.resolvedFieldFor(fi);
				return field.isPresent() && classLoader.whiteListingWizard.whiteListingModelOf(field.get()).isPresent();
			}

			protected final boolean hasWhiteListingModel(InvokeInstruction invoke) {
				Optional<? extends Executable> executable = resolver.resolvedExecutableFor(invoke);
				return executable.isPresent() && whiteListingModelOf(executable.get(), invoke).isPresent();
			}

			protected final boolean mightBeReachedFromStaticMethods(Method method) {
				return !lambdasUnreachableFromStaticMethods.contains(method);
			}
		}

		public class MethodVerification {
			private final Method method;
			private final InstructionList instructions;
			private final LineNumberTable lines;

			private MethodVerification(Method method) {
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

			public abstract class Check extends Verification.Check {
				protected final Method method = MethodVerification.this.method;

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
		}
	}
}