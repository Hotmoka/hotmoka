package io.hotmoka.network.thin.client.webSockets.stomp

/**
 * It represents a STOMP message, a class which holds the STOMP command,
 * the STOMP headers and the STOMP payload.
 */
data class Message(
    val command: StompCommand,
    val headers: StompHeaders,
    val payload: String? = null
)