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

package io.hotmoka.closeables.internal;

import java.util.function.Supplier;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.ClosureLocks;
import io.hotmoka.closeables.api.ClosureLock;

/**
 * Partial implementation of an autocloseable with support for blocking calls to its methods after
 * it has been closed. Any attempt to call a method after closure will throw an exception.
 * 
 * @param <E> the type of exception thrown if {@link #mkScope()} is called after {@link #stopNewCalls()}
 */
@ThreadSafe
public abstract class AutoCloseableWithLockImpl<E extends Exception> implements AutoCloseable {

	/**
	 * The lock used to block new calls after closure.
	 */
	private final ClosureLock closureLock = ClosureLocks.create();

	/**
	 * The supplier of the exception that must be thrown when attempting
	 * to create a call scope after closure.
	 */
	private final Supplier<E> exceptionSupplier;

	/**
	 * Creates the autocloseable.
	 * 
	 * @param exceptionSupplier the supplier of the exception that must be thrown when attempting
	 *                          to create a call scope after closure
	 */
	protected AutoCloseableWithLockImpl(Supplier<E> exceptionSupplier) {
		this.exceptionSupplier = exceptionSupplier;
	}

	/**
	 * Stops future new calls and waits for all unfinished calls to complete.
	 * After this call, invocations of {@link #mkScope()} fail with an exception.
	 * 
	 * @return true if and only if it actually stopped new calls, false if
	 *         this situation already held, because it was already requested
	 * @throws InterruptedException if the execution gets interrupted while
	 *                              waiting for unfinished calls to complete
	 */
	protected boolean stopNewCalls() throws InterruptedException {
		return closureLock.stopNewCalls();
	}

	/**
	 * Yields a call scope during which this autocloseable cannot be closed. This is typically
	 * called at the beginning of each new method call to the autocloseable
	 * and the resulting scope is closed at the end of that call.
	 * It guarantees to succeed only if {@link #stopNewCalls()} has not been called yet.
	 * Otherwise, the supplier provided to the constructor is used to throw an exception.
	 * 
	 * @return the scope
	 * @throws E the exception thrown if {@code #stopNewCalls()} has already been called
	 */
	protected ClosureLock.Scope mkScope() throws E {
		return closureLock.scope(exceptionSupplier);
	}
}