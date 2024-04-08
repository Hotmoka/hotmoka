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

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.messages.api.GetStateResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#getState(StorageReference)} method.
 */
public class GetStateResultMessageImpl extends AbstractRpcMessage implements GetStateResultMessage {

	/**
	 * The result of the call.
	 */
	private final Update[] result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 */
	public GetStateResultMessageImpl(Stream<Update> result, String id) {
		super(id);

		this.result = Objects.requireNonNull(result, "result cannot be null")
			.map(update -> Objects.requireNonNull(update, "result cannot contain null elements"))
			.toArray(Update[]::new);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GetStateResultMessageImpl gsrmi)
			return super.equals(other) && Arrays.equals(result, gsrmi.result); // optimization
		else
			return other instanceof GetStateResultMessage gsrm && super.equals(other) && Arrays.equals(result, gsrm.get().toArray(Update[]::new));
	}

	@Override
	protected String getExpectedType() {
		return GetStateResultMessage.class.getName();
	}

	@Override
	public Stream<Update> get() {
		return Stream.of(result);
	}
}