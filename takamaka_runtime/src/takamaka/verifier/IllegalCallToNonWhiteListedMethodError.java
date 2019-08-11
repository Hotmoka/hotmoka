package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalCallToNonWhiteListedMethodError extends Error {

	public IllegalCallToNonWhiteListedMethodError(ClassGen clazz, Method where, int line, java.lang.reflect.Method method) {
		super(clazz, where, line, "illegal call to non-white-listed method " + method.getDeclaringClass().getName() + "." + method.getName());
	}
}