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

package io.hotmoka.node.service.internal;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.network.requests.EventRequestModel;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.node.messages.api.GetNodeInfoMessage;
import io.hotmoka.node.service.api.NodeService;
import io.hotmoka.node.service.api.NodeServiceConfig;
import io.hotmoka.node.service.internal.websockets.WebSocketsEventController;
import io.hotmoka.websockets.beans.ExceptionMessages;
import io.hotmoka.websockets.server.AbstractServerEndpoint;
import io.hotmoka.websockets.server.AbstractWebSocketServer;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.node.api.Node}.
 */
public class NodeServiceImpl extends AbstractWebSocketServer implements NodeService {
	private final static Logger LOGGER = Logger.getLogger(NodeServiceImpl.class.getName());

	/**
	 * The node for which the service is created.
	 */
	private final Node node;

	private final ConfigurableApplicationContext context;

	/**
	 * The subscription to the events generated by the exposed node.
	 */
	private final Subscription eventSubscription;

	/**
	 * True if and only if this service has been closed already.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * The prefix used in the log messages;
	 */
	private final String logPrefix;

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

	/**
	 * Yields an implementation of a network service that exposes an API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @throws DeploymentException if the service cannot be deployed
	 * @throws IOException if an I/O error occurs
	 */
    public NodeServiceImpl(NodeServiceConfig config, Node node) throws DeploymentException, IOException {
    	this.node = node;

    	// we disable Spring's logging otherwise it will interfere with Hotmoka's logging
		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");

		this.logPrefix = "node service(ws://localhost:" + config.getPort() + "): ";
		this.context = SpringApplication.run(Application.class, springArgumentsFor(config));
    	this.context.getBean(Application.class).setNode(node);
    	this.eventSubscription = node.subscribeToEvents(null, this::publishEvent);

    	// TODO: remove the +2 at the end
    	startContainer("", config.getPort() + 2,
   			GetNodeInfoEndpoint.config(this)
   		);

    	// if the node gets closed, then this service will be closed as well
    	node.addOnCloseHandler(this_close);

    	LOGGER.info(logPrefix + "published");
    }

    @Override
    public void close() {
    	if (!isClosed.getAndSet(true)) {
    		node.removeOnCloseHandler(this_close);
    		stopContainer();
    		SpringApplication.exit(context);
        	eventSubscription.close();
			LOGGER.info(logPrefix + "closed");
		}
    }

    /**
	 * Sends an exception message to the given session.
	 * 
	 * @param session the session
	 * @param e the exception used to build the message
	 * @param id the identifier of the message to send
	 * @throws IOException if there was an I/O problem
	 */
	private void sendExceptionAsync(Session session, Exception e, String id) throws IOException {
		sendObjectAsync(session, ExceptionMessages.of(e, id));
	}

	protected void onGetNodeInfo(GetNodeInfoMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_NODE_INFO_ENDPOINT + " request");

		try {
			try {
				sendObjectAsync(session, GetNodeInfoResultMessages.of(node.getNodeInfo(), message.getId()));
			}
			catch (TimeoutException | InterruptedException | NodeException e) {
				sendExceptionAsync(session, e, message.getId());
			}
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	public static class GetNodeInfoEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			addMessageHandler(session, (GetNodeInfoMessage message) -> getServer().onGetNodeInfo(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetNodeInfoEndpoint.class, GET_NODE_INFO_ENDPOINT,
				GetNodeInfoMessages.Decoder.class, GetNodeInfoResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	/**
     * Builds, from the configuration, the array of arguments required by Spring in order to start the application.
     * 
     * @param config the configuration
     * @return the array of arguments required by Spring
     */
    private static String[] springArgumentsFor(NodeServiceConfig config) {
    	return new String[] {
   			"--server.port=" + config.getPort(),
   			"--spring.main.banner-mode=" + Banner.Mode.OFF
    	};
    }

    private void publishEvent(StorageReference creator, StorageReference event) {
		WebSocketsEventController controller = this.context.getBean(WebSocketsEventController.class);
		controller.addEvent(new EventRequestModel(creator, event));
    }
}