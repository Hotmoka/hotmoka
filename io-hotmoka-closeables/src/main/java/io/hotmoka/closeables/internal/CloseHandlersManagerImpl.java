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

import java.util.concurrent.CopyOnWriteArrayList;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.api.CloseHandler;
import io.hotmoka.closeables.api.CloseHandlersManager;

/**
 * An implementation of a container of close handlers that, when closed, calls all the registered handlers.
 */
@ThreadSafe
public class CloseHandlersManagerImpl implements CloseHandlersManager {

	/**
	 * The handlers to run when this object gets closed.
	 */
	private final CopyOnWriteArrayList<CloseHandler> onCloseHandlers = new CopyOnWriteArrayList<>();

	@Override
	public void addCloseHandler(CloseHandler handler) {
		onCloseHandlers.add(handler);
	}

	@Override
	public void removeCloseHandler(CloseHandler handler) {
		onCloseHandlers.add(handler);
	}

	@Override
	public void close() throws InterruptedException, Exception {
		callCloseHandlers(onCloseHandlers.toArray(CloseHandler[]::new), 0);
	}

	private void callCloseHandlers(CloseHandler[] handlers, int pos) throws InterruptedException, Exception {
		if (pos < handlers.length) {
			try {
				handlers[pos].close();
			}
			finally {
				callCloseHandlers(handlers, pos + 1);
			}
		}
	}
}