package io.hotmoka.network.internal.websockets.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketsEvents {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebSocketsEvents.class);

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