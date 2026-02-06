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

package io.hotmoka.node.messages.internal.json;

import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionMessage;
import io.hotmoka.node.messages.internal.AddJarStoreInitialTransactionMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of an {@link AddJarStoreInitialTransactionMessage}.
 */
public abstract class AddJarStoreInitialTransactionMessageJson extends AbstractRpcMessageJsonRepresentation<AddJarStoreInitialTransactionMessage> {
	private final TransactionRequests.Json request;

	protected AddJarStoreInitialTransactionMessageJson(AddJarStoreInitialTransactionMessage message) {
		super(message);

		this.request = new TransactionRequests.Json(message.getRequest());
	}

	/**
	 * Yields the JSON of the request in the message.
	 * 
	 * @return the JSON of the request in the message
	 */
	public final TransactionRequests.Json getRequest() {
		return request;
	}

	@Override
	public AddJarStoreInitialTransactionMessage unmap() throws InconsistentJsonException {
		return new AddJarStoreInitialTransactionMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return AddJarStoreInitialTransactionMessage.class.getName();
	}
}