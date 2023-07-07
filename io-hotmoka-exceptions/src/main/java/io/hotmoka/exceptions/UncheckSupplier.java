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

import java.util.function.Supplier;

/**
 * This class provides a method to transform a supplier with exceptions
 * into a supplier, by unchecking its exceptions.
 */
public abstract class UncheckSupplier {

	private UncheckSupplier() {}

	/**
	 * Transforms a supplier with exceptions into a supplier without checked
	 * exceptions. This means that all checked exceptions get wrapped into
	 * a {@link UncheckedException}. They can later be recovered through
	 * a method from {@link CheckRunnable} or {@link CheckSupplier}.
	 * 
	 * @param <T> the type of the supplied value
	 * @param wrapped the supplier with exceptions
	 * @return the supplier without exceptions
	 */
	public static <T> Supplier<T> uncheck(SupplierWithExceptions<T> wrapped) {
		return new Supplier<>() {

			@Override
			public T get() {
				try {
					return wrapped.get();
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