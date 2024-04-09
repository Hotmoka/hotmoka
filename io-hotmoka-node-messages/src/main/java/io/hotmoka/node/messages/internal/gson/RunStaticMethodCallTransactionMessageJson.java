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
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of an {@link RunStaticMethodCallTransactionMessage}.
 */
public abstract class RunStaticMethodCallTransactionMessageJson extends AbstractRpcMessageJsonRepresentation<RunStaticMethodCallTransactionMessage> {
	private final TransactionRequests.Json request;

	protected RunStaticMethodCallTransactionMessageJson(RunStaticMethodCallTransactionMessage message) {
		super(message);

		this.request = new TransactionRequests.Json(message.getRequest());
	}

	@Override
	public RunStaticMethodCallTransactionMessage unmap() throws IllegalArgumentException, HexConversionException, Base64ConversionException {
		var unmappedRequest = request.unmap();
		if (unmappedRequest instanceof StaticMethodCallTransactionRequest smctr)
			return RunStaticMethodCallTransactionMessages.of(smctr, getId());
		else
			throw new IllegalArgumentException("The argument of the runStaticMethodCallTransactionRequest() method must be a StaticMethodCallTransactionRequest");
	}

	@Override
	protected String getExpectedType() {
		return RunStaticMethodCallTransactionMessage.class.getName();
	}
}