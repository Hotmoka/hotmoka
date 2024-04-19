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
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetStateMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

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
		super(id);

		this.reference = Objects.requireNonNull(reference, "reference cannot be null");
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