package io.hotmoka.network.internal.websocket;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.websocket.config.GsonMessageConverter;
import io.hotmoka.network.internal.websocket.config.WebSocketConfig;
import io.hotmoka.network.models.errors.ErrorModel;


/**
 * A websocket client class to subscribe, send and receive messages from websocket endpoints.
 */
public class WebsocketClient implements AutoCloseable {
    private final WebSocketStompClient stompClient;
    private final String clientKey;
    private final String url;

    /**
     * The current session
     */
    private StompSession stompSession;

    private Subscription subscription;
    //private Subscription errorSubscription;

    private final static Logger LOGGER = LoggerFactory.getLogger(WebsocketClient.class);

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
     * @param topic the topic destination
     * @param clazz the response class type
     * @return {@link SubscriptionTask}
     * @throws InterruptedException 
     * @throws ExecutionException 
     */
    public <T> T subscribeAndSend(String topic, Class<T> clazz, Optional<Object> payload) throws ExecutionException, InterruptedException {
    	Subscription<T> subscription = new Subscription<>("/user/" + clientKey + topic, clazz);
    	this.subscription = subscription;
    	stompSession.send(topic, payload.orElse(null));

    	CompletableFuture.anyOf(subscription.result, subscription.error).get();

    	subscription.unsubscribe();
        if (subscription.error.isDone())
            throw new NetworkExceptionResponse(subscription.error.get());
        else
            return subscription.result.get();
    }

    /**
     * It connects to the websocket endpoint and it creates the current session.
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private void connect() throws ExecutionException, InterruptedException {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", clientKey);

        stompSession = stompClient.connect(url, headers, new StompClientSessionHandler()).get();
    }

    /**
     * It reconnects to the websocket endpoint.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private void reconnect() throws ExecutionException, InterruptedException {
    	subscription.unsubscribe();
        connect();
    }

    @Override
    public void close() {
    	if (subscription != null)
    		subscription.unsubscribe();

    	if (stompSession != null)
    		stompSession.disconnect();

    	stompClient.stop();
    }

    /**
     * Generates a unique key for this websocket client.
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

    /**
     * Subscription task to subscribe to a websocket topic and to get the future result of the subscription.
     */
    private class Subscription<T> implements StompFrameHandler {
        private final Class<T> responseTypeClass;
        private final StompSession.Subscription normalSubscription;
        private final StompSession.Subscription errorSubscription;
        private final String topic;

        /**
         * The result.
         */
        private final CompletableFuture<T> result = new CompletableFuture<>();
        private final CompletableFuture<ErrorModel> error = new CompletableFuture<>();

        private Subscription(String topic, Class<T> resultTypeClass) {
            this.topic = topic;
            String errorTopic = topic + "/error";
            this.responseTypeClass = resultTypeClass;
            this.normalSubscription = stompSession.subscribe(topic, this);
            StompFrameHandler errorHandler = new StompFrameHandler() {

				@Override
				public Type getPayloadType(StompHeaders headers) {
					return ErrorModel.class;
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					if (payload == null)
		            	error.complete(new ErrorModel(new InternalFailureException("Received null payload")));
					else if (payload instanceof ErrorModel)
						error.complete((ErrorModel) payload);
					else
						error.complete(new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s] - Expected payload type [%s]" + payload.getClass(), ErrorModel.class))));
				}
            };
            this.errorSubscription = stompSession.subscribe(errorTopic, errorHandler);

            LOGGER.info("Subscribed to " + topic);
            LOGGER.info("Subscribed to " + topic + "/error");
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return responseTypeClass;
        }

        @SuppressWarnings("unchecked")
		@Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (payload == null)
            	error.complete(new ErrorModel(new InternalFailureException("Received null payload")));
            else if (payload.getClass() == GsonMessageConverter.NullObject.class)
                result.complete(null);
            else if (payload.getClass() != responseTypeClass)
            	error.complete(new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s] - Expected payload type [%s]" + payload.getClass(), responseTypeClass))));
            else
                result.complete((T) payload);
        }

        /**
         * It notifies when a generic websocket exception or a transport websocket exception
         * is thrown by the server.
         */
        private void notifyError() {
            if (!error.isDone() && !result.isDone())
                error.complete(new ErrorModel(new InternalFailureException("Unexpected error")));
        }

        /**
         * Unsubscribe from a websocket subscription.
         */
        private void unsubscribe() {
        	normalSubscription.unsubscribe();
        	errorSubscription.unsubscribe();
            LOGGER.info("Unsubscribed from " + topic);
            LOGGER.info("Unsubscribed from " + topic + "/error");
        }
    }

    /**
     * Client session handler to handle the lifecycle of a STOMP session.
     */
    private class StompClientSessionHandler implements StompSessionHandler {

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            LOGGER.info("New session established: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            LOGGER.error("STOMP Session exception", exception);
            onError();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            LOGGER.error("STOMP Session Transport Error", exception);
            onError();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {}

        private void onError() {
        	subscription.notifyError();

            try {
                // on session error, the session gets closed so we reconnect to the websocket endpoint
                reconnect();
            }
            catch (ExecutionException | InterruptedException e) {
                throw InternalFailureException.of(e);
            }
        }
    }
}