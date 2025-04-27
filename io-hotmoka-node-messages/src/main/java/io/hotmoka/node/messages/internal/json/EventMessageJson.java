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
import io.hotmoka.node.messages.api.EventMessage;
import io.hotmoka.node.messages.internal.EventMessageImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
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

	public final StorageValues.Json getCreator() {
		return creator;
	}

	public final StorageValues.Json getEvent() {
		return event;
	}

	@Override
	public EventMessage unmap() throws InconsistentJsonException {
		return new EventMessageImpl(this);
	}
}