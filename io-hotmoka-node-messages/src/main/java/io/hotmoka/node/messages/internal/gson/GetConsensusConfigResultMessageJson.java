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

package io.hotmoka.node.messages.internal.gson;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.beans.ConsensusConfigBuilders;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.node.messages.GetConsensusConfigResultMessages;
import io.hotmoka.node.messages.api.GetConsensusConfigResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of a {@link GetConsensusConfigResultMessage}.
 */
public abstract class GetConsensusConfigResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetConsensusConfigResultMessage> {
	private final ConsensusConfigBuilders.Json result;

	protected GetConsensusConfigResultMessageJson(GetConsensusConfigResultMessage message) {
		super(message);

		this.result = new ConsensusConfigBuilders.Json(message.get());
	}

	@Override
	public GetConsensusConfigResultMessage unmap() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, Base64ConversionException {
		return GetConsensusConfigResultMessages.of(result.unmap(), getId());
	}

	@Override
	protected String getExpectedType() {
		return GetConsensusConfigResultMessage.class.getName();
	}
}