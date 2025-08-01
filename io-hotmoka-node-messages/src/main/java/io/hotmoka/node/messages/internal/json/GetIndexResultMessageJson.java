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

package io.hotmoka.node.messages.internal.json;

import java.util.stream.Stream;

import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.messages.api.GetIndexResultMessage;
import io.hotmoka.node.messages.internal.GetIndexResultMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link GetIndexResultMessage}.
 */
public abstract class GetIndexResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetIndexResultMessage> {
	private final TransactionReferences.Json[] result;

	protected GetIndexResultMessageJson(GetIndexResultMessage message) {
		super(message);

		this.result = message.get()
			.map(TransactionReferences.Json::new)
			.toArray(TransactionReferences.Json[]::new);
	}

	public final Stream<TransactionReferences.Json> getResult() {
		return result == null ? Stream.empty() : Stream.of(result);
	}

	@Override
	public GetIndexResultMessage unmap() throws InconsistentJsonException {
		return new GetIndexResultMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return GetIndexResultMessage.class.getName();
	}
}