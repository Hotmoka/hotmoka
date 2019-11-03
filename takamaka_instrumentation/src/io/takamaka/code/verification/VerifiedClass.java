package io.takamaka.code.verification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import io.takamaka.code.verification.internal.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.internal.checksOnClass.BootstrapsAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnClass.EntriesAreOnlyCalledFromContractsCheck;
import io.takamaka.code.verification.internal.checksOnClass.PackagesAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnClass.StorageClassesHaveFieldsOfStorageTypeCheck;
import io.takamaka.code.verification.internal.checksOnMethods.BytecodesAreLegalCheck;
import io.takamaka.code.verification.internal.checksOnMethods.CallerIsUsedOnThisAndInEntryCheck;
import io.takamaka.code.verification.internal.checksOnMethods.EntryCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.EntryCodeIsInstanceAndInContractsCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ExceptionHandlersAreForCheckedExceptionsCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotNativeCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotStaticInitializerCheck;
import io.takamaka.code.verification.internal.checksOnMethods.IsNotSynchronizedCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeIsEntryCheck;
import io.takamaka.code.verification.internal.checksOnMethods.PayableCodeReceivesAmountCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ThrowsExceptionsCodeIsPublicCheck;
import io.takamaka.code.verification.internal.checksOnMethods.ThrowsExceptionsIsConsistentWithClassHierarchyCheck;
import io.takamaka.code.verification.internal.checksOnMethods.UsedCodeIsWhiteListedCheck;
import io.takamaka.code.verification.issues.Issue;
import io.takamaka.code.whitelisting.MustRedefineHashCode;
import io.takamaka.code.whitelisting.MustRedefineHashCodeOrToString;

/**
 * A class that passed the static Takamaka verification tests.
 */
public class VerifiedClass extends ClassGen implements Comparable<VerifiedClass> {

	/**
	 * The class loader used to load this class and the other classes of the program it belongs to.
	 */
	public final TakamakaClassLoader classLoader;

	/**
	 * The utility that can be used to transform BCEL types into their corresponding
	 * Java class tag, by using the class loader of this class.
	 */
	public final BcelToClass bcelToClass;

	/**
	 * The utility about the lambda bootstraps contained in this class.
	 */
	public final Bootstraps bootstraps;

	/**
	 * The utility that can be used to resolve targets of calls and field accesses in this class.
	 */
	public final Resolver resolver;

	/**
	 * The utility that knows about the annotations in the methods of this class.
	 */
	public final Annotations annotations;

	/**
	 * A methods of this class, in editable version.
	 */
	private final Set<MethodGen> methods;

	/**
	 * Builds and verify a class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param classLoader the Takamaka class loader for the context of the class
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is built during blockchain initialization
	 * @throws VefificationException if the class could not be verified
	 */
	public VerifiedClass(JavaClass clazz, TakamakaClassLoader classLoader, Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
		super(clazz);

		this.methods = Stream.of(getMethods()).map(method -> new MethodGen(method, getClassName(), getConstantPool())).collect(Collectors.toSet());
		this.classLoader = classLoader;
		this.bcelToClass = new BcelToClass(this);
		this.annotations = new Annotations(this);
		this.bootstraps = new Bootstraps(this);
		this.resolver = new Resolver(this);

		new ClassVerification(issueHandler, duringInitialization);
	}

	@Override
	public int compareTo(VerifiedClass other) {
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
		return classLoader.getWhiteListingWizard().whiteListingModelOf(resolver.resolvedFieldFor(fi).get()).get();
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
	 * Yields the methods inside this class, in generator form.
	 * 
	 * @return the methods inside this class
	 */
	public Stream<MethodGen> getMethodGens() {
		return methods.stream();
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
				(() -> checkINVOKESPECIAL(invoke, classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable)));
		else
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException
				(() -> checkINVOKESPECIAL(invoke, classLoader.getWhiteListingWizard().whiteListingModelOf((Method) executable)));
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
	public class ClassVerification {

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
		private ClassVerification(Consumer<Issue> issueHandler, boolean duringInitialization) throws VerificationException {
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
			protected final VerifiedClass clazz = VerifiedClass.this;
			protected final TakamakaClassLoader classLoader = clazz.classLoader;
			protected final boolean duringInitialization = ClassVerification.this.duringInitialization;
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
				String sourceFile = getFileName();
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

			public abstract class Check extends ClassVerification.Check {
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