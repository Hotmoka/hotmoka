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

import java.util.Optional;

import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.api.AddInstanceMethodCallTransactionResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of a {@link AddInstanceMethodCallTransactionResultMessage}.
 */
public abstract class AddInstanceMethodCallTransactionResultMessageJson extends AbstractRpcMessageJsonRepresentation<AddInstanceMethodCallTransactionResultMessage> {
	private final StorageValues.Json result;

	protected AddInstanceMethodCallTransactionResultMessageJson(AddInstanceMethodCallTransactionResultMessage message) {
		super(message);

		this.result = message.get().map(StorageValues.Json::new).orElse(null);
	}

	@Override
	public AddInstanceMethodCallTransactionResultMessage unmap() throws IllegalArgumentException, HexConversionException {
		return AddInstanceMethodCallTransactionResultMessages.of(result == null ? Optional.empty() : Optional.of(result.unmap()), getId());
	}

	@Override
	protected String getExpectedType() {
		return AddInstanceMethodCallTransactionResultMessage.class.getName();
	}
}