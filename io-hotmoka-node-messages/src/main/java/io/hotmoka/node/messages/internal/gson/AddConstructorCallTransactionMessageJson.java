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

package io.hotmoka.node.messages.internal.gson;

import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionMessage;
import io.hotmoka.node.messages.internal.AddConstructorCallTransactionMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of an {@link AddConstructorCallTransactionMessage}.
 */
public abstract class AddConstructorCallTransactionMessageJson extends AbstractRpcMessageJsonRepresentation<AddConstructorCallTransactionMessage> {
	private final TransactionRequests.Json request;

	protected AddConstructorCallTransactionMessageJson(AddConstructorCallTransactionMessage message) {
		super(message);

		this.request = new TransactionRequests.Json(message.getRequest());
	}

	public final TransactionRequests.Json getRequest() {
		return request;
	}

	@Override
	public AddConstructorCallTransactionMessage unmap() throws InconsistentJsonException {
		return new AddConstructorCallTransactionMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return AddConstructorCallTransactionMessage.class.getName();
	}
}