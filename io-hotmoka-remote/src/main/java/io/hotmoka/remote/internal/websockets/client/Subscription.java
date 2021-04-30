/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.remote.internal.websockets.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscription to a topic with its result handler.
 */
class Subscription {
	private final static Logger LOGGER = LoggerFactory.getLogger(Subscription.class);

	private final String topic;
    private final String subscriptionId;
    private final ResultHandler<?> resultHandler;
    private final Object LOCK = new Object();
    private boolean isSubscribed = false;

    Subscription(String topic, String subscriptionId, ResultHandler<?> resultHandler) {
        this.topic = topic;
        this.subscriptionId = subscriptionId;
        this.resultHandler = resultHandler;
    }

    /**
     * Emits that the subscription is completed.
     */
    public void emitSubscription() {
        synchronized(LOCK) {
            LOCK.notify();
        }
    }

    /**
     * Awaits if necessary for the subscription to complete.
     */
    public void awaitSubscription() {
    	synchronized(LOCK) {
    		if (!isSubscribed)
    			try {
    				LOCK.wait();
    				isSubscribed = true;
    			}
    			catch (InterruptedException e) {
    				LOGGER.error("interrupted while waiting for subscription", e);
    			}
    	}
    }

    public ResultHandler<?> getResultHandler() {
        return resultHandler;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getTopic() {
        return topic;
    }
}