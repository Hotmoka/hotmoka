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
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.messages.api.GetRequestResultMessage;
import io.hotmoka.node.messages.internal.json.GetRequestResultMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#getRequest(TransactionReference)} method.
 */
public class GetRequestResultMessageImpl extends AbstractRpcMessage implements GetRequestResultMessage {

	/**
	 * The result of the call.
	 */
	private final TransactionRequest<?> result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 */
	public GetRequestResultMessageImpl(TransactionRequest<?> result, String id) {
		this(result, id, IllegalArgumentException::new);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public GetRequestResultMessageImpl(GetRequestResultMessageJson json) throws InconsistentJsonException {
		this(
			Objects.requireNonNull(json.getResult(), "result cannot be null", InconsistentJsonException::new).unmap(),
			json.getId(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Creates the message.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> GetRequestResultMessageImpl(TransactionRequest<?> result, String id, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.result = Objects.requireNonNull(result, "result cannot be null", onIllegalArgs);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GetRequestResultMessage grrm && super.equals(other) && result.equals(grrm.get());
	}

	@Override
	protected String getExpectedType() {
		return GetRequestResultMessage.class.getName();
	}

	@Override
	public TransactionRequest<?> get() {
		return result;
	}
}