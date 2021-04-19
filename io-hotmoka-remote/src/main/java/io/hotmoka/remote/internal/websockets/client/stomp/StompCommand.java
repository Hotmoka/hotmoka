package io.hotmoka.remote.internal.websockets.client.stomp;

/**
 * Enum class which represents the STOMP protocol commands.
 */
public enum StompCommand {
    SUBSCRIBE,
    UNSUBSCRIBE,
    CONNECT,
    CONNECTED,
    RECEIPT,
    SEND,
    ERROR,
    MESSAGE
}
