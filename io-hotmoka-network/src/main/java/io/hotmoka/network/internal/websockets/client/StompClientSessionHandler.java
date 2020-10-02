package io.hotmoka.network.internal.websockets.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * Class to handle the lifecycle of a STOMP session.
 */
class StompClientSessionHandler implements StompSessionHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(StompClientSessionHandler.class);
    private final Consumer<Throwable> errorHandler;


    public StompClientSessionHandler(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        LOGGER.info("[WsClient] New session established: " + session.getSessionId());
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        LOGGER.error("[WsClient] STOMP session " + session.getSessionId()+ " exception", exception);
        errorHandler.accept(exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        LOGGER.error("[WsClient] STOMP session " + session.getSessionId() + " transport error", exception);
        errorHandler.accept(exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return String.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {}
}
