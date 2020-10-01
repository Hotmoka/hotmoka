package io.hotmoka.network.internal.websockets.client;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.websockets.config.GsonMessageConverter;
import io.hotmoka.network.internal.websockets.config.WebSocketsConfig;
import io.hotmoka.network.models.errors.ErrorModel;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * A websockets client class to subscribe, send and receive messages from a websockets end-point.
 */
public class WebSocketClient implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);

	/**
	 * The supporting STOMP client.
	 */
	private final WebSocketStompClient stompClient;

    /**
     * The unique identifier of this client. This allows more clients to connect to the same server.
     */
    private final String clientKey;

    /**
     * The websockets end-point.
     */
    private final String url;

    /**
     * The current session.
     */
    private StompSession stompSession;

    /**
     * The websockets subscriptions open so far with this client, per topic.
     */
    private final ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    /**
     * The queues of the topics where the results are delivered.
     */
    private final ConcurrentHashMap<String, BlockingQueue<Object>> queues = new ConcurrentHashMap<>();



    /**
     * Creates an instance of a websockets client to subscribe, send and receive messages from a websockets end-point.
     * 
     * @param url the websockets end-point
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    public WebSocketClient(String url) throws ExecutionException, InterruptedException {
        this.url = url;
        this.clientKey = generateClientKey();

        // container configuration with the message size limit
        WsWebSocketContainer wsWebSocketContainer = new WsWebSocketContainer();
        wsWebSocketContainer.setDefaultMaxTextMessageBufferSize(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 8192
        wsWebSocketContainer.setDefaultMaxBinaryMessageBufferSize(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 8192

        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient(wsWebSocketContainer));
        this.stompClient.setInboundMessageSizeLimit(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 64 * 1024
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        connect();
    }


    /**
     * Subscribes and sends a request for the given topic, expecting a result of the given type and
     * bearing the given payload. The subscription is recycled.
     *
     * @param topic the topic
     * @param resultTypeClass the result class type
     * @param payload, the payload, if any
     * @return the result of the request
     */
    public <T> T subscribeAndSend(String topic, Class<T> resultTypeClass, Optional<Object> payload) throws InterruptedException {
        String resultTopic = "/user/" + clientKey + topic;
        String errorResultTopic = "/user/" + clientKey + topic + "/error";

        BlockingQueue<Object> queue = this.queues.computeIfAbsent(topic, _key -> new LinkedBlockingQueue<>());
        subscribe(resultTopic, resultTypeClass, queue);
        subscribe(errorResultTopic, ErrorModel.class, queue);

        synchronized (stompSession) {
            stompSession.send(topic, payload.orElse(null));
        }

        Object result = queue.take();
        if (result instanceof ErrorModel)
            throw new NetworkExceptionResponse((ErrorModel) result);
        else if (result instanceof GsonMessageConverter.NullObject)
            return null;
        else
            return (T) result;
    }

    /**
     * Subscribes to a topic and then handles the result published by the topic.
     * @param topic the topic destination
     * @param resultTypeClass the result type class
     * @param handler the handler of the result
     * @param <T> the result type class
     */
    public <T> void subscribeToTopic(String topic, Class<T> resultTypeClass, BiConsumer<T, ErrorModel> handler) {

        subscriptions.computeIfAbsent(topic, _topic -> {
            Subscription stompSubscription = stompSession.subscribe(topic, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return resultTypeClass;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {

                    if (payload == null)
                        handler.accept(null, new ErrorModel(new InternalFailureException("Received a null payload")));
                    else if (payload instanceof GsonMessageConverter.NullObject)
                        handler.accept(null, new ErrorModel(new InternalFailureException("Received a null object")));
                    else if (payload instanceof ErrorModel)
                        handler.accept(null, (ErrorModel) payload);
                    else if (payload.getClass() != resultTypeClass)
                        handler.accept(null, new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s]: expected [%s]" + payload.getClass().getName(), resultTypeClass))));
                    else
                        handler.accept((T) payload, null);
                }
            });

            LOGGER.info("Subscribed to " + topic);
            return stompSubscription;
        });
    }

    /**
     * Subscribes to a topic and register its queue where to deliver the result. The subscription is recycled.
     * @param topic the topic
     * @param resultTypeClass the result type
     * @param queue the queue
     * @param <T> the type of the result
     */
    private <T> void subscribe(String topic, Class<T> resultTypeClass, BlockingQueue<Object> queue) {
        subscriptions.computeIfAbsent(topic, _topic -> {
            StompSession.Subscription stompSubscription = stompSession.subscribe(topic, new FrameHandler<>(resultTypeClass, queue));

            LOGGER.info("Subscribed to " + topic);
            return stompSubscription;
        });
    }

    /**
     * Connects to the websockets end-point and creates the current session.
     * 
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private void connect() throws ExecutionException, InterruptedException {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", clientKey);
        boolean wasNull = stompSession == null;
        stompSession = stompClient.connect(url, headers, new StompClientSessionHandler(this::onSessionError)).get();
        if (!wasNull)
        	LOGGER.info("Updated STOMP session to " + stompSession.getSessionId());
    }

    private void onSessionError(Throwable throwable) {
        LOGGER.info("Got a session error: " + throwable.getMessage());
        subscriptions.values().forEach(Subscription::unsubscribe);

        try {
            // on session error, the session gets closed so we reconnect to the websocket endpoint
            subscriptions.values().forEach(Subscription::unsubscribe);
            subscriptions.clear();
            connect();
        }
        catch (ExecutionException | InterruptedException e) {
            throw InternalFailureException.of(e);
        }
    }

    @Override
    public void close() {
    	subscriptions.values().forEach(Subscription::unsubscribe);
    	subscriptions.clear();

    	if (stompSession != null)
            stompSession.disconnect();

    	stompClient.stop();
    }

    /**
     * Generates a unique key for this websockets client.
     * 
     * @return the unique key
     */
    private static String generateClientKey() {
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(salt.digest());
        }
        catch (Exception e) {
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
}