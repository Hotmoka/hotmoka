package io.hotmoka.remote.internal.websockets.client;

import io.hotmoka.remote.internal.websockets.client.stomp.StompCommand;
import io.hotmoka.remote.internal.websockets.client.stomp.StompHeaders;

/**
 * It represents a parsed STOMP message, a class which holds the STOMP command,
 * the STOMP headers and the STOMP payload.
 */
public class Message {
    private final StompCommand command;
    private final StompHeaders stompHeaders;
    private final String payload;

    public Message(StompCommand command, StompHeaders stompHeaders, String payload) {
        this.command = command;
        this.stompHeaders = stompHeaders;
        this.payload = payload;
    }

    public Message(StompCommand command, StompHeaders stompHeaders) {
        this(command, stompHeaders, null);
    }

    public String getPayload() {
        return payload;
    }

    public StompHeaders getStompHeaders() {
        return stompHeaders;
    }

    public StompCommand getCommand() {
        return command;
    }
}
