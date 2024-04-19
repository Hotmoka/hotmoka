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

import java.util.stream.Stream;

import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.Updates;
import io.hotmoka.node.messages.GetStateResultMessages;
import io.hotmoka.node.messages.api.GetStateResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of a {@link GetStateResultMessage}.
 */
public abstract class GetStateResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetStateResultMessage> {
	private final Updates.Json[] result;

	protected GetStateResultMessageJson(GetStateResultMessage message) {
		super(message);

		this.result = message.get()
			.map(Updates.Json::new)
			.toArray(Updates.Json[]::new);
	}

	@Override
	public GetStateResultMessage unmap() throws HexConversionException {
		return CheckSupplier.check(HexConversionException.class, () -> GetStateResultMessages.of(Stream.of(result).map(UncheckFunction.uncheck(Updates.Json::unmap)), getId()));
	}

	@Override
	protected String getExpectedType() {
		return GetStateResultMessage.class.getName();
	}
}