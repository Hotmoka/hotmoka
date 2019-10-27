package io.takamaka.code.instrumentation;

/**
 * An error thrown if the classpath specified for a transaction misses
 * some class of the Takamaka runtime or some dependency.
 */
public class IncompleteClasspathError extends Error {
	private static final long serialVersionUID = 6525998903613112872L;

	public IncompleteClasspathError(ClassNotFoundException e) {
		super(e);
	}
}