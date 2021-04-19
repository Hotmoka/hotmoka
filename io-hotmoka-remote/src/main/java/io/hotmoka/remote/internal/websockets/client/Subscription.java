package io.hotmoka.remote.internal.websockets.client;

/**
 * Subscription to a topic with its result handler.
 */
public class Subscription {
    private final String topic;
    private final String subscriptionId;
    private final ResultHandler<?> resultHandler;
    private final Object LOCK = new Object();
    private boolean isSubscribed = false;

    public Subscription(String topic, String subscriptionId, ResultHandler<?> resultHandler) {
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

            if (isSubscribed) {
                return;
            }

            try {
                LOCK.wait();
                isSubscribed = true;
            }
            catch (InterruptedException e) {
                e.printStackTrace();
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
