package io.takamaka.code.verification.internal;

import io.takamaka.code.verification.IncompleteClasspathError;

/**
 * Utilities for throwing an {@link io.takamaka.code.verification.IncompleteClasspathError}
 * instead of a {@link java.lang.ClassNotFoundException}.
 */
public class ThrowIncompleteClasspathError {

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