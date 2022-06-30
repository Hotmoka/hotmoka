/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.remote.internal.websockets.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;
import io.hotmoka.remote.internal.websockets.client.stomp.StompCommand;
import io.hotmoka.remote.internal.websockets.client.stomp.StompMessageHelper;
import io.hotmoka.ws.client.WebSocket;
import io.hotmoka.ws.client.WebSocketAdapter;
import io.hotmoka.ws.client.WebSocketException;
import io.hotmoka.ws.client.WebSocketFactory;
import io.hotmoka.ws.client.WebSocketFrame;

/**
 * A websockets client class to subscribe, send and receive messages from a websockets end-point.
 */
public class WebSocketClient implements AutoCloseable {
    private final static Logger LOGGER = Logger.getLogger(WebSocketClient.class.getName());

    /**
     * The websockets end-point.
     */
    private final String url;

    /**
     * The unique identifier of this client. This allows more clients to connect to the same server.
     */
    private final String clientKey;

    /**
     * The websockets subscriptions open so far with this client, per topic.
     */
    private final Map<String, Subscription> internalSubscriptions;

    /**
     * The websockets queues where the results are published and consumed, per topic.
     */
    private final ConcurrentHashMap<String, BlockingQueue<Object>> queues;

    /**
     * The latch used to wait for the connection of the websocket.
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * The websocket instance.
     */
    private WebSocket webSocket;

    /**
     * Creates an instance of a websocket client to subscribe, send and receive messages from a websockets end-point.
     *
     * @param url the websockets end-point
     * @throws ExecutionException   if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    public WebSocketClient(String url) throws ExecutionException, InterruptedException, WebSocketException, IOException {
        this.url = url;
        this.clientKey = generateClientKey();
        this.internalSubscriptions = new HashMap<>();
        this.queues = new ConcurrentHashMap<>();

        connect();
    }

    private final WebSocketAdapter adapter = new WebSocketAdapter() {

		@Override
		public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
			LOGGER.info("connected to server");

			// we open the stomp session
			websocket.sendText(StompMessageHelper.buildConnectMessage());
		}

		@Override
		public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
			LOGGER.info("webSocket session closed");
		}

		public void onTextMessage(WebSocket websocket, String txtMessage) {
			LOGGER.info("received message");

			try {
				Message message = StompMessageHelper.parseStompMessage(txtMessage);
				String payload = message.getPayload();
				StompCommand command = message.getCommand();
				LOGGER.info("received " + command);

				switch (command) {

				case CONNECTED:
					LOGGER.info("connected to stomp session");
					emitClientConnected();
					break;

				case RECEIPT:
					String destination = message.getStompHeaders().getDestination();
					LOGGER.info("subscribed to topic " + destination);

					Subscription subscription;
					synchronized (internalSubscriptions) {
						subscription = internalSubscriptions.get(destination);
					}

					if (subscription == null)
						throw new NoSuchElementException("Topic not found");

					subscription.emitSubscription();
					break;

				case ERROR:
					LOGGER.warning("stomp session error: " + payload);
					// clean-up client resources because the server closed the connection
					close();
					break;

				case MESSAGE:
					destination = message.getStompHeaders().getDestination();
					handleStompDestinationResult(payload, destination);
					break;

				default:
					LOGGER.warning("unexpected stomp message " + command);
				}
			}
			catch (Exception e) {
				LOGGER.log(Level.WARNING, "exception while handling the stomp message", e);
			}
		}

		@Override
		public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
			LOGGER.log(Level.SEVERE, "webSocket session error", cause);
			close();
		}
	};

    /**
     * It opens a webSocket connection and connects to the STOMP endpoint.
     */
    private void connect() throws IOException, WebSocketException {
        LOGGER.info("connecting to " + url);

        webSocket = new WebSocketFactory()
       		.setConnectionTimeout(30 * 1000)
       		.createSocket(url)
       		.addHeader("uuid", clientKey)
       		.addListener(adapter)
       		.connect();

        awaitClientConnection();
    }

    /**
     * Emits that the client is connected.
     */
    private void emitClientConnected() {
    	latch.countDown();
    }

    /**
     * Awaits if necessary until the websocket client is connected.
     */
    private void awaitClientConnection() {
    	try {
    		latch.await();
    	}
    	catch (InterruptedException e) {
    		throw InternalFailureException.of("unexpected exception", e);
    	}
    }

    /**
     * It handles a STOMP result message of a destination.
     *
     * @param result      the result
     * @param destination the destination
     */
    private void handleStompDestinationResult(String result, String destination) {
        Subscription subscription;
        synchronized (internalSubscriptions) {
        	subscription = internalSubscriptions.get(destination);
        }

        if (subscription != null) {
            ResultHandler<?> resultHandler = subscription.getResultHandler();

            if (resultHandler.getResultTypeClass() == Void.class || result == null || result.equals("null"))
                resultHandler.deliverNothing();
            else
                resultHandler.deliverResult(result);
        }
    }

    /**
     * It sends a payload to a previous subscribed topic. The method {@link #subscribeToTopic(String, Class, BiConsumer)}}
     * is used to subscribe to a topic.
     *
     * @param topic   the topic
     * @param payload the payload
     */
    public <T> void sendToTopic(String topic, T payload) {
        LOGGER.info("sending to " + topic);
        webSocket.sendText(StompMessageHelper.buildSendMessage(topic, payload));
    }

    /**
     * It sends a payload to the "user" and "error" topic by performing an initial subscription
     * and waits for a result. The subscription is recycled.
     *
     * @param topic      the topic
     * @param resultType the result type
     */
    public <T> T subscribeAndSend(String topic, Class<T> resultType) throws InterruptedException {
        return subscribeAndSend(topic, resultType, null);
    }

