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
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.messages.api.GetPolledResponseMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to {@link Node#getPolledResponse(TransactionReference)}.
 */
public class GetPolledResponseMessageImpl extends AbstractRpcMessage implements GetPolledResponseMessage {

	private final TransactionReference reference;

	/**
	 * Creates the message.
	 * 
	 * @param reference the reference to the transaction whose response is required
	 * @param id the identifier of the message
	 */
	public GetPolledResponseMessageImpl(TransactionReference reference, String id) {
		super(id);

		this.reference = Objects.requireNonNull(reference, "reference cannot be null");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GetPolledResponseMessage gprm && super.equals(other) && reference.equals(gprm.getReference());
	}

	@Override
	protected String getExpectedType() {
		return GetPolledResponseMessage.class.getName();
	}

	@Override
	public TransactionReference getReference() {
		return reference;
	}
}