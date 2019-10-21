package takamaka.verifier.internal;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

import takamaka.translator.IncompleteClasspathError;
import takamaka.translator.TakamakaClassLoader;
import takamaka.verifier.ClassBootstraps;
import takamaka.verifier.Issue;
import takamaka.verifier.VerificationException;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.internal.checks.onClass.BootstrapsAreLegalCheck;
import takamaka.verifier.internal.checks.onClass.EntriesAreOnlyCalledFromContractsCheck;
import takamaka.verifier.internal.checks.onClass.PackagesAreLegalCheck;
import takamaka.verifier.internal.checks.onClass.StorageClassesHaveFieldsOfStorageTypeCheck;
import takamaka.verifier.internal.checks.onMethods.BytecodesAreLegalCheck;
import takamaka.verifier.internal.checks.onMethods.CallerIsUsedOnThisAndInEntryCheck;
import takamaka.verifier.internal.checks.onMethods.EntryCodeIsConsistentWithClassHierarchyCheck;
import takamaka.verifier.internal.checks.onMethods.EntryCodeIsInstanceAndInContractsCheck;
import takamaka.verifier.internal.checks.onMethods.ExceptionHandlersAreForCheckedExceptionsCheck;
import takamaka.verifier.internal.checks.onMethods.IsNotNativeCheck;
import takamaka.verifier.internal.checks.onMethods.IsNotStaticInitializerCheck;
import takamaka.verifier.internal.checks.onMethods.IsNotSynchronizedCheck;
import takamaka.verifier.internal.checks.onMethods.PayableCodeIsConsistentWithClassHierarchyCheck;
import takamaka.verifier.internal.checks.onMethods.PayableCodeIsEntryCheck;
import takamaka.verifier.internal.checks.onMethods.PayableCodeReceivesAmountCheck;
import takamaka.verifier.internal.checks.onMethods.ThrowsExceptionsCodeIsPublicCheck;
import takamaka.verifier.internal.checks.onMethods.ThrowsExceptionsIsConsistentWithClassHierarchyCheck;
import takamaka.verifier.internal.checks.onMethods.UsedCodeIsWhiteListedCheck;
import takamaka.whitelisted.MustRedefineHashCode;
import takamaka.whitelisted.MustRedefineHashCodeOrToString;

/**
 * A BCEL class, that passed the static Takamaka verification tests.
 */
public class VerifiedClassGenImpl extends ClassGen implements VerifiedClassGen {

	/**
	 * The class loader used to load this class and the other classes of the program it belongs to.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * The object that provides utilities about the lambda bootstraps contained in this class.
	 */
	private final ClassBootstrapsImpl classBootstraps;

	/**
	 * The utility that can be used to resolve targets of calls and field accesses in this class.
	 */
	private final Resolver resolver;

	/**
	 * A methods of this class, in editable version.
	 */
	private final Set<MethodGen> methods;

	/**
	 * Builds and verify a BCEL class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param classLoader the Takamaka class loader for the context of the class
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is built during blockchain initialization
	 * @throws VefificationException if the class could not be verified
	 */
	public VerifiedClassGenImpl(JavaClass clazz, TakamakaClassLoader classLoader, Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
		super(clazz);

		this.methods = Stream.of(getMethods()).map(method -> new MethodGen(method, getClassName(), getConstantPool())).collect(Collectors.toSet());
		this.classLoader = classLoader;
		this.classBootstraps = new ClassBootstrapsImpl(this);
		this.resolver = new Resolver(this);

		new Verification(issueHandler, duringInitialization);
	}

	@Override
	public TakamakaClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public ClassBootstraps getClassBootstraps() {
		return classBootstraps;
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	@Override
	public Field whiteListingModelOf(FieldInstruction fi) {
		return classLoader.whiteListingWizard.whiteListingModelOf(resolver.resolvedFieldFor(fi).get()).get();
	}

	@Override
	public Executable whiteListingModelOf(InvokeInstruction invoke) {
		return whiteListingModelOf(resolver.resolvedExecutableFor(invoke).get(), invoke).get();
	}

	@Override
	public Stream<MethodGen> getMethodGens() {
		return methods.stream();
	}

	/**
	 * Yields the utility that can be used to resolve the targets of calls and field access instructions in this class.
	 * 
	 * @return the utility
	 */
	public Resolver getClassResolver() {
		return resolver;
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
		 * A map from each method to its line number table.
		 */
		private final Map<MethodGen, LineNumberTable> lines;

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
			this.lines = methods.stream().collect(Collectors.toMap(method -> method, method -> method.getLineNumberTable(getConstantPool())));
			this.duringInitialization = duringInitialization;

			new PackagesAreLegalCheck(this);
			new BootstrapsAreLegalCheck(this);
			new StorageClassesHaveFieldsOfStorageTypeCheck(this);
			new EntriesAreOnlyCalledFromContractsCheck(this);

			getMethodGens().forEach(MethodVerification::new);

			if (hasErrors)
				throw new VerificationException();
		}

		public abstract class Check {
			protected final VerifiedClassGenImpl clazz = VerifiedClassGenImpl.this;
			protected final TakamakaClassLoader classLoader = clazz.classLoader;
			protected final boolean duringInitialization = Verification.this.duringInitialization;
			protected final String className = getClassName();
			protected final ConstantPoolGen cpg = getConstantPool();
		
			protected final void issue(Issue issue) {
				issueHandler.accept(issue);
				hasErrors |= issue instanceof takamaka.verifier.Error;
			}

			protected final boolean hasWhiteListingModel(FieldInstruction fi) {
				Optional<Field> field = resolver.resolvedFieldFor(fi);
				return field.isPresent() && classLoader.whiteListingWizard.whiteListingModelOf(field.get()).isPresent();
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
		}

		public class MethodVerification {
			private final MethodGen method;

			private MethodVerification(MethodGen method) {
				this.method = method;
			
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
				new ExceptionHandlersAreForCheckedExceptionsCheck(this);
				new UsedCodeIsWhiteListedCheck(this);
			}

			public abstract class Check extends Verification.Check {
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