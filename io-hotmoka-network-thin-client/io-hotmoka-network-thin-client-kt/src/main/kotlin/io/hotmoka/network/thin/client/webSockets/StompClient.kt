package io.hotmoka.network.thin.client.webSockets

import com.google.gson.Gson
import io.hotmoka.network.thin.client.exceptions.InternalFailureException
import io.hotmoka.network.thin.client.models.errors.ErrorModel
import io.hotmoka.network.thin.client.webSockets.stomp.*
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import kotlin.jvm.Throws


/**
 * A thread safe webSocket client which implements the STOMP protocol [https://stomp.github.io/index.html].
 * @param url the url of the webSocket endpoint, without the protocol, e.g localhost:8080
 */
class StompClient(private val url: String): AutoCloseable {
    private val clientKey = generateClientKey()
    private val okHttpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    private val subscriptions: MutableMap<String, InternalSubscription> = mutableMapOf()
    private lateinit var webSocket: WebSocket
    private val gson = Gson()


    /**
     * It opens a webSocket connection and connects to the STOMP endpoint.
     * @param onStompConnectionOpened handler for a successful STOMP endpoint connection
     * @param onWebSocketFailure handler for the webSocket connection failure due to an error reading from or writing to the network
     * @param onWebSocketClosed handler for the webSocket connection when both peers have indicated that no more messages
     * will be transmitted and the connection has been successfully released.
     */
    fun connect(
        onStompConnectionOpened: (() -> Unit)? = null,
        onWebSocketFailure: (() -> Unit)? = null,
        onWebSocketClosed: (() -> Unit)? = null
    ) {
        println("[Stomp client] Connecting to $url ...")

        val request = Request.Builder()
            .url("ws://$url")
            .addHeader("uuid", clientKey)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                // we open the stomp session
                webSocket.send(StompMessageBuilder.buildConnectMessage())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {

                try {
                    val message = StompMessageParser(text).parse()
                    val payload = message.payload

                    when (message.command) {

                        StompCommand.CONNECTED -> {
                            println("[Stomp client] Connected to stomp session")
                            onStompConnectionOpened?.invoke()
                        }
                        StompCommand.RECEIPT -> {
                            val destination = message.headers.getDestination()
                            println("[Stomp client] Subscribed to topic $destination")

                            synchronized(subscriptions) {
                                val subscription = subscriptions[destination] ?: throw NoSuchElementException("Topic not found")
                                subscription.emitSubscription()
                            }
                        }
                        StompCommand.ERROR -> {
                            println("[Stomp client] STOMP Session Error: $payload")
                            // clean-up client resources because the server closed the connection
                            close()
                            onWebSocketFailure?.invoke()
                        }
                        StompCommand.MESSAGE -> {
                            val destination = message.headers.getDestination()
                            println("[Stomp client] Received message from topic $destination")
                            handleStompDestinationPayload(payload, destination)
                        }
                        else -> println("Got an unknown message")
                    }

                } catch (e: Exception) {
                    println("[Stomp client] Got an exception while handling message")
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("[Stomp client] WebSocket Session error")
                t.printStackTrace()

                close()
                onWebSocketFailure?.invoke()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("[Stomp client] WebSocket session closed")
                onWebSocketClosed?.invoke()
            }
        })
    }

    /**
     * Returns the key of this client instance. Each instance has a different key.
     * The key is an UUID.
     */
    fun getClientKey(): String {
        return this.clientKey
    }

    /**
     * It subscribes to a topic.
     * @param topic the topic
     * @param resultTypeClass the result type class
     * @param handler the handler to consume with the result and/or error
     * @param afterSubscribed optional callback to be invoked after a successful subscription
     */
    fun <T> subscribeTo(topic: String, resultTypeClass: Class<T>, handler: BiConsumer<T?, ErrorModel?>, afterSubscribed: (() -> Unit)? = null): Subscription {
        println("[Stomp client] Subscribing to  $topic")

        val isSubscribed: Boolean
        val subscription: InternalSubscription

        synchronized(subscriptions) {
            isSubscribed = subscriptions[topic] != null
            subscription = subscriptions.computeIfAbsent(topic) { topic_ ->
                val subscriptionId = "" + (subscriptions.size + 1)
                InternalSubscription(Subscription(topic_, subscriptionId, this), ResultHandler(handler, resultTypeClass), afterSubscribed)
            }
        }

        if (!isSubscribed) {
            webSocket.send(StompMessageBuilder.buildSubscribeMessage(subscription.clientSubscription.topic, subscription.clientSubscription.subscriptionId))
            subscription.awaitSubscription()
            subscription.afterSubscribed?.invoke()
        }

        return subscription.clientSubscription
    }

    /**
     * It unsubscribes from a topic.
     * @param topic the topic
     */
    @Throws(NoSuchElementException::class)
    fun unsubscribeFrom(topic: String) {
        println("[Stomp client] Unsubscribing from  $topic")

        val subscriptionId: String
        synchronized(subscriptions) {
            subscriptionId = subscriptions[topic]?.clientSubscription?.subscriptionId ?: throw NoSuchElementException("Topic not found")
            subscriptions.remove(topic)
        }

        webSocket.send(StompMessageBuilder.buildUnsubscribeMessage(subscriptionId))
    }

    /**
     * It sends a payload to a destination.
     * @param destination the destination
     * @param payload the payload
     */
    fun <T> sendTo(destination: String, payload: T?) {
        println("[Stomp client] Sending message to destination $destination")
        webSocket.send(StompMessageBuilder.buildSendMessage(destination, payload))
    }

    /**
     * It handles the STOMP payload of a destination.
     * @param payload the payload
     * @param destination the destination
     */
    private fun handleStompDestinationPayload(payload: String?, destination: String) {

        val resultHandler: ResultHandler<*>?
        synchronized(subscriptions) {
            resultHandler = subscriptions[destination]?.resultHandler
        }

        resultHandler?.let { handler ->

            when {
                handler.resultTypeClass == Unit::class.java -> {
                    handler.handler.accept(null, null)
                }
                payload != null -> {
                    try {
                        handler.handler.accept(handler.toModel(payload, gson), null)
                    } catch (e: InternalFailureException) {
                        handler.handler.accept(null, ErrorModel(e.message ?: "Got a deserialization error", InternalFailureException::class.java.name))
                    }
                }
                else -> {
                    handler.handler.accept(null, ErrorModel("Got an error", InternalFailureException::class.java.name))
                }
            }
        }
    }


    /**
     * Clears the subscriptions and closes the webSocket connection.
     */
    override fun close() {
        println("[Stomp client] Closing webSocket session")

        synchronized(subscriptions) {
            subscriptions.clear()
        }

        // indicates a normal closure
        webSocket.close(1000, null)
    }

    /**
     * Generates an UUID for this webSocket client.
     */
    private fun generateClientKey(): String {
        return UUID.randomUUID().toString()
    }
}