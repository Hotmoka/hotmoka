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

import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.messages.GetResponseMessages;
import io.hotmoka.node.messages.api.GetResponseMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of an {@link GetResponseMessage}.
 */
public abstract class GetResponseMessageJson extends AbstractRpcMessageJsonRepresentation<GetResponseMessage> {
	private final TransactionReferences.Json reference;

	protected GetResponseMessageJson(GetResponseMessage message) {
		super(message);

		this.reference = new TransactionReferences.Json(message.getReference());
	}

	@Override
	public GetResponseMessage unmap() throws InconsistentJsonException {
		return GetResponseMessages.of(reference.unmap(), getId());
	}

	@Override
	protected String getExpectedType() {
		return GetResponseMessage.class.getName();
	}
}