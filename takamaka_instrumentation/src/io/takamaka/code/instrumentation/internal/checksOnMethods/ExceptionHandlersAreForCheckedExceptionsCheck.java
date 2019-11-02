package io.takamaka.code.instrumentation.internal.checksOnMethods;

import org.apache.bcel.Const;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ObjectType;

import io.takamaka.code.instrumentation.internal.ThrowIncompleteClasspathError;
import io.takamaka.code.instrumentation.internal.VerifiedClass;
import io.takamaka.code.instrumentation.issues.UncheckedExceptionHandlerError;

/**
 * A check that the exception handlers of a method are only for checked exceptions.
 */
public class ExceptionHandlersAreForCheckedExceptionsCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public ExceptionHandlersAreForCheckedExceptionsCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		for (CodeExceptionGen exc: method.getExceptionHandlers()) {
			ObjectType catchType = exc.getCatchType();
			String exceptionName = catchType == null ? "java.lang.Throwable" : catchType.getClassName();

			if (!specialCatchInsideEnumInitializer(exceptionName) && canCatchUncheckedExceptions(exceptionName))
				issue(new UncheckedExceptionHandlerError(inferSourceFile(), methodName, lineOf(exc.getHandlerPC()), exceptionName));
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
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Class<?> clazz = classLoader.loadClass(exceptionName);
			return RuntimeException.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(RuntimeException.class) ||
				java.lang.Error.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(java.lang.Error.class);
		});
	}
}