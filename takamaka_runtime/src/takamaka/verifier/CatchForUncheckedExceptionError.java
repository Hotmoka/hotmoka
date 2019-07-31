package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class CatchForUncheckedExceptionError extends Error {

	public CatchForUncheckedExceptionError(ClassGen clazz, Method where, int line, String exceptionName) {
		super(clazz, where, line, "exception handler for unchecked exception " + exceptionName);
	}
}