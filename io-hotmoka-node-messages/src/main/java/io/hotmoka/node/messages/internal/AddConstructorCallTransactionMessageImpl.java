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

import java.util.Objects;

import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to {@link Node#addConstructorCallTransaction(ConstructorCallTransactionRequest)}.
 */
public class AddConstructorCallTransactionMessageImpl extends AbstractRpcMessage implements AddConstructorCallTransactionMessage {

	private final ConstructorCallTransactionRequest request;

	/**
	 * Creates the message.
	 * 
	 * @param request the request of the transaction required to add
	 * @param id the identifier of the message
	 */
	public AddConstructorCallTransactionMessageImpl(ConstructorCallTransactionRequest request, String id) {
		super(id);

		this.request = Objects.requireNonNull(request, "request cannot be null");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AddConstructorCallTransactionMessage acctm && super.equals(other) && request.equals(acctm.getRequest());
	}

	@Override
	protected String getExpectedType() {
		return AddConstructorCallTransactionMessage.class.getName();
	}

	@Override
	public ConstructorCallTransactionRequest getRequest() {
		return request;
	}
}