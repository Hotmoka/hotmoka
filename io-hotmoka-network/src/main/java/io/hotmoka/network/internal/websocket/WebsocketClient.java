package io.hotmoka.network.internal.websocket;

import io.hotmoka.network.internal.websocket.config.GsonMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * A websocket client class to send and subscribe to messages
 */
public class WebsocketClient implements AutoCloseable {
    private final StompSession stompSession;
    private final WebSocketStompClient stompClient;
    private final String clientKey;

    public WebsocketClient(String url) throws ExecutionException, InterruptedException {
        this.clientKey = generateClientKey();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", this.clientKey);
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        this.stompSession = stompClient.connect(url, headers, new StompClientSessionHandler()).get();
    }

    /**
     * Yields a {@link io.hotmoka.network.internal.websocket.WebsocketClient.SubscriptionTask} by subscribing to a topic
     * @param to the topic destination
     * @param clazz the response class type
     * @return {@link io.hotmoka.network.internal.websocket.WebsocketClient.SubscriptionTask}
     */
    public <T> SubscriptionTask<T> subscribe(String to, Class<T> clazz) {
        return new SubscriptionTask<>(to, new SubscriptionHandler<>(clazz), this.stompSession);
    }

    /**
     * Method to send the message data to a websocket endpoint
     * @param to the destination
     * @param payload the payload
     */
    public void send(String to, Object payload) {
        this.stompSession.send(to, payload);
    }

    /**
     * Method to send an empty message to a websocket endpoint
     * @param to the destination
     */
    public void send(String to) {
        this.stompSession.send(to, null);
    }

    public void disconnect() {
        this.stompSession.disconnect();
        this.stompClient.stop();
    }

    @Override
    public void close() {
        disconnect();
    }

    public String getClientKey() {
        return this.clientKey;
    }

    /**
     * Generates a unique key for this websocket client
     * @return a unique key
     */
    private static String generateClientKey() {
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(salt.digest());
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        byte [] HEX_ARRAY = "0123456789abcdef".getBytes();
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Task class to subscribe to a websocket endpoint and to wait and get the asynchronous result
     */
    public static class SubscriptionTask<T> implements AutoCloseable {
        private final static Logger LOGGER = LoggerFactory.getLogger(SubscriptionTask.class);
        private final SubscriptionHandler<T> subscription;
        private final StompSession.Subscription stompSubscription;

        public SubscriptionTask(String to, SubscriptionHandler<T> subscriptionHandler, StompSession stompSession) {
            this.subscription = subscriptionHandler;
            this.stompSubscription = stompSession.subscribe(to, subscriptionHandler);
            LOGGER.info("Subscribed to " + to);
        }

        /**
         * Waits if necessary for this task to complete, and then returns its result.
         *
         * @return the result value
         * @throws CancellationException if this future was cancelled
         * @throws ExecutionException if this future completed exceptionally
         * @throws InterruptedException if the current thread was interrupted
         */
        public T get() throws ExecutionException, InterruptedException {
            return this.subscription.get();
        }

        @Override
        public void close() throws Exception {
            this.stompSubscription.unsubscribe();
        }
    }

    /**
     * Handler class which delivers the result of the subscription
     */
    private static class SubscriptionHandler<T> implements StompFrameHandler {
        private final Class<T> responseTypeClass;
        private final CompletableFuture<T> completableFuture;

        public SubscriptionHandler(Class<T> clazz) {
            this.responseTypeClass = clazz;
            this.completableFuture = new CompletableFuture<>();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return responseTypeClass;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (payload != null && payload.getClass() == responseTypeClass) {
                completableFuture.complete((T) payload);
            }
        }

        /**
         * Waits if necessary for this task to complete, and then returns its result.
         *
         * @return the result value
         * @throws CancellationException if this future was cancelled
         * @throws ExecutionException if this future completed exceptionally
         * @throws InterruptedException if the current thread was interrupted
         */
        public T get() throws ExecutionException, InterruptedException {
            return completableFuture.get();
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
