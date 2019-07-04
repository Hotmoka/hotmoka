package takamaka.translator;

/**
 * An error thrown if the classpath specified for a transaction misses
 * some class of the Takamaka runtime or some dependency.
 */
public class IncompleteClasspathError extends Error {
	public IncompleteClasspathError(ClassNotFoundException e) {
		super(e);
	}
}