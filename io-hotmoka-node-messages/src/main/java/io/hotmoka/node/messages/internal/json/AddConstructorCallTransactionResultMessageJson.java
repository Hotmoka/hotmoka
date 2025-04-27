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

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionResultMessage;
import io.hotmoka.node.messages.internal.AddConstructorCallTransactionResultMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of an {@link AddConstructorCallTransactionResultMessage}.
 */
public abstract class AddConstructorCallTransactionResultMessageJson extends AbstractRpcMessageJsonRepresentation<AddConstructorCallTransactionResultMessage> {
	private final StorageValues.Json result;

	protected AddConstructorCallTransactionResultMessageJson(AddConstructorCallTransactionResultMessage message) {
		super(message);

		this.result = new StorageValues.Json(message.get());
	}

	public final StorageValues.Json getResult() {
		return result;
	}

	@Override
	public AddConstructorCallTransactionResultMessage unmap() throws InconsistentJsonException {
		return new AddConstructorCallTransactionResultMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return AddConstructorCallTransactionResultMessage.class.getName();
	}
}