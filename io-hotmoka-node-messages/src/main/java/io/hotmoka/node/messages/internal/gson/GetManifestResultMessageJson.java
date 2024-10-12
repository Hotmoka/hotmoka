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

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.api.GetManifestResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of a {@link GetManifestResultMessage}.
 */
public abstract class GetManifestResultMessageJson extends AbstractRpcMessageJsonRepresentation<GetManifestResultMessage> {
	private final StorageValues.Json result;

	protected GetManifestResultMessageJson(GetManifestResultMessage message) {
		super(message);

		this.result = new StorageValues.Json(message.get());
	}

	@Override
	public GetManifestResultMessage unmap() throws InconsistentJsonException {
		var unmappedResult = result.unmap();
		if (unmappedResult instanceof StorageReference sr)
			return GetManifestResultMessages.of(sr, getId());
		else
			throw new InconsistentJsonException("The result of a getManifest() call must be a storage reference");
	}

	@Override
	protected String getExpectedType() {
		return GetManifestResultMessage.class.getName();
	}
}