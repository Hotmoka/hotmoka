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

package io.hotmoka.node.messages.internal;

import java.util.Objects;

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.messages.api.EventMessage;

/**
 * Implementation of the network message corresponding to an event generated in a Hotmoka node.
 */
public class EventMessageImpl implements EventMessage {
	private final StorageReference creator;
	private final StorageReference event;

	/**
	 * Creates the message.
	 * 
	 * @param creator the reference to the creator of the event
	 * @param event the reference to the event object
	 */
	public EventMessageImpl(StorageReference creator, StorageReference event) {
		this.creator = Objects.requireNonNull(creator, "creator cannot be null");
		this.event = Objects.requireNonNull(event, "event cannot be null");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EventMessage em && event.equals(em.getEvent()) && creator.equals(em.getCreator());
	}

	@Override
	public StorageReference getCreator() {
		return creator;
	}

	@Override
	public StorageReference getEvent() {
		return event;
	}
}