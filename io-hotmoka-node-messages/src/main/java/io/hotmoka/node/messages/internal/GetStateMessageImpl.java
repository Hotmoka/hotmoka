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
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetStateMessage;
import io.hotmoka.node.messages.internal.json.GetStateMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the network message corresponding to {@link Node#getState(StorageReference)}.
 */
public class GetStateMessageImpl extends AbstractRpcMessage implements GetStateMessage {

	private final StorageReference reference;

	/**
	 * Creates the message.
	 * 
	 * @param reference the reference to the object whose state is required
	 * @param id the identifier of the message
	 */
	public GetStateMessageImpl(StorageReference reference, String id) {
		this(reference, id, IllegalArgumentException::new);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public GetStateMessageImpl(GetStateMessageJson json) throws InconsistentJsonException {
		this(unmapReference(json), json.getId(), InconsistentJsonException::new);
	}

	/**
	 * Creates the message.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param reference the reference to the object whose state is required
	 * @param id the identifier of the message
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> GetStateMessageImpl(StorageReference reference, String id, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.reference = Objects.requireNonNull(reference, "reference cannot be null", onIllegalArgs);
	}

	private static StorageReference unmapReference(GetStateMessageJson json) throws InconsistentJsonException {
		var unmappedReference = Objects.requireNonNull(json.getReference(), "reference cannot be null", InconsistentJsonException::new).unmap();
		if (unmappedReference instanceof StorageReference sr)
			return sr;
		else
			throw new InconsistentJsonException("The argument of a getState() call must be a storage reference");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GetStateMessage gsm && super.equals(other) && reference.equals(gsm.getReference());
	}

	@Override
	protected String getExpectedType() {
		return GetStateMessage.class.getName();
	}

	@Override
	public StorageReference getReference() {
		return reference;
	}
}