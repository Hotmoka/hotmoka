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

package io.hotmoka.node.internal.nodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.SubscriptionsManager;

/**
 * Implementation of a manager of subscriptions to the events occurring in a Hotmoka node.
 */
@ThreadSafe
public class SubscriptionsManagerImpl implements SubscriptionsManager {

	/**
	 * A map from each key of events to the subscription for that key.
	 * The {@code null} key is allowed, meaning that the subscriptions are for all keys.
	 */
	private final Map<StorageReference, Set<SubscriptionImpl>> subscriptions = new HashMap<>();

	/**
	 * Creates a new manager of subscriptions.
	 */
	public SubscriptionsManagerImpl() {}

	@Override
	public Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) {
		Objects.requireNonNull(handler, "the handler cannot be null");

		var subscription = new SubscriptionImpl(creator, handler);

		synchronized (subscriptions) {
			subscriptions.computeIfAbsent(creator, __ -> new HashSet<>()).add(subscription);
		}

		return subscription;
	}

	@Override
	public void notifyEvent(StorageReference creator, StorageReference event) {
		Set<SubscriptionImpl> subscriptionsPerKey;
		Set<SubscriptionImpl> subscriptionsForAllKeys;

		synchronized (subscriptions) {
			subscriptionsPerKey = subscriptions.get(creator);
			subscriptionsForAllKeys = subscriptions.get(null);
		}

		if (subscriptionsPerKey != null)
			subscriptionsPerKey.forEach(subscription -> subscription.accept(creator, event));

		// we forward the event also to the subscriptions for all keys
		if (subscriptionsForAllKeys != null)
			subscriptionsForAllKeys.forEach(subscription -> subscription.accept(creator, event));
	}

	/**
	 * An implementation of a subscription to events. It handles events
	 * with the event handler provided to the constructor and unsubscribes to events on close.
	 */
	private class SubscriptionImpl implements Subscription {
		private final StorageReference key;
		private final BiConsumer<StorageReference, StorageReference> handler;

		private SubscriptionImpl(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) {
			this.key = key;
			this.handler = handler;
		}

		@Override
		public void close() {
			synchronized (subscriptions) {
				Set<SubscriptionImpl> subscriptionsForKey = subscriptions.get(key);
				if (subscriptionsForKey != null && subscriptionsForKey.remove(this) && subscriptionsForKey.isEmpty())
					subscriptions.remove(key);
			}
		}

		private void accept(StorageReference key, StorageReference event) {
			handler.accept(key, event);
		}
	}
}