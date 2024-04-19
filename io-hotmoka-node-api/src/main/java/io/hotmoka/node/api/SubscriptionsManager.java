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

package io.hotmoka.node.api;

import java.util.function.BiConsumer;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A manager of subscriptions to the events occurring in a Hotmoka node.
 */
@ThreadSafe
public interface SubscriptionsManager {

	/**
	 * Subscribes the given handler for events with the given creator.
	 * 
	 * @param creator the creator of the events that will be forwarded to the handler; if this is {@code null},
	 *                all events will be forwarded to the handler
	 * @param handler a handler that gets executed when an event with the given creator occurs; a handler can be
	 *                subscribed to more creators; for each event, it receives its creator and the event itself
	 * @return the subscription, that can be closed later to stop event handling with {@code handler}
	 */
	Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler);

	/**
	 * Notifies the given event to all event handlers for the given creator.
	 * 
	 * @param creator the creator of the event
	 * @param event the event to notify
	 */
	void notifyEvent(StorageReference creator, StorageReference event);
}