    /**
     * It sends a payload to the "user" and "error" topic by performing an initial subscription
     * and waits for a result. The subscription is recycled.
     *
     * @param <T>        the type of the expected result
     * @param <P>        the type of the payload
     * @param topic      the topic
     * @param resultType the result type
     * @param payload    the payload
     */
    @SuppressWarnings("unchecked")
	public <T, P> T subscribeAndSend(String topic, Class<T> resultType, P payload) throws InterruptedException {
        LOGGER.info("subscribing to " + topic);

        String resultTopic = "/user/" + clientKey + topic;
        String errorResultTopic = resultTopic + "/error";
        Object result;

        BlockingQueue<Object> queue = queues.computeIfAbsent(topic, _key -> new LinkedBlockingQueue<>(1));
        synchronized (queue) {
            subscribe(errorResultTopic, ErrorModel.class, queue);
            subscribe(resultTopic, resultType, queue);

            LOGGER.info("sending payload to  " + topic);
            webSocket.sendText(StompMessageHelper.buildSendMessage(topic, payload));
            result = queue.take();
        }

        if (result instanceof Nothing)
            return null;
        else if (result instanceof ErrorModel)
            throw new NetworkExceptionResponse("400", (ErrorModel) result);
        else
            return (T) result;
    }

    /**
     * Subscribes to a topic providing a {@link BiConsumer} handler to handle the result published by the topic.
     *
     * @param topic      the topic destination
     * @param resultType the result type
     * @param handler    handler of the result
     * @param <T>        the result type
     */
    public <T> void subscribeToTopic(String topic, Class<T> resultType, BiConsumer<T, ErrorModel> handler) {
    	LOGGER.info("subscribing to " + topic);

    	Subscription subscription;

    	synchronized (internalSubscriptions) {
    		subscription = internalSubscriptions.computeIfAbsent(topic, _topic -> {

    			ResultHandler<T> resultHandler = new ResultHandler<>(resultType) {

    				@Override
    				public void deliverResult(String result) {
    					try {
    						handler.accept(this.toModel(result), null);
    					}
    					catch (InternalFailureException e) {
    						deliverError(new ErrorModel(e.getMessage() != null ? e.getMessage() : "deserialization error", InternalFailureException.class));
    					}
    				}

    				@Override
    				public void deliverError(ErrorModel errorModel) {
    					handler.accept(null, errorModel);
    				}

    				@Override
    				public void deliverNothing() {
    					handler.accept(null, null);
    				}
    			};

    			return subscribeInternal(topic, resultHandler);
    		});
    	}

        LOGGER.info("waiting for subscription to " + topic);
        subscription.awaitSubscription();
    }

    /**
     * Subscribes to a topic.
     *
     * @param topic      the topic
     * @param resultType the result type
     * @param queue      the queue
     * @param <T>        the result type
     */
    private <T> void subscribe(String topic, Class<T> resultType, BlockingQueue<Object> queue) {
    	LOGGER.info("subscribing to " + topic);
    	Subscription subscription;

    	synchronized (internalSubscriptions) {
    		subscription = internalSubscriptions.computeIfAbsent(topic, _topic -> subscribeInternal(topic, new ResultHandler<>(resultType) {

    			@Override
    			public void deliverResult(String result) {
    				try {
    					deliverInternal(this.toModel(result));
    				}
    				catch (Exception e) {
    					deliverError(new ErrorModel(e.getMessage() != null ? e.getMessage() : "Got a deserialization error", InternalFailureException.class));
    				}
    			}

    			@Override
    			public void deliverError(ErrorModel errorModel) {
    				deliverInternal(errorModel);
    			}

    			@Override
    			public void deliverNothing() {
    				deliverInternal(Nothing.INSTANCE);
    			}

    			private void deliverInternal(Object result) {
    				try {
    					queue.put(result);
    				}
    				catch (Exception e) {
    					LOGGER.log(Level.WARNING, "queue put error", e);
    				}
    			}
    		}));
    	}

        LOGGER.info("waiting for subscription to " + topic);
        subscription.awaitSubscription();
    }

    /**
     * Internal method to subscribe to a topic. The subscription is recycled.
     *
     * @param topic   the topic
     * @param handler the result handler of the topic
     * @return the subscription
     */
    private Subscription subscribeInternal(String topic, ResultHandler<?> handler) {
        String subscriptionId;
        
        synchronized (internalSubscriptions) {
        	subscriptionId = String.valueOf(internalSubscriptions.size() + 1);
        }

        Subscription subscription = new Subscription(topic, subscriptionId, handler);
        webSocket.sendText(StompMessageHelper.buildSubscribeMessage(subscription.getTopic(), subscription.getSubscriptionId()));

        return subscription;
    }

    /**
     * It unsubscribes from a topic.
     *
     * @param subscription the subscription
     */
    private void unsubscribeFrom(Subscription subscription) {
        LOGGER.info("unsubscribing from " + subscription.getTopic());
        webSocket.sendText(StompMessageHelper.buildUnsubscribeMessage(subscription.getSubscriptionId()));
    }


    @Override
    public void close() {
        LOGGER.info("closing session and websocket client");

        synchronized (internalSubscriptions) {
        	internalSubscriptions.values().forEach(this::unsubscribeFrom);
        	internalSubscriptions.clear();
        }

        // indicates a normal closure
        webSocket.disconnect(1000);
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
        byte[] HEX_ARRAY = "0123456789abcdef".getBytes();
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Special object to wrap a NOP.
     */
    private static class Nothing {
        private final static Nothing INSTANCE = new Nothing();

        private Nothing() {
        }
    }
}