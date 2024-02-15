/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.closeables;

import java.util.function.Supplier;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.internal.AutoCloseableWithLockImpl;

/**
 * Partial implementation of an autocloseable with support for blocking calls to its methods after
 * it has been closed. Any attempt to call a method after closure will throw an exception.
 * 
 * @param <E> the type of exception thrown if {@link #mkScope()} is called after {@link #stopNewCalls()}
 */
@ThreadSafe
public abstract class AbstractAutoCloseableWithLock<E extends Exception> extends AutoCloseableWithLockImpl<E> {

	/**
	 * Creates the autocloseable.
	 * 
	 * @param exceptionSupplier the supplier of the exception that must be thrown when attempting
	 *                          to create a call scope after closure
	 */
	protected AbstractAutoCloseableWithLock(Supplier<E> exceptionSupplier) {
		super(exceptionSupplier);
	}
}