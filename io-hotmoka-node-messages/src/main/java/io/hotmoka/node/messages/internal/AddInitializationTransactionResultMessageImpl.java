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

package io.hotmoka.node.messages.internal;

import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.messages.api.AddInitializationTransactionResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#addInitializationTransaction(InitializationTransactionRequest)} method.
 */
public class AddInitializationTransactionResultMessageImpl extends AbstractRpcMessage implements AddInitializationTransactionResultMessage {

	/**
	 * Creates the message.
	 * 
	 * @param id the identifier of the message
	 */
	public AddInitializationTransactionResultMessageImpl(String id) {
		super(id);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AddInitializationTransactionResultMessage && super.equals(other);
	}

	@Override
	protected String getExpectedType() {
		return AddInitializationTransactionResultMessage.class.getName();
	}
}