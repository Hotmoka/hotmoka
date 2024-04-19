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

import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.messages.GetRequestMessages;
import io.hotmoka.node.messages.api.GetRequestMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of an {@link GetRequestMessage}.
 */
public abstract class GetRequestMessageJson extends AbstractRpcMessageJsonRepresentation<GetRequestMessage> {
	private final TransactionReferences.Json reference;

	protected GetRequestMessageJson(GetRequestMessage message) {
		super(message);

		this.reference = new TransactionReferences.Json(message.getReference());
	}

	@Override
	public GetRequestMessage unmap() throws HexConversionException {
		return GetRequestMessages.of(reference.unmap(), getId());
	}

	@Override
	protected String getExpectedType() {
		return GetRequestMessage.class.getName();
	}
}