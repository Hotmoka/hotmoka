package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.Const;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;

import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.UncheckedExceptionHandlerError;

/**
 * A check that the exception handlers of a method are only for checked exceptions.
 */
public class ExceptionHandlersAreForCheckedExceptionsCheck extends CheckOnMethods {

	public ExceptionHandlersAreForCheckedExceptionsCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

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
		return ((isEnum() && method.isSynthetic())
			|| (Const.STATIC_INITIALIZER_NAME.equals(methodName) && isSynthetic()))
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