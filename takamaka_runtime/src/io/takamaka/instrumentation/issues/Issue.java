package io.takamaka.instrumentation.issues;

import static java.util.Comparator.comparing;

import java.util.Comparator;

import org.apache.bcel.generic.ClassGen;

/**
 * An issue generated during the verification of the class files of a Takamaka program.
 * Issues are first ordered by where they occur, then by message and finally by issue class name.
 */
public abstract class Issue implements Comparable<Issue> {
	public final String where;
	public final String message;
	private final static Comparator<Issue> comparator =
		comparing((Issue issue) -> issue.where)
		.thenComparing(issue -> issue.getClass().getName())
		.thenComparing(issue -> issue.message);

	/**
	 * Creates an issue at the given class.
	 * 
	 * @param clazz the class where the issue occurs
	 * @param message the message of the issue
	 */
	protected Issue(ClassGen clazz, String message) {
		this.where = inferSourceFile(clazz);
		this.message = message;
	}

	/**
	 * Creates an issue at the given program field.
	 * 
	 * @param clazz the class where the issue occurs
	 * @param fieldName the name of the field where the issue occurs
	 * @param message the message of the issue
	 */
	protected Issue(ClassGen clazz, String fieldName, String message) {
		this.where = inferSourceFile(clazz) + " field " + fieldName;
		this.message = message;
	}

	/**
	 * Creates an issue at the given program line.
	 * 
	 * @param clazz the class where the issue occurs
	 * @param methodName the name of the method where the issue occurs
	 * @param line the line where the issue occurs. Use -1 if the issue is related to the method as a whole
	 * @param message the message of the issue
	 */
	protected Issue(ClassGen clazz, String methodName, int line, String message) {
		this.where = inferSourceFile(clazz) + (line >= 0 ? (":" + line) : (" method " + methodName));
		this.message = message;
	}

	private static String inferSourceFile(ClassGen clazz) {
		String sourceFile = clazz.getFileName();
		String className = clazz.getClassName();
	
		if (sourceFile != null) {
			int lastDot = className.lastIndexOf('.');
			if (lastDot > 0)
				return className.substring(0, lastDot).replace('.', '/') + '/' + sourceFile;
			else
				return sourceFile;
		}
	
		return className;
	}

	@Override
	public final int compareTo(Issue other) {
		return comparator.compare(this, other);
	}

	@Override
	public final boolean equals(Object other) {
		return other instanceof Issue && getClass() == other.getClass() && where.equals(((Issue) other).where) && message.equals(((Issue) other).message);
	}

	@Override
	public final int hashCode() {
		return where.hashCode() ^ message.hashCode();
	}

	@Override
	public final String toString() {
		return where + ": " + message;
	}
}