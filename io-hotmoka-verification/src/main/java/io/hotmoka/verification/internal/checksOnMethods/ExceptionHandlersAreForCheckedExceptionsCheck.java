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

import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;

import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.errors.UncheckedExceptionHandlerError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that the exception handlers of a method are only for checked exceptions.
 */
public class ExceptionHandlersAreForCheckedExceptionsCheck extends CheckOnMethods {

	public ExceptionHandlersAreForCheckedExceptionsCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws UnknownTypeException {
		super(builder, method);

		for (CodeExceptionGen exc: method.getExceptionHandlers()) {
			ObjectType catchType = exc.getCatchType();
			String exceptionName = catchType == null ? "java.lang.Throwable" : catchType.getClassName();

			if (canCatchUncheckedExceptions(exceptionName))
				issue(new UncheckedExceptionHandlerError(inferSourceFile(), method, lineOf(exc.getHandlerPC()), exceptionName));
		}
	}

	private boolean canCatchUncheckedExceptions(String exceptionName) throws UnknownTypeException {
		try {
			Class<?> clazz = classLoader.loadClass(exceptionName);
			return RuntimeException.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(RuntimeException.class) ||
					java.lang.Error.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(java.lang.Error.class);
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(exceptionName);
		}
	}
}