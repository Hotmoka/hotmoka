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

	public static <T, R> Function<T, R> uncheck(FunctionWithExceptions<T, R> wrapped) {
		return new Function<>() {

			@Override
			public R apply(T t) {
				try {
					return wrapped.apply(t);
				}
				catch (Exception e) {
					throw new UncheckedException2(e);
				}
			}
		};
	}
}