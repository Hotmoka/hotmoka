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
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.messages.PostConstructorCallTransactionMessages;
import io.hotmoka.node.messages.api.PostConstructorCallTransactionMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link PostConstructorCallTransactionMessage}.
 */
public abstract class PostConstructorCallTransactionMessageJson extends AbstractRpcMessageJsonRepresentation<PostConstructorCallTransactionMessage> {
	private final TransactionRequests.Json request;

	protected PostConstructorCallTransactionMessageJson(PostConstructorCallTransactionMessage message) {
		super(message);

		this.request = new TransactionRequests.Json(message.getRequest());
	}

	@Override
	public PostConstructorCallTransactionMessage unmap() throws InconsistentJsonException {
		var unmappedRequest = request.unmap();
		if (unmappedRequest instanceof ConstructorCallTransactionRequest cctr)
			return PostConstructorCallTransactionMessages.of(cctr, getId());
		else
			throw new InconsistentJsonException("The argument of the postConstructorCallTransactionRequest() method must be a ConstructorCallTransactionRequest");
	}

	@Override
	protected String getExpectedType() {
		return PostConstructorCallTransactionMessage.class.getName();
	}
}