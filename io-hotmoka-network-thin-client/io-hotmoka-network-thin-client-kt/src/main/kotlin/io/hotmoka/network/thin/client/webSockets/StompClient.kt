package io.hotmoka.network.thin.client.webSockets

import com.google.gson.Gson
import io.hotmoka.network.thin.client.exceptions.InternalFailureException
import io.hotmoka.network.thin.client.models.errors.ErrorModel
import io.hotmoka.network.thin.client.webSockets.stomp.ResultHandler
import io.hotmoka.network.thin.client.webSockets.stomp.StompCommand
import io.hotmoka.network.thin.client.webSockets.stomp.StompMessageBuilder
import io.hotmoka.network.thin.client.webSockets.stomp.StompMessageParser
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import kotlin.jvm.Throws


/**
 * A webSocket client which implements the STOMP protocol [https://stomp.github.io/index.html].
 * @param url the url of the webSocket endpoint, without the protocol, e.g localhost:8080
 */
class StompClient(private val url: String): AutoCloseable {
    private val clientKey = generateClientKey()
    private val okHttpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    private val subscriptions: MutableMap<String, Subscription> = mutableMapOf()
    private lateinit var webSocket: WebSocket
    private val gson = Gson()


    var onStompSessionError: (() -> Unit)? = null



    /**
     * It opens a webSocket connection and connects to the STOMP endpoint.
     * @param onStompConnectionOpened handler for a successful STOMP endpoint connection
     * @param onWebSocketConnectionFailed handler for the webSocket connection failure
     * @param onWebSocketConnectionClosed handler for the webSocket connection closure
     */
    fun connect(
        onStompConnectionOpened: (() -> Unit)? = null,
        onWebSocketConnectionFailed: (() -> Unit)? = null,
        onWebSocketConnectionClosed: (() -> Unit)? = null
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
                        }
                        StompCommand.ERROR -> {
                            println("[Stomp client] STOMP Session Error: $payload")
                            onStompSessionErrorInternal()
                            onStompSessionError?.invoke()
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
                onWebSocketConnectionFailed?.invoke()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("[Stomp client] WebSocket session closed")
                onWebSocketConnectionClosed?.invoke()
            }
        })
    }

    /**
     * It subscribes to a topic.
     * @param topic the topic
     * @param resultTypeClass the result type class
     * @param handler the handler to consume with the result and/or error
     */
    fun <T> subscribeTo(topic: String, resultTypeClass: Class<T>, handler: BiConsumer<T?, ErrorModel?>): Subscription {
        println("[Stomp client] Subscribing to  $topic")

        val isSubscribed: Boolean
        val subscription: Subscription

        synchronized(subscriptions) {
            isSubscribed = subscriptions[topic] != null
            subscription = subscriptions.computeIfAbsent(topic) { topic_ ->
                val subscriptionId = "" + (subscriptions.size + 1)
                Subscription(topic_, subscriptionId, this, ResultHandler(handler, resultTypeClass))
            }
        }

        if (!isSubscribed) {
            webSocket.send(StompMessageBuilder.buildSubscribeMessage(subscription.topic, subscription.subscriptionId))
        }

        return subscription
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
            subscriptionId = subscriptions[topic]?.subscriptionId ?: throw NoSuchElementException("Topic not found")
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
     * Handler for the internal STOMP session error. It clears the subscriptions.
     */
    private fun onStompSessionErrorInternal() {
        synchronized(subscriptions) {
            subscriptions.clear()
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