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

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.messages.api.GetIndexMessage;
import io.hotmoka.node.messages.internal.GetIndexMessageImpl;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The JSON representation of an {@link GetIndexMessage}.
 */
public abstract class GetIndexMessageJson extends AbstractRpcMessageJsonRepresentation<GetIndexMessage> {
	private final StorageValues.Json reference;

	protected GetIndexMessageJson(GetIndexMessage message) {
		super(message);

		this.reference = new StorageValues.Json(message.getReference());
	}

	public final StorageValues.Json getReference() {
		return reference;
	}

	@Override
	public GetIndexMessage unmap() throws InconsistentJsonException {
		return new GetIndexMessageImpl(this);
	}

	@Override
	protected String getExpectedType() {
		return GetIndexMessage.class.getName();
	}
}