package io.hotmoka.network.internal.websocket;

import io.hotmoka.network.internal.websocket.config.GsonMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A websocket client class to send and subscribe to messages
 */
public class WebsocketClient implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebsocketClient.class);
    private final ConcurrentMap<String, StompSession.Subscription> subscriptions = new ConcurrentHashMap<>();
    private final StompSession stompSession;
    public final WebSocketStompClient stompClient;

    public WebsocketClient(String url) throws ExecutionException, InterruptedException {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        this.stompSession = stompClient.connect(url, new StompClientSessionHandler()).get();
    }

    public void subscribe(String to, SubscriptionResponseHandler<?> subscriptionResponseHandler) {
        this.subscriptions.putIfAbsent(to, this.stompSession.subscribe(to, subscriptionResponseHandler));
        LOGGER.info("Subscribed to " + to);
    }

    public void send(String to, Object payload) {
        this.stompSession.send(to, payload);
    }

    public void send(String to) {
        this.stompSession.send(to, null);
    }

    public void unsubscribeAll() {
        this.subscriptions.forEach((key, subscription) -> subscription.unsubscribe());
        this.subscriptions.clear();
    }

    public void disconnect() {
        this.stompSession.disconnect();
        this.stompClient.stop();
    }

    @Override
    public void close() {
        unsubscribeAll();
        disconnect();
    }


    public static class SubscriptionResponseHandler<T> implements StompFrameHandler {
        private final Class<T> tClass;
        private final Consumer<T> consumer;

        public SubscriptionResponseHandler(Class<T> tClass, Consumer<T> consumer) {
            this.tClass = tClass;
            this.consumer = consumer;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return tClass;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (payload != null && payload.getClass() == tClass)
               consumer.accept((T) payload);
        }
    }

    private static class StompClientSessionHandler implements StompSessionHandler {
        private final static Logger LOGGER = LoggerFactory.getLogger(StompClientSessionHandler.class);

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            LOGGER.info("New session established: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
              LOGGER.error("Got an exception", exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            LOGGER.error("Got a transportError", exception);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return null;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {

        }
    }
}
