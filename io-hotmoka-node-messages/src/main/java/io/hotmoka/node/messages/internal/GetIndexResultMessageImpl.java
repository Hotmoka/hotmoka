/*
Copyright 2025 Fausto Spoto

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

import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetIndexResultMessage;
import io.hotmoka.node.messages.internal.json.GetIndexResultMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#getIndex(StorageReference)} method.
 */
public class GetIndexResultMessageImpl extends AbstractRpcMessage implements GetIndexResultMessage {

	/**
	 * The result of the call.
	 */
	private final TransactionReference[] result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 */
	public GetIndexResultMessageImpl(Stream<TransactionReference> result, String id) {
		this(
			Objects.requireNonNull(result, "result cannot be null", IllegalArgumentException::new).toArray(TransactionReference[]::new),
			id,
			IllegalArgumentException::new
		);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public GetIndexResultMessageImpl(GetIndexResultMessageJson json) throws InconsistentJsonException {
		this(unmapResult(json), json.getId(), InconsistentJsonException::new);
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
	private <E extends Exception> GetIndexResultMessageImpl(TransactionReference[] result, String id, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.result = result;
	}

	private static TransactionReference[] unmapResult(GetIndexResultMessageJson json) throws InconsistentJsonException {
		TransactionReferences.Json[] referencesJson = json.getResult().toArray(TransactionReferences.Json[]::new);
		var result = new TransactionReference[referencesJson.length];
		int pos = 0;
		for (var referenceJson: referencesJson)
			result[pos++] = Objects.requireNonNull(referenceJson, "result cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GetIndexResultMessageImpl gsrmi)
			return super.equals(other) && Arrays.equals(result, gsrmi.result); // optimization
		else
			return other instanceof GetIndexResultMessage girm && super.equals(other) && Arrays.equals(result, girm.get().toArray(TransactionReference[]::new));
	}

	@Override
	protected String getExpectedType() {
		return GetIndexResultMessage.class.getName();
	}

	@Override
	public Stream<TransactionReference> get() {
		return Stream.of(result);
	}
}