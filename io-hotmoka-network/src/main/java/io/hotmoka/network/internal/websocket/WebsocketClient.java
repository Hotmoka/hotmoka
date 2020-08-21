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
import java.util.concurrent.*;
import java.util.function.Consumer;


/**
 * A websocket client class to send and subscribe to messages
 */
public class WebsocketClient implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebsocketClient.class);
    private final ConcurrentMap<String, Subscription<?>> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StompSession.Subscription> endpointsSubscription = new ConcurrentHashMap<>();
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

    public <T> Subscription<T> subscribe(String to, Class<T> clazz) {

        if (this.subscriptions.containsKey(to)) {
            return (Subscription<T>) this.subscriptions.get(to);
        }
        else {
            Subscription<T> subscription = new Subscription<>(clazz);
            this.subscriptions.put(to, subscription);
            this.endpointsSubscription.put(to, this.stompSession.subscribe(to, subscription));
            LOGGER.info("Subscribed to " + to);
            return subscription;
        }
    }

    public <T> void subscribe(String to, Class<T> clazz, Consumer<T> consumer) {
        this.endpointsSubscription.putIfAbsent(to, this.stompSession.subscribe(to, new Subscription<>(clazz, consumer)));
        LOGGER.info("Subscribed to " + to);
    }

    public void send(String to, Object payload) {
        this.stompSession.send(to, payload);
    }

    public void send(String to) {
        this.stompSession.send(to, null);
    }

    public void unsubscribeAll() {
        this.endpointsSubscription.forEach((key, subscription) -> subscription.unsubscribe());
        this.endpointsSubscription.clear();
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

    public String getClientKey() {
        return this.clientKey;
    }

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


    public static class Subscription<T> implements StompFrameHandler {
        private final BlockingQueue<T> queue = new LinkedBlockingQueue<>(); // TODO
        private final Class<T> responseTypeClass;
        private final Consumer<T> consumer;

        public Subscription(Class<T> responseTypeClass) {
            this(responseTypeClass, null);
        }

        public Subscription(Class<T> clazz, Consumer<T> consumer) {
            this.responseTypeClass = clazz;
            this.consumer = consumer;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return responseTypeClass;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (payload != null && payload.getClass() == responseTypeClass) {

                if (consumer != null)
                    consumer.accept((T) payload);
                else
                    queue.add((T) payload);
            }
        }

        /**
         * Waits if necessary for this task to complete, and then returns its result.
         *
         * @return the result value
         * @throws Exception if the task failed to complete
         */
        public T get() throws Exception {
            return queue.take();
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
