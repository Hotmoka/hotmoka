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
import java.util.UUID;
import java.util.concurrent.*;


/**
 * A websocket client class to subscribe, receive and send messages to websocket endpoints.
 */
public class WebsocketClient implements AutoCloseable {
    private final ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final WebSocketStompClient stompClient;
    private final String clientKey;
    private StompSession stompSession;

    public WebsocketClient(String url) throws ExecutionException, InterruptedException {
        this.clientKey = generateClientKey();

        // container configuration with the message size limit
        WsWebSocketContainer wsWebSocketContainer = new WsWebSocketContainer();
        wsWebSocketContainer.setDefaultMaxTextMessageBufferSize(WebSocketConfig.MESSAGE_SIZE_LIMIT); // default 8192
        wsWebSocketContainer.setDefaultMaxBinaryMessageBufferSize(WebSocketConfig.MESSAGE_SIZE_LIMIT); // default 8192

        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient(wsWebSocketContainer));
        this.stompClient.setInboundMessageSizeLimit(WebSocketConfig.MESSAGE_SIZE_LIMIT); // default 64 * 1024
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        connect(url);
    }


    /**
     * Yields a {@link Subscription} by subscribing to a topic and to its error topic. <br>
     * e.g. topic /topic/getTime and its error topic /topic/getTime/error
     *
     * @param to the topic destination
     * @param clazz the response class type
     * @return {@link SubscriptionTask}
     */
    public Subscription subscribe(String to, Class<?> clazz) {
        this.subscriptions.computeIfAbsent(to, s -> new SubscriptionImpl(to, clazz, this.clientKey, this.stompSession));
        return subscriptions.get(to);
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
     * It disconnects from a websocket session and clears the subscriptions.
     */
    public void disconnect() {
        this.stompSession.disconnect();
        this.stompClient.stop();
        this.subscriptions.values().forEach(SubscriptionTask::unsubscribe);
    }

    /**
     * It connects to a websocket server and it creates a session.
     * @param url the url of the websocket server
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private void connect(String url) throws ExecutionException, InterruptedException {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", this.clientKey);

        this.stompSession = this.stompClient.connect(url, headers, new StompClientSessionHandler(() -> {
           /*
            this.subscriptions.values().forEach(Subscription::notifyError);
            this.subscriptions.clear(); */
            /* TODO: try to reconnect
            try {
                // on session error the session gets closed so we reconnect to the websocket endpoint
                connect(url);
            }
            catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }*/
        })).get();
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


    private static class SubscriptionImpl implements Subscription {
        private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        private final SubscriptionTask subscriptionTask;
        private final SubscriptionTask subscriptionErrorTask;

        public SubscriptionImpl(String to, Class<?> responseClassType, String clientKey, StompSession stompSession) {
            this.subscriptionTask = new SubscriptionTaskImpl("/user/" + clientKey + to, responseClassType, stompSession, queue);
            this.subscriptionErrorTask = new SubscriptionTaskImpl("/user/" + clientKey + to + "/error", ErrorModel.class, stompSession, queue);
        }

        /**
         * Yields the result of this subscription.
         * @return the result
         * @throws InterruptedException if the current thread was interrupted
         * @throws NetworkExceptionResponse if the websocket endpoint threw an exception
         */
        @Override
        public Object get() throws InterruptedException {
            Object response = this.queue.take();

            if (response instanceof ErrorModel)
                throw new NetworkExceptionResponse((ErrorModel) response);
            else if (response instanceof GsonMessageConverter.NullObject)
                return null;
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
        public void unsubscribe() {
            this.subscriptionTask.unsubscribe();
            this.subscriptionErrorTask.unsubscribe();
        }
    }

    /**
     * A subscription to a websocket topic and to its error topic. <br>
     * e.g. topic /topic/getTime and its error topic /topic/getTime/error
     */
    public interface Subscription extends SubscriptionTask {

        /**
         * Yields the result of the subscription.
         * @return the result
         * @throws InterruptedException if the current thread was interrupted
         * @throws NetworkExceptionResponse if the websocket endpoint threw an exception
         */
        Object get() throws ExecutionException, InterruptedException;

    }

    /**
     * Subscription task to subscribe to a websocket topic and to get the future result of the subscription.
     */
    private interface SubscriptionTask {

        /**
         * It notifies when a generic websocket exception or a transport websocket exception
         * is thrown by the server.
         */
        void notifyError();

        /**
         * Used to unsubscribe from a websocket subscription.
         */
        void unsubscribe();
    }

    private static class SubscriptionTaskImpl implements SubscriptionTask, StompFrameHandler {
        private final static Logger LOGGER = LoggerFactory.getLogger(SubscriptionTaskImpl.class);
        private final Class<?> responseTypeClass;
        private final BlockingQueue<Object> queue;
        private final StompSession.Subscription stompSubscription;
        private final String destination;

        public SubscriptionTaskImpl(String to, Class<?> resultTypeClass, StompSession stompSession, BlockingQueue<Object> queue) {
            this.destination = to;
            this.responseTypeClass = resultTypeClass;
            this.queue = queue;
            this.stompSubscription = stompSession.subscribe(to, this);

            LOGGER.info("Subscribed to " + destination);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return this.responseTypeClass;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {

            if (payload == null) {
                this.queue.add(new ErrorModel(new InternalFailureException("Received null payload")));
            }
            else if (payload.getClass() == GsonMessageConverter.NullObject.class) {
                this.queue.add(new GsonMessageConverter.NullObject());
            }
            else if (payload.getClass() != this.responseTypeClass) {
                this.queue.add(new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s] - Expected payload type [%s]" + payload.getClass(), this.responseTypeClass))));
            }
            else {
                this.queue.add(payload);
            }
        }

        @Override
        public void notifyError() {
            this.queue.add(new ErrorModel(new InternalFailureException("Unexpected error")));
        }

        @Override
        public void unsubscribe() {
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
