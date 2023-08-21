/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.exceptions;

import java.util.function.Function;

/**
 * This class provides a method to transform a function with exceptions
 * into a function, by unchecking its exceptions.
 */
public abstract class UncheckFunction {

	private UncheckFunction() {}

	/**
	 * Transforms a function with exceptions into a function without checked
	 * exceptions. This means that all checked exceptions get wrapped into
	 * a {@link UncheckedException}. They can later be recovered through
	 * a method from {@link CheckRunnable} or {@link CheckSupplier}.
	 * 
	 * @param <T> the type of the parameter of the function
	 * @param <R> the type of the result of the function
	 * @param wrapped the function with exceptions
	 * @return the function without exceptions
	 */
	public static <T, R> Function<T, R> uncheck(FunctionWithExceptions<T, R> wrapped) {
		return new Function<>() {

			@Override
			public R apply(T t) {
				try {
					return wrapped.apply(t);
				}
				catch (RuntimeException | Error e) {
					throw e;
				}
				catch (Throwable e) {
					if (InterruptedException.class.isInstance(e))
						Thread.currentThread().interrupt();

					throw new UncheckedException(e);
				}
			}
		};
	}
}