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

package io.hotmoka.closeables.api;

import java.util.function.Supplier;

import io.hotmoka.annotations.ThreadSafe;

/**
 * An object used in closeables whose methods must be disabled by throwing
 * an exception after they have been closed. Moreover, the close
 * operation must wait for all methods to complete before closing the object.
 */
@ThreadSafe
public interface ClosureLock {

	/**
	 * A scope during which this lock is taken, that is, calls to
	 * {@link ClosureLock#stopNewCalls()} are blocked until all scopes have been closed.
	 */
	interface Scope extends AutoCloseable {

		@Override
		void close(); // no exception
	}

	/**
	 * Yields a scope during which the object cannot be closed.
	 * 
	 * @param <E> the type of the supplied exception
	 * @param exception the supplier of the exception thrown if the object is already closed
	 * @return the scope
	 * @throws E if the object is already closed
	 */
	<E extends Exception> Scope scope(Supplier<E> exception) throws E;

	/**
	 * Stops future new calls and waits for all unfinished calls to complete.
	 * 
	 * @return true if and only if it actually stopped new calls, false if
	 *         this situation already held, because it was already requested
	 * @throws InterruptedException if the execution gets interrupted while
	 *                              waiting for unfinished calls to complete
	 */
	boolean stopNewCalls() throws InterruptedException;
}