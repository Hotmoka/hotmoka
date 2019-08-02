package takamaka.verifier;

import java.lang.reflect.Field;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalAccessToNonWhiteListedFieldError extends Error {

	public IllegalAccessToNonWhiteListedFieldError(ClassGen clazz, Method where, int line, Field field) {
		super(clazz, where, line, "illegal access to non-white-listed field " + field.getDeclaringClass().getName() + "." + field.getName());
	}
}