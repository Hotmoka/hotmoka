package takamaka.translator;

/**
 * An error thrown if the classpath specified for a transaction misses
 * some class of the Takamaka runtime or some dependency.
 */
public class IncompleteClasspathError extends Error {
	private static final long serialVersionUID = 6525998903613112872L;

	public IncompleteClasspathError(ClassNotFoundException e) {
		super(e);
	}

	public interface Task {
		public void run() throws ClassNotFoundException;
	}

	public interface Computation<T> {
		public T run() throws ClassNotFoundException;
	}

	public static void insteadOfClassNotFoundException(Task task) {
		try {
			task.run();
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	public static <T> T insteadOfClassNotFoundException(Computation<T> computation) {
		try {
			return computation.run();
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}
}