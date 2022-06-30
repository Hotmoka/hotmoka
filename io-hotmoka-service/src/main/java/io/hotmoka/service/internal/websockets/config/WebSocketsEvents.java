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

package io.hotmoka.service.internal.websockets.config;

import java.util.logging.Logger;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketsEvents {
    private final static Logger LOGGER = Logger.getLogger(WebSocketsEvents.class.getName());

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        String username = event.getUser() != null ? event.getUser().getName() : "unknown";
        LOGGER.info("[WsServer] Client " + username + " connected");
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        String username = event.getUser() != null ? event.getUser().getName() : "unknown";
        String reason = event.getCloseStatus().getReason() != null ? " with reason " + event.getCloseStatus().getReason() : "";
        LOGGER.info("[WsServer] Client " + username + " disconnected" + reason);
    }
}