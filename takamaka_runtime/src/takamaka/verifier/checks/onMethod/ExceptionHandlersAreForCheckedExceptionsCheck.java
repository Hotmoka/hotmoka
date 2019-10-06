package takamaka.verifier.checks.onMethod;

import org.apache.bcel.Const;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ObjectType;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.UncheckedExceptionHandlerError;

/**
 * A check that the exception handlers of a method are only for checked exceptions.
 */
public class ExceptionHandlersAreForCheckedExceptionsCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public ExceptionHandlersAreForCheckedExceptionsCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		for (CodeExceptionGen exc: method.getExceptionHandlers()) {
			ObjectType catchType = exc.getCatchType();
			String exceptionName = catchType == null ? "java.lang.Throwable" : catchType.getClassName();

			if (!specialCatchInsideEnumInitializer(exceptionName) && canCatchUncheckedExceptions(exceptionName))
				issue(new UncheckedExceptionHandlerError(clazz, methodName, lineOf(exc.getHandlerPC()), exceptionName));
		}
	}

	/**
	 * enum's are sometimes compiled with synthetic methods that catch NoSuchFieldError.
	 * These handlers must be allowed in Takamaka code.
	 * 
	 * @param exceptionName the name of the caught exception
	 * @return true if the exception is NoSuchFieldError thrown inside the
	 *         static initializer of an enumeration
	 */
	private boolean specialCatchInsideEnumInitializer(String exceptionName) {
		return ((clazz.isEnum() && method.isSynthetic())
			|| (Const.STATIC_INITIALIZER_NAME.equals(methodName) && clazz.isSynthetic()))
			&& exceptionName.equals("java.lang.NoSuchFieldError");
	}

	private boolean canCatchUncheckedExceptions(String exceptionName) {
		return IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Class<?> clazz = classLoader.loadClass(exceptionName);
			return RuntimeException.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(RuntimeException.class) ||
				java.lang.Error.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(java.lang.Error.class);
		});
	}
}