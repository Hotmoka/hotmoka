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

import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionMessages;
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of an {@link AddJarStoreInitialTransactionMessage}.
 */
public abstract class AddJarStoreInitialTransactionMessageJson extends AbstractRpcMessageJsonRepresentation<AddJarStoreInitialTransactionMessage> {
	private final TransactionRequests.Json request;

	protected AddJarStoreInitialTransactionMessageJson(AddJarStoreInitialTransactionMessage message) {
		super(message);

		this.request = new TransactionRequests.Json(message.getRequest());
	}

	@Override
	public AddJarStoreInitialTransactionMessage unmap() throws IllegalArgumentException, HexConversionException, Base64ConversionException {
		var unmappedRequest = request.unmap();
		if (unmappedRequest instanceof JarStoreInitialTransactionRequest jstr)
			return AddJarStoreInitialTransactionMessages.of(jstr, getId());
		else
			throw new IllegalArgumentException("The argument of the addJarStoreInitialTransactionRequest() method must be a JarStoreInitialTransactionRequest");
	}

	@Override
	protected String getExpectedType() {
		return AddJarStoreInitialTransactionMessage.class.getName();
	}
}