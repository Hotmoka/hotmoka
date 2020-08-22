package io.hotmoka.network.internal.websocket;

import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.websocket.config.GsonMessageConverter;
import io.hotmoka.network.models.errors.ErrorModel;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * A websocket client class to subscribe, receive and send messages to websocket endpoints.
 */
public class WebsocketClient implements AutoCloseable {
    private final StompSession stompSession;
    private final WebSocketStompClient stompClient;
    private final String clientKey;

    public WebsocketClient(String url) throws ExecutionException, InterruptedException {
        this.clientKey = generateClientKey();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", this.clientKey);

        // container configuration with the message size limit
        WsWebSocketContainer wsWebSocketContainer = new WsWebSocketContainer();
        wsWebSocketContainer.setDefaultMaxTextMessageBufferSize(20*1024*1024);
        wsWebSocketContainer.setDefaultMaxTextMessageBufferSize(20*1024*1024);

        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient(wsWebSocketContainer));
        this.stompClient.setInboundMessageSizeLimit(20*1024*1024);
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        this.stompSession = stompClient.connect(url, headers, new StompClientSessionHandler()).get();
    }

    /**
     * Yields a {@link Subscription} by subscribing to a topic.
     * e.g websocket endpoint /get/takamakaCode with its topic /topic/get/takamakaCode
     *
     * @param to the topic destination
     * @param clazz the response class type
     * @return {@link Subscription}
     */
    public <T> Subscription subscribe(String to, Class<T> clazz) {
        return new Subscription(new SubscriptionTaskImpl<>(to, clazz, this.stompSession));
    }

    /**
     * Yields a {@link Subscription} by subscribing to a topic and combining the subscription with its error topic.
     * e.g websocket endpoint /get/takamakaCode with its topic /topic/get/takamakaCode and its error topic /user/clientKey/get/takamakaCode/error
     *
     * @param to the topic destination
     * @param clazz the response class type
     * @return {@link SubscriptionTask}
     */
    public <T> Subscription subscribeWithErrorHandler(String to, Class<T> clazz) {
       return new Subscription(
               new SubscriptionTaskImpl<>(to, clazz, this.stompSession),
               new SubscriptionTaskImpl<>( "/user/" + clientKey + to.replace("topic/", "") + "/error", ErrorModel.class, this.stompSession)
       );
    }

    /**
     * Method to send the message data to a websocket endpoint
     * e.g websocket endpoint /get/takamakaCode
     *
     * @param to the destination
     * @param payload the payload
     */
    public void send(String to, Object payload) {
        this.stompSession.send(to, payload);
    }

    /**
     * Method to send an empty message to a websocket endpoint
     * e.g websocket endpoint /get/takamakaCode
     *
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
     * A subscription class to hold the subscription tasks and to fetch the final result of the asynchronous tasks execution.
     * Throws a {@link io.hotmoka.network.internal.services.NetworkExceptionResponse} if the response is
     * an instance of {@link io.hotmoka.network.models.errors.ErrorModel}.
     */
    public static class Subscription implements AutoCloseable {
        private final SubscriptionTask<?>[] subscriptionTasks;

        public Subscription(SubscriptionTask<?>... subscriptionTasks) {
            this.subscriptionTasks = subscriptionTasks;
        }

        /**
         * Yields the result of this subscription.
         * @return the result
         * @throws CancellationException if this future was cancelled
         * @throws ExecutionException if this future completed exceptionally
         * @throws InterruptedException if the current thread was interrupted
         * @throws NetworkExceptionResponse if the websocket endpoint threw an exception
         */
        public Object get() throws ExecutionException, InterruptedException {
            Object response = CompletableFuture.anyOf(Arrays.stream(this.subscriptionTasks)
                        .map(SubscriptionTask::getCompletableFuture)
                        .toArray(CompletableFuture<?>[]::new)).get();

            if (response instanceof ErrorModel)
                throw new NetworkExceptionResponse((ErrorModel) response);
            else
                return response;
        }

        @Override
        public void close() {
            Arrays.stream(this.subscriptionTasks).forEach(SubscriptionTask::close);
        }
    }

    /**
     * Task interface to get the future result of an asynchronous execution.
     */
    private interface SubscriptionTask<T> extends AutoCloseable {

        /**
         * Yields the future result of an asynchronous execution.
         * @return the future result
         */
        CompletableFuture<T> getCompletableFuture();

        @Override
        void close();
    }

    private static class SubscriptionTaskImpl<T> implements SubscriptionTask<T> {
        private final static Logger LOGGER = LoggerFactory.getLogger(SubscriptionTaskImpl.class);
        private final SubscriptionHandler<T> subscriptionHandler;
        private final StompSession.Subscription stompSubscription;
        private final String destination;

        public SubscriptionTaskImpl(String to, Class<T> resultTypeClass, StompSession stompSession) {
            this.destination = to;
            this.subscriptionHandler = new SubscriptionHandler<>(resultTypeClass);
            this.stompSubscription = stompSession.subscribe(to, subscriptionHandler);

            LOGGER.info("Subscribed to " + destination);
        }

        @Override
        public CompletableFuture<T> getCompletableFuture() {
            return this.subscriptionHandler.getCompletableFuture();
        }

        @Override
        public void close() {
            LOGGER.info("Unsubscribed from " + destination);
            this.stompSubscription.unsubscribe();
        }
    }

    /**
     * Handler class which delivers the result of the websocket subscription
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

        public CompletableFuture<T> getCompletableFuture() {
            return this.completableFuture;
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
