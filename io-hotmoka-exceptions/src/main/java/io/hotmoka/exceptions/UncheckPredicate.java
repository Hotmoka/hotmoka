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

import java.util.function.Predicate;

/**
 * This class provides a method to transform a predicate with exceptions
 * into a predicate, by unchecking its exceptions.
 */
public abstract class UncheckPredicate {

	private UncheckPredicate() {}

	/**
	 * Transforms a predicate with exceptions into a predicate without checked
	 * exceptions. This means that all checked exceptions get wrapped into
	 * a {@link UncheckedException}. They can later be recovered through
	 * a method from {@link CheckRunnable} or {@link CheckSupplier}.
	 * 
	 * @param <T> the type of the tested value
	 * @param wrapped the predicate with exceptions
	 * @return the predicate without exceptions
	 */
	public static <T> Predicate<T> uncheck(PredicateWithExceptions<T> wrapped) {
		return new Predicate<>() {
	
			@Override
			public boolean test(T t) {
				try {
					return wrapped.test(t);
				}
				catch (RuntimeException | Error e) {
					throw e;
				}
				catch (Throwable e) {
					throw new UncheckedException(e);
				}
			}
		};
	}
}