/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.verification.internal.checksOnMethods;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.UncheckedExceptionHandlerError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that the exception handlers of a method are only for checked exceptions.
 */
public class ExceptionHandlersAreForCheckedExceptionsCheck extends CheckOnMethods {

	public ExceptionHandlersAreForCheckedExceptionsCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws IllegalJarException {
		super(builder, method);

		for (CodeExceptionGen exc: method.getExceptionHandlers()) {
			ObjectType catchType = exc.getCatchType();
			String exceptionName = catchType == null ? "java.lang.Throwable" : catchType.getClassName();

			if (canCatchUncheckedExceptions(exceptionName) && !specialCatchInsideEnumInitializer(exceptionName) && !specialCatchInsideSwitchOnEnum(exceptionName))
				issue(new UncheckedExceptionHandlerError(inferSourceFile(), methodName, lineOf(exc.getHandlerPC()), exceptionName));
		}
	}

	/**
	 * enum's are sometimes compiled with synthetic methods that catch NoSuchFieldError.
	 * These handlers must be allowed in Takamaka code. This is safe since no Takamaka exception
	 * (such as out of gas exception) is caught by a NoSuchFieldError.
	 * 
	 * @param exceptionName the name of the caught exception
	 * @return true if the exception is NoSuchFieldError thrown inside the
	 *         static initializer of an enumeration
	 */
	private boolean specialCatchInsideEnumInitializer(String exceptionName) {
		return ((isEnum() && method.isSynthetic())
			|| (Const.STATIC_INITIALIZER_NAME.equals(methodName) && isSynthetic()))
			&& NoSuchFieldError.class.getName().equals(exceptionName);
	}

	/**
	 * A switch on an enum is sometimes compiled with a method that catch NoSuchFieldError.
	 * These handlers must be allowed in Takamaka code. The method is not even marked as synthetic
	 * by most compilers. This is safe since no Takamaka exception
	 * (such as out of gas exception) is caught by a NoSuchFieldError.
	 * 
	 * @param exceptionName the name of the caught exception
	 * @return true if the exception is NoSuchFieldError thrown inside the
	 *         method for a switch on an enumeration
	 */
	private boolean specialCatchInsideSwitchOnEnum(String exceptionName) {
		return !method.isPublic() && NoSuchFieldError.class.getName().equals(exceptionName)
			&& "$SWITCH_TABLE$".equals(methodName)
			&& methodReturnType instanceof ArrayType at && at.getBasicType() == Type.INT;
	}

	private boolean canCatchUncheckedExceptions(String exceptionName) throws IllegalJarException {
		try {
			Class<?> clazz = classLoader.loadClass(exceptionName);
			return RuntimeException.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(RuntimeException.class) ||
					java.lang.Error.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(java.lang.Error.class);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}
	}
}