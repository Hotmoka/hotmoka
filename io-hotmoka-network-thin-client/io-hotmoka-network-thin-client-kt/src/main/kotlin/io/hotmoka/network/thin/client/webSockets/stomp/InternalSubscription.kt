package io.hotmoka.network.thin.client.webSockets.stomp

import io.hotmoka.network.thin.client.webSockets.Subscription
import okhttp3.internal.notify
import okhttp3.internal.wait

/**
 * Internal subscription class which holds the client subscription,
 * the result handler of the topic subscription, and an optional callback
 * to be invoked after a successful subscription.
 */
class InternalSubscription(
    val clientSubscription: Subscription,
    val resultHandler: ResultHandler<*>,
    val afterSubscribed: (() -> Unit)? = null
) {
    private val lock = object{}


    /**
     * Emit that this subscription is successful.
     */
    fun emitSubscription() {
        synchronized(lock) {
            lock.notify()
        }
    }

    /**
     * Await for this subscription to finish.
     */
    fun awaitSubscription() {
       synchronized(lock) {
           lock.wait()
       }
    }
}