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

import java.util.Optional;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.api.GetManifestResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of a {@link GetManifestResultMessage}.
 */
public abstract class GetManifestResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetManifestResultMessage> {
	private final StorageValues.Json result;

	protected GetManifestResultMessageJson(GetManifestResultMessage message) {
		super(message);

		this.result = message.get().map(StorageValues.Json::new).orElse(null);
	}

	@Override
	public GetManifestResultMessage unmap() throws HexConversionException {
		if (result == null)
			return GetManifestResultMessages.of(Optional.empty(), getId());

		var unmappedResult = result.unmap();
		if (unmappedResult instanceof StorageReference sr)
			return GetManifestResultMessages.of(Optional.of(sr), getId());
		else
			throw new IllegalArgumentException("The result of a getManifest() call must be a storage reference");
	}

	@Override
	protected String getExpectedType() {
		return GetManifestResultMessage.class.getName();
	}
}