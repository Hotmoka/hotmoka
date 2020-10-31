package io.hotmoka.network.thin.client.webSockets.stomp

/**
 * Enum class which represents the STOMP protocol commands.
 */
enum class StompCommand {
    SUBSCRIBE,
    UNSUBSCRIBE,
    CONNECT,
    CONNECTED,
    RECEIPT,
    SEND,
    ERROR,
    MESSAGE
}