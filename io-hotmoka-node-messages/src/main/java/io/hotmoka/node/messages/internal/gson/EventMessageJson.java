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

import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.EventMessages;
import io.hotmoka.node.messages.api.EventMessage;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of an {@link EventMessage}.
 */
public abstract class EventMessageJson implements JsonRepresentation<EventMessage> {
	private final StorageValues.Json creator;
	private final StorageValues.Json event;

	protected EventMessageJson(EventMessage message) {
		this.creator = new StorageValues.Json(message.getCreator());
		this.event = new StorageValues.Json(message.getEvent());
	}

	@Override
	public EventMessage unmap() throws IllegalArgumentException, HexConversionException {
		var unmappedEvent = event.unmap();
		if (unmappedEvent instanceof StorageReference event) {
			var unmappedCreator = creator.unmap();
			if (unmappedCreator instanceof StorageReference creator)
				return EventMessages.of(creator, event);
			else
				throw new IllegalArgumentException("The creator of an event message must be a storage reference");
		}
		else
			throw new IllegalArgumentException("The event in an event message must be a storage reference");
	}
}