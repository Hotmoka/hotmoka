package io.hotmoka.network.internal.websocket;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.websocket.config.GsonMessageConverter;
import io.hotmoka.network.internal.websocket.config.WebSocketConfig;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * A websocket client class to subscribe, send and receive messages from websocket endpoints.
 */
public class WebsocketClient implements AutoCloseable {
    private final Map<String, Subscription<?>> subscriptions = new HashMap<>();
    private final WebSocketStompClient stompClient;
    private final String clientKey;
    private final String url;

    /**
     * The current session
     */
    private StompSession stompSession;


    /**
     * It creates the instance of a websocket client to subscribe, send and receive messages from a websocket endpoint.
     * @param url the websocket endpoint
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    public WebsocketClient(String url) throws ExecutionException, InterruptedException {
        this.url = url;
        this.clientKey = generateClientKey();

        // container configuration with the message size limit
        WsWebSocketContainer wsWebSocketContainer = new WsWebSocketContainer();
        wsWebSocketContainer.setDefaultMaxTextMessageBufferSize(WebSocketConfig.MESSAGE_SIZE_LIMIT); // default 8192
        wsWebSocketContainer.setDefaultMaxBinaryMessageBufferSize(WebSocketConfig.MESSAGE_SIZE_LIMIT); // default 8192

        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient(wsWebSocketContainer));
        this.stompClient.setInboundMessageSizeLimit(WebSocketConfig.MESSAGE_SIZE_LIMIT); // default 64 * 1024
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        connect();
    }


    /**
     * Yields a {@link Subscription} by subscribing to a topic and to its error topic. <br>
     * e.g. topic /topic/getTime and its error topic /topic/getTime/error
     *
     * @param to the topic destination
     * @param clazz the response class type
     * @return {@link SubscriptionTask}
     */
    public Subscription<?> subscribe(String to, Class<?> clazz) {
       Subscription<?> subscription = new SubscriptionImpl(
               new SubscriptionTask("/user/" + this.clientKey + to, clazz, this.stompSession),
               new SubscriptionTask( "/user/" + this.clientKey + to + "/error", ErrorModel.class, this.stompSession)
       );

       this.subscriptions.put(to, subscription);
       return subscription;
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

    /**
     * It disconnects from a websocket endpoint, and clears the session and the  subscriptions.
     */
    public void disconnect() {
        this.stompSession.disconnect();
        this.stompClient.stop();
        this.subscriptions.clear();
    }

    /**
     * It connects to the websocket endpoint and it creates the current session.
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private void connect() throws ExecutionException, InterruptedException {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", this.clientKey);

        this.stompSession = this.stompClient.connect(this.url, headers, new StompClientSessionHandler(() -> {
            this.subscriptions.values().forEach(Subscription::notifyError);

            try {
                // on session error, the session gets closed so we reconnect to the websocket endpoint
                reconnect();
            }
            catch (ExecutionException | InterruptedException e) {
                throw InternalFailureException.of(e);
            }
        })).get();
    }

    /**
     * It reconnects to the websocket endpoint.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    public void reconnect() throws ExecutionException, InterruptedException {
        this.subscriptions.clear();
        connect();
    }

    /**
     * Yields the current session id.
     * @return the session id
     */
    public String getSessionId() {
        if (this.stompSession != null)
            return this.stompSession.getSessionId();

        return null;
    }

    @Override
    public void close() {
        disconnect();
    }

    /**
     * Yields the unique client key.
     * @return the client key
     */
    public String getClientKey() {
        return this.clientKey;
    }

    /**
     * Generates a unique key for this websocket client.
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
     * It defines methods to manage a subscription to a websocket topic.
     */
    public interface Subscription<T> extends AutoCloseable {

        /**
         * Yields the result of the subscription.
         * @return the result
         * @throws CancellationException if this future was cancelled
         * @throws ExecutionException if this future completed exceptionally
         * @throws InterruptedException if the current thread was interrupted
         * @throws NetworkExceptionResponse if the websocket endpoint threw an exception
         */
        T get() throws ExecutionException, InterruptedException;

        /**
         * It notifies when a generic websocket exception or a transport websocket exception
         * is thrown by the server.
         */
        void notifyError();

        /**
         * Used to unsubscribe from a websocket subscription.
         */
        @Override
        void close();
    }

    /**
     * A subscription to a websocket topic and to its error topic. <br>
     * e.g. topic /topic/getTime and its error topic /topic/getTime/error
     */
    private static class SubscriptionImpl implements Subscription<Object> {
        private final SubscriptionTask subscriptionTask;
        private final SubscriptionTask subscriptionErrorTask;

        public SubscriptionImpl(SubscriptionTask subscriptionTask, SubscriptionTask subscriptionErrorTask) {
            this.subscriptionTask = subscriptionTask;
            this.subscriptionErrorTask = subscriptionErrorTask;
        }

        /**
         * Yields the result of this subscription.
         * @return the result
         * @throws CancellationException if this future was cancelled
         * @throws ExecutionException if this future completed exceptionally
         * @throws InterruptedException if the current thread was interrupted
         * @throws NetworkExceptionResponse if the websocket endpoint threw an exception
         */
        @Override
        public Object get() throws ExecutionException, InterruptedException {
            Object response = CompletableFuture.anyOf(this.subscriptionTask.get(), this.subscriptionErrorTask.get()).get();

            if (response instanceof ErrorModel)
                throw new NetworkExceptionResponse((ErrorModel) response);
            else
                return response;
        }

        /**
         * It notifies when a generic websocket exception or a transport websocket exception
         * is thrown by the server.
         */
        @Override
        public void notifyError() {
            this.subscriptionErrorTask.notifyError();
        }

        /**
         * Used to unsubscribe from a websocket subscription.
         */
        @Override
        public void close() {
            this.subscriptionTask.close();
            this.subscriptionErrorTask.close();
        }
    }


    /**
     * Subscription task to subscribe to a websocket topic and to get the future result of the subscription.
     */
    private static class SubscriptionTask implements Subscription<CompletableFuture<Object>>, StompFrameHandler {
        private final static Logger LOGGER = LoggerFactory.getLogger(SubscriptionTask.class);
        private final Class<?> responseTypeClass;
        private final CompletableFuture<Object> completableFuture;
        private final StompSession.Subscription stompSubscription;
        private final String destination;

        public SubscriptionTask(String to, Class<?> resultTypeClass, StompSession stompSession) {
            this.destination = to;
            this.responseTypeClass = resultTypeClass;
            this.completableFuture = new CompletableFuture<>();
            this.stompSubscription = stompSession.subscribe(to, this);

            LOGGER.info("Subscribed to " + destination);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return this.responseTypeClass;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {

            if (payload == null)
                this.completableFuture.complete(new ErrorModel(new InternalFailureException("Received null payload")));
            else if (payload.getClass() == GsonMessageConverter.NullObject.class)
                this.completableFuture.complete(null);
            else if (payload.getClass() != this.responseTypeClass)
                this.completableFuture.complete(new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s] - Expected payload type [%s]" + payload.getClass(), this.responseTypeClass))));
            else
                this.completableFuture.complete(payload);
        }

        public CompletableFuture<Object> get() {
            return this.completableFuture;
        }

        @Override
        public void notifyError() {
            if (!this.completableFuture.isDone()) {
                this.completableFuture.complete(new ErrorModel(new InternalFailureException("Unexpected error")));
            }
        }

        @Override
        public void close() {
            this.stompSubscription.unsubscribe();
            LOGGER.info("Unsubscribed from " + this.destination);
        }
    }

    private interface WebsocketErrorCallback {
        void onError();
    }

    /**
     * Client session handler to handle the lifecycle of a STOMP session.
     */
    private static class StompClientSessionHandler implements StompSessionHandler {
        private final static Logger LOGGER = LoggerFactory.getLogger(StompClientSessionHandler.class);
        private final WebsocketErrorCallback errorCallback;

        public StompClientSessionHandler(WebsocketErrorCallback errorCallback) {
            this.errorCallback = errorCallback;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            LOGGER.info("New session established: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            LOGGER.error("STOMP Session exception", exception);
            this.errorCallback.onError();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            LOGGER.error("STOMP Session Transport Error", exception);
            this.errorCallback.onError();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {

        }
    }

}
