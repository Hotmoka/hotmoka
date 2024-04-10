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

import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.api.PostInstanceMethodCallTransactionMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of a {@link PostInstanceMethodCallTransactionMessage}.
 */
public abstract class PostInstanceMethodCallTransactionMessageJson extends AbstractRpcMessageJsonRepresentation<PostInstanceMethodCallTransactionMessage> {
	private final TransactionRequests.Json request;

	protected PostInstanceMethodCallTransactionMessageJson(PostInstanceMethodCallTransactionMessage message) {
		super(message);

		this.request = new TransactionRequests.Json(message.getRequest());
	}

	@Override
	public PostInstanceMethodCallTransactionMessage unmap() throws IllegalArgumentException, HexConversionException, Base64ConversionException {
		var unmappedRequest = request.unmap();
		if (unmappedRequest instanceof InstanceMethodCallTransactionRequest cctr)
			return PostInstanceMethodCallTransactionMessages.of(cctr, getId());
		else
			throw new IllegalArgumentException("The argument of the postInstanceMethodCallTransactionRequest() method must be an InstanceMethodCallTransactionRequest");
	}

	@Override
	protected String getExpectedType() {
		return PostInstanceMethodCallTransactionMessage.class.getName();
	}
}