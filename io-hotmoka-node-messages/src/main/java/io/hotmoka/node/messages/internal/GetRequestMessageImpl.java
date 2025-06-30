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

import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.messages.api.GetRequestMessage;
import io.hotmoka.node.messages.internal.json.GetRequestMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the network message corresponding to {@link Node#getRequest(TransactionReference)}.
 */
public class GetRequestMessageImpl extends AbstractRpcMessage implements GetRequestMessage {

	private final TransactionReference reference;

	/**
	 * Creates the message.
	 * 
	 * @param reference the reference to the transaction whose request is required
	 * @param id the identifier of the message
	 */
	public GetRequestMessageImpl(TransactionReference reference, String id) {
		this(reference, id, IllegalArgumentException::new);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public GetRequestMessageImpl(GetRequestMessageJson json) throws InconsistentJsonException {
		this(
			Objects.requireNonNull(json.getReference(), "reference cannot be null", InconsistentJsonException::new).unmap(),
			json.getId(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Creates the message.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param reference the reference to the object whose class tag is required
	 * @param id the identifier of the message
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> GetRequestMessageImpl(TransactionReference reference, String id, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.reference = Objects.requireNonNull(reference, "reference cannot be null", onIllegalArgs);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GetRequestMessage grm && super.equals(other) && reference.equals(grm.getReference());
	}

	@Override
	protected String getExpectedType() {
		return GetRequestMessage.class.getName();
	}

	@Override
	public TransactionReference getReference() {
		return reference;
	}
}