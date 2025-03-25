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

import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.AddGameteCreationTransactionResultMessage;
import io.hotmoka.node.messages.internal.gson.AddGameteCreationTransactionResultMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#addGameteCreationTransaction(GameteCreationTransactionRequest)} method.
 */
public class AddGameteCreationTransactionResultMessageImpl extends AbstractRpcMessage implements AddGameteCreationTransactionResultMessage {

	/**
	 * The result of the call.
	 */
	private final StorageReference result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 */
	public AddGameteCreationTransactionResultMessageImpl(StorageReference result, String id) {
		this(result, id, IllegalArgumentException::new);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public AddGameteCreationTransactionResultMessageImpl(AddGameteCreationTransactionResultMessageJson json) throws InconsistentJsonException {
		this(unmapResult(json), json.getId(), InconsistentJsonException::new);
	}

	/**
	 * Creates the message.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param result the result of the transaction
	 * @param id the identifier of the message
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> AddGameteCreationTransactionResultMessageImpl(StorageReference result, String id, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.result = Objects.requireNonNull(result, "result cannot be null", onIllegalArgs);
	}

	private static StorageReference unmapResult(AddGameteCreationTransactionResultMessageJson json) throws InconsistentJsonException {
		var unmappedResult = Objects.requireNonNull(json.getResult(), "result cannot be null", InconsistentJsonException::new).unmap();
		if (unmappedResult instanceof StorageReference sr)
			return sr;
		else
			throw new InconsistentJsonException("The result of addGameteCreationTransaction() must be a StorageReference");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AddGameteCreationTransactionResultMessage agctrm && super.equals(other) && result.equals(agctrm.get());
	}

	@Override
	protected String getExpectedType() {
		return AddGameteCreationTransactionResultMessage.class.getName();
	}

	@Override
	public StorageReference get() {
		return result;
	}
}