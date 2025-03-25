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
import java.util.stream.Stream;

import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetStateResultMessage;
import io.hotmoka.node.messages.internal.gson.GetStateResultMessageJson;
import io.hotmoka.websockets.beans.AbstractRpcMessage;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
		this(
			Objects.requireNonNull(result, "result cannot be null", IllegalArgumentException::new).toArray(Update[]::new),
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
	public GetStateResultMessageImpl(GetStateResultMessageJson json) throws InconsistentJsonException {
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
	private <E extends Exception> GetStateResultMessageImpl(Update[] result, String id, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(Objects.requireNonNull(id, "id cannot be null", onIllegalArgs));
	
		this.result = result;
	}

	private static Update[] unmapResult(GetStateResultMessageJson json) throws InconsistentJsonException {
		Updates.Json[] updatesJson = json.getResult().toArray(Updates.Json[]::new);
		var result = new Update[updatesJson.length];
		int pos = 0;
		for (var updateJson: updatesJson)
			result[pos++] = Objects.requireNonNull(updateJson, "result cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
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