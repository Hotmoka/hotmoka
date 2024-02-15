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

package io.hotmoka.closeables.internal;

import java.util.function.Supplier;

import io.hotmoka.annotations.GuardedBy;
import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.api.ClosureLock;

/**
 * Implementation of an object used in closeables whose methods must be disabled by throwing
 * an exception after they have been closed. Moreover, the close
 * operation must wait for all methods to complete before closing the object.
 */
@ThreadSafe
public class ClosureLockImpl implements ClosureLock {

	private final Object lock = new Object();

	/**
	 * True if and only if {@link #stopNewCalls()} has been called already.
	 */
	@GuardedBy("lock")
	private boolean callsHaveBeenRequiredToStop;

	@GuardedBy("lock")
	private int currentCallsCount;

	@Override
	public <E extends Exception> Scope scope(Supplier<E> exception) throws E {
		beforeCall(exception);
		return this::afterCall;
	}

	@Override
	public boolean stopNewCalls() throws InterruptedException {
		synchronized (lock) {
			if (callsHaveBeenRequiredToStop)
				return false;
	
			callsHaveBeenRequiredToStop = true;
	
			if (currentCallsCount > 0)
				lock.wait();
	
			return true;
		}
	}

	/**
	 * Guarantees that the node is open if a call starts.
	 * 
	 * @param <E> the type of the supplied exception
	 * @param exception the supplier of the exception thrown if the object is already closed
	 * @throws E if the object is already closed
	 */
	private <E extends Exception> void beforeCall(Supplier<E> exception) throws E {
		synchronized (lock) {
			if (callsHaveBeenRequiredToStop)
				throw exception.get();

			currentCallsCount++;
		}
	}

	/**
	 * At the end of the last call, it signals every thread waiting for this event.
	 */
	private void afterCall() {
		synchronized (lock) {
			if (--currentCallsCount == 0)
				lock.notifyAll();
		}
	}
}