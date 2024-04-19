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

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.api.GetClassTagMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of an {@link GetClassTagMessage}.
 */
public abstract class GetClassTagMessageJson extends AbstractRpcMessageJsonRepresentation<GetClassTagMessage> {
	private final StorageValues.Json reference;

	protected GetClassTagMessageJson(GetClassTagMessage message) {
		super(message);

		this.reference = new StorageValues.Json(message.getReference());
	}

	@Override
	public GetClassTagMessage unmap() throws IllegalArgumentException, HexConversionException {
		var unmappedReference = reference.unmap();
		if (unmappedReference instanceof StorageReference sr)
			return GetClassTagMessages.of(sr, getId());
		else
			throw new IllegalArgumentException("The argument of a getClassTag() call must be a storage reference");
	}

	@Override
	protected String getExpectedType() {
		return GetClassTagMessage.class.getName();
	}
}