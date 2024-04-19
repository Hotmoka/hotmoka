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

import java.util.Optional;

import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest)} method.
 */
public class RunInstanceMethodCallTransactionResultMessageImpl extends AbstractRpcMessage implements RunInstanceMethodCallTransactionResultMessage {

	/**
	 * The result of the call.
	 */
	private final Optional<StorageValue> result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call; this might be empty for void methods
	 * @param id the identifier of the message
	 */
	public RunInstanceMethodCallTransactionResultMessageImpl(Optional<StorageValue> result, String id) {
		super(id);

		this.result = result;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RunInstanceMethodCallTransactionResultMessage rimctrm && super.equals(other) && result.equals(rimctrm.get());
	}

	@Override
	protected String getExpectedType() {
		return RunInstanceMethodCallTransactionResultMessage.class.getName();
	}

	@Override
	public Optional<StorageValue> get() {
		return result;
	}
}