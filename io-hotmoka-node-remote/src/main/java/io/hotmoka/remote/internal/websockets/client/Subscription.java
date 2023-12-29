/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subscription to a topic with its result handler.
 */
class Subscription {
	private final static Logger LOGGER = Logger.getLogger(Subscription.class.getName());

	private final String topic;
    private final String subscriptionId;
    private final ResultHandler<?> resultHandler;
    private final CountDownLatch latch = new CountDownLatch(1);

    Subscription(String topic, String subscriptionId, ResultHandler<?> resultHandler) {
        this.topic = topic;
        this.subscriptionId = subscriptionId;
        this.resultHandler = resultHandler;
    }

    /**
     * Emits that the subscription is completed.
     */
    public void emitSubscription() {
    	latch.countDown();
    }

    /**
     * Awaits if necessary for the subscription to complete.
     */
    public void awaitSubscription() {
    	try {
    		latch.await();
    	}
    	catch (InterruptedException e) {
    		LOGGER.log(Level.SEVERE, "interrupted while waiting for subscription", e);
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