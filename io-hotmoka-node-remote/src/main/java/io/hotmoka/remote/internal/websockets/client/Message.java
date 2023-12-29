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
