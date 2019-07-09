package takamaka.verifier;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.Type;

import takamaka.translator.IncompleteClasspathError;
import takamaka.translator.TakamakaClassLoader;

/**
 * A BCEL class generator, specialized in order to verify some constraints required by Takamaka.
 */
public class VerifiedClassGen extends ClassGen implements Comparable<VerifiedClassGen> {

	/**
	 * Builds and verify a BCEL class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param classLoader the Takamaka class loader for the context of the class
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @throws VefificationException if the class could not be verified
	 */
	public VerifiedClassGen(JavaClass clazz, TakamakaClassLoader classLoader, Consumer<Issue> issueHandler) throws VerificationException {
		super(clazz);

		new Verification(classLoader, issueHandler);
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	/**
	 * The algorithms that perform the verification of the BCEL class.
	 */
	private class Verification {
		/**
		 * The name of the class under verification.
		 */
		private final String className;

		/**
		 * The class loader used to load the class under verification and the other classes of the program
		 * it belongs to.
		 */
		private final TakamakaClassLoader classLoader;

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		private final Consumer<Issue> issueHandler;

		/**
		 * True if and only if at least an error was issued during verification.
		 */
		private boolean hasErrors;

		private Verification(TakamakaClassLoader classLoader, Consumer<Issue> issueHandler) throws VerificationException {
			this.className = getClassName();
			this.classLoader = classLoader;
			this.issueHandler = issueHandler;

			entryIsLegal();
			entryIsConsistentAlongSubclasses();
			payableIsOnlyAppliedToEntries();
			payableIsConsistentAlongSubclasses();

			if (hasErrors)
				throw new VerificationException();
		}

		private void issue(Issue issue) {
			issueHandler.accept(issue);
			hasErrors |= issue instanceof Error;
		}

		/**
		 * Checks that {@code @@Entry} is applied only to instance public methods or constructors of contracts.
		 */
		private void entryIsLegal() {
			boolean isContract = classLoader.isContract(className);

			for (Method method: getMethods()) {
				Class<?> isEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
				if (isEntry != null) {
					if (!classLoader.contractClass.isAssignableFrom(isEntry))
						issue(new IllegalEntryArgumentError(VerifiedClassGen.this, method));
					if (!method.isPublic() || method.isStatic() || !isContract)
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
				if (!method.getName().equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
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
					.filter(m -> Modifier.isPublic(m.getModifiers())
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
		 * Checks that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
		 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
		 */
		private void payableIsConsistentAlongSubclasses() {
			for (Method method: getMethods())
				if (!method.getName().equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
					boolean wasPayable = classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType());

					try {
						isIdenticallyPayableInSupertypesOf(classLoader.loadClass(className), method, wasPayable);
					}
					catch (ClassNotFoundException e) {
						throw new IncompleteClasspathError(e);
					}
				}
		}

		private void isIdenticallyPayableInSupertypesOf(Class<?> clazz, Method method, boolean wasPayable) {
			String name = method.getName();
			Type returnType = method.getReturnType();
			Type[] args = method.getArgumentTypes();

			if (Stream.of(clazz.getDeclaredMethods())
					.filter(m -> Modifier.isPublic(m.getModifiers())
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
	}
}