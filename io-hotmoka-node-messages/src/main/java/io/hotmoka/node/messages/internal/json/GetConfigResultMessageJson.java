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

import java.security.NoSuchAlgorithmException;

import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.messages.api.GetConfigResultMessage;
import io.hotmoka.node.messages.internal.GetConsensusConfigResultMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link GetConfigResultMessage}.
 */
public abstract class GetConfigResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetConfigResultMessage> {
	private final ConsensusConfigBuilders.Json result;

	protected GetConfigResultMessageJson(GetConfigResultMessage message) {
		super(message);

		this.result = new ConsensusConfigBuilders.Json(message.get());
	}

	public final ConsensusConfigBuilders.Json getResult() {
		return result;
	}

	@Override
	public GetConfigResultMessage unmap() throws NoSuchAlgorithmException, InconsistentJsonException {
		return new GetConsensusConfigResultMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return GetConfigResultMessage.class.getName();
	}
}