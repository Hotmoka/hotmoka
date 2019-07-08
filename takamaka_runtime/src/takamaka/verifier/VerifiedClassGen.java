package takamaka.verifier;

import java.util.function.Consumer;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.translator.TakamakaClassLoader;

/**
 * A BCEL class generator, specialized in order to verify some constraints required by Takamaka.
 */
public class VerifiedClassGen extends ClassGen implements Comparable<VerifiedClassGen> {

	/**
	 * The class loader used to load this class and the other classes of the program
	 * it belongs to.
	 */
	private final TakamakaClassLoader classLoader;

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

		this.classLoader = classLoader;

		verify(issueHandler);
	}

	@Override
	public int compareTo(VerifiedClassGen other) {
		return getClassName().compareTo(other.getClassName());
	}

	private void verify(Consumer<Issue> issueHandler) throws VerificationException{
		class IssueHandlerProxy implements Consumer<Issue> {
			private boolean hasErrors;

			@Override
			public void accept(Issue issue) {
				issueHandler.accept(issue);
				hasErrors |= issue instanceof Error;
			}
		};

		IssueHandlerProxy proxy = new IssueHandlerProxy();

		payableIsOnlyAppliedToEntries(proxy);

		if (proxy.hasErrors)
			throw new VerificationException();
	}

	private void payableIsOnlyAppliedToEntries(Consumer<Issue> issueHandler) {
		String className = getClassName();
		for (Method method: getMethods())
			if (classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType())
				&& classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType()) == null)
				issueHandler.accept(new PayableWithoutEntryError(this, method));
	}
}