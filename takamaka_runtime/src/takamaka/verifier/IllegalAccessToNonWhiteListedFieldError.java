package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalAccessToNonWhiteListedFieldError extends Error {

	public IllegalAccessToNonWhiteListedFieldError(ClassGen clazz, Method where, int line, String definingClassName, String fieldName) {
		super(clazz, where, line, "illegal access to non-white-listed field " + definingClassName + "." + fieldName);
	}
}