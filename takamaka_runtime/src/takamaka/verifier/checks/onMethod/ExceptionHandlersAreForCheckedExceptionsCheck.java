package takamaka.verifier.checks.onMethod;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ConstantClass;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.UncheckedExceptionHandlerError;

/**
 * A check that the exception handlers of a method are only for checked exceptions.
 */
public class ExceptionHandlersAreForCheckedExceptionsCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public ExceptionHandlersAreForCheckedExceptionsCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		Code code = method.getCode();
		if (code != null) {
			CodeException[] excs = code.getExceptionTable();
			if (excs != null)
				for (CodeException exc: excs) {
					int classIndex = exc.getCatchType();
					String exceptionName = classIndex == 0 ?
						"java.lang.Throwable" :
						((ConstantClass) cpg.getConstant(classIndex)).getBytes(cpg.getConstantPool()).replace('/', '.');

					// enum's are sometimes compiled with synthetic methods that catch NoSuchFieldError
					if (((clazz.isEnum() && method.isSynthetic())
							|| (method.getName().equals(Const.STATIC_INITIALIZER_NAME) && clazz.isSynthetic()))
						&& exceptionName.equals("java.lang.NoSuchFieldError"))
						continue;

					if (canCatchUncheckedExceptions(exceptionName))
						issue(new UncheckedExceptionHandlerError(clazz, method.getName(), lineOf(exc.getHandlerPC()), exceptionName));
				}
		}
	}

	private boolean canCatchUncheckedExceptions(String exceptionName) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Class<?> clazz = classLoader.loadClass(exceptionName);
			return RuntimeException.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(RuntimeException.class) ||
				java.lang.Error.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(java.lang.Error.class);
		});
	}
}