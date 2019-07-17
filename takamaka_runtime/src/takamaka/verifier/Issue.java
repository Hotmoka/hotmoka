package takamaka.verifier;

import java.lang.reflect.Field;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

/**
 * An issue generated during the verification of the class files of a Takamaka program.
 * Issues are first ordered by where they occur, then by message and finally by issue class name.
 */
public abstract class Issue implements Comparable<Issue> {
	public final String where;
	public final String message;

	protected Issue(String where, String message) {
		this.where = where;
		this.message = message;
	}

	protected Issue(ClassGen where, String message) {
		this.where = inferSourceFile(where);
		this.message = message;
	}

	protected Issue(ClassGen clazz, Method where, String message) {
		this.where = inferSourceFile(clazz) + " method " + where.getName();
		this.message = message;
	}

	protected Issue(ClassGen clazz, Field where, String message) {
		this.where = inferSourceFile(clazz) + " field " + where.getName();
		this.message = message;
	}

	protected Issue(ClassGen clazz, Method where, int line, String message) {
		this.where = inferSourceFile(clazz) + (line >= 0 ? (":" + line) : (" method " + where.getName()));
		this.message = message;
	}

	private String inferSourceFile(ClassGen clazz) {
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
		int diff = where.compareTo(other.where);
		if (diff != 0)
			return diff;

		diff = message.compareTo(other.message);
		if (diff != 0)
			return diff;
		else
			return getClass().getName().compareTo(other.getClass().getName());
	}

	@Override
	public final boolean equals(Object other) {
		if (other instanceof Issue) {
			Issue otherAsIssue = (Issue) other;
			return where.equals(otherAsIssue.where) && message.equals(otherAsIssue.message) && getClass() == otherAsIssue.getClass();
		}
		else
			return false;
	}

	@Override
	public final int hashCode() {
		return where.hashCode() ^ message.hashCode();
	}

	@Override
	public String toString() {
		return where + ": " + message;
	}
}