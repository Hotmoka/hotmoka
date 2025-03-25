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
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionResultMessage;
import io.hotmoka.node.messages.internal.AddJarStoreInitialTransactionResultMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link AddJarStoreInitialTransactionResultMessage}.
 */
public abstract class AddJarStoreInitialTransactionResultMessageJson extends AbstractRpcMessageJsonRepresentation<AddJarStoreInitialTransactionResultMessage> {
	private final TransactionReferences.Json result;

	protected AddJarStoreInitialTransactionResultMessageJson(AddJarStoreInitialTransactionResultMessage message) {
		super(message);

		this.result = new TransactionReferences.Json(message.get());
	}

	public final TransactionReferences.Json getResult() {
		return result;
	}

	@Override
	public AddJarStoreInitialTransactionResultMessage unmap() throws InconsistentJsonException {
		return new AddJarStoreInitialTransactionResultMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return AddJarStoreInitialTransactionResultMessage.class.getName();
	}
}