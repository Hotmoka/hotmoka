package io.hotmoka.network.internal.websocket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WsEvents {
    private final static Logger LOGGER = LoggerFactory.getLogger(WsEvents.class);

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        String username = event.getUser() != null ? event.getUser().getName() : "unknown";
        LOGGER.info("Client " + username + " connected");
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        String username = event.getUser() != null ? event.getUser().getName() : "unknown";
        String reason = event.getCloseStatus().getReason() != null ? " with reason " + event.getCloseStatus().getReason() : "";

        LOGGER.info("Client " + username + " disconnected" + reason);
    }
}
