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
import io.hotmoka.closeables.CloseHandlersManagers;
import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.closeables.api.OnCloseHandlersContainer;
import io.hotmoka.closeables.api.OnCloseHandlersManager;

/**
 * Partial implementation of an autocloseable with support for blocking calls to its methods after
 * it has been closed. Any attempt to call a method after closure will throw an exception.
 * Moreover, it has support for close handlers, that must be called when the
 * object gets closed.
 * 
 * @param <E> the type of exception thrown if {@link #mkScope()} is called after {@link #stopNewCalls()}
 */
@ThreadSafe
public abstract class AutoCloseableWithLockAndHandlersImpl<E extends Exception> extends AutoCloseableWithLockImpl<E> implements OnCloseHandlersContainer, AutoCloseable {

	/**
	 * The close handlers manager.
	 */
	private final OnCloseHandlersManager manager = CloseHandlersManagers.create();

	/**
	 * Creates the autocloseable.
	 * 
	 * @param exceptionSupplier the supplier of the exception that must be thrown when attempting
	 *                          to create a call scope after closure
	 */
	protected AutoCloseableWithLockAndHandlersImpl(Supplier<E> exceptionSupplier) {
		super(exceptionSupplier);
	}

	@Override
	public void addOnCloseHandler(OnCloseHandler handler) {
		manager.addOnCloseHandler(handler);
	}

	@Override
	public void removeOnCloseHandler(OnCloseHandler handler) {
		manager.removeOnCloseHandler(handler);
	}

	/**
	 * Calls all close handlers added to this object. If any of them fails with an exception,
	 * it tries to close the others before giving up and throwing the exception.
	 * 
	 * @throws InterruptedException if the closure operation gets interrupted
	 * @throws Exception if some handler throws this exception
	 */
	protected void callCloseHandlers() throws InterruptedException, Exception {
		manager.close();
	}
}