package takamaka.verifier;

import java.lang.reflect.Constructor;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalCallToNonWhiteListedConstructorError extends Error {

	public IllegalCallToNonWhiteListedConstructorError(ClassGen clazz, Method where, int line, Constructor<?> constructor) {
		super(clazz, where, line, "illegal call to non-white-listed constructor of " + constructor.getDeclaringClass().getName());
	}
}