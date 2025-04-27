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

import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.EventMessage;
import io.hotmoka.node.messages.internal.json.EventMessageJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
		this(creator, event, IllegalArgumentException::new);
	}

	/**
	 * Creates the message from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public EventMessageImpl(EventMessageJson json) throws InconsistentJsonException {
		this(unmapCreator(json), unmapEvent(json), InconsistentJsonException::new);
	}

	/**
	 * Creates the message.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param creator the reference to the creator of the event
	 * @param event the reference to the event object
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> EventMessageImpl(StorageReference creator, StorageReference event, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.creator = Objects.requireNonNull(creator, "creator cannot be null", onIllegalArgs);
		this.event = Objects.requireNonNull(event, "event cannot be null", onIllegalArgs);
	}

	private static StorageReference unmapCreator(EventMessageJson json) throws InconsistentJsonException {
		var unmappedCreator = Objects.requireNonNull(json.getCreator(), "creator cannot be null", InconsistentJsonException::new).unmap();

		if (unmappedCreator instanceof StorageReference sr)
			return sr;
		else
			throw new InconsistentJsonException("The creator of an event message must be a storage reference");
	}

	private static StorageReference unmapEvent(EventMessageJson json) throws InconsistentJsonException {
		var unmappedEvent = Objects.requireNonNull(json.getEvent(), "event cannot be null", InconsistentJsonException::new).unmap();

		if (unmappedEvent instanceof StorageReference sr)
			return sr;
		else
			throw new InconsistentJsonException("The event of an event message must be a storage reference");
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