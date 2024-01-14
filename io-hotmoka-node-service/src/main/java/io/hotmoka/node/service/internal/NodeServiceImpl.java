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

import java.util.logging.Logger;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.network.requests.EventRequestModel;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.service.api.NodeService;
import io.hotmoka.node.service.api.NodeServiceConfig;
import io.hotmoka.node.service.internal.websockets.WebSocketsEventController;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.node.api.Node}.
 */
public class NodeServiceImpl implements NodeService {
	private final static Logger LOGGER = Logger.getLogger(NodeServiceImpl.class.getName());
	private final ConfigurableApplicationContext context;

	/**
	 * The subscription to the events generated by the exposed node.
	 */
	private final Subscription eventSubscription;

	/**
	 * Yields an implementation of a network service that exposes an API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 */
    public NodeServiceImpl(NodeServiceConfig config, Node node) {
		// we disable Spring's logging otherwise it will interfere with Hotmoka's logging
		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
    	this.context = SpringApplication.run(Application.class, springArgumentsFor(config));
    	this.context.getBean(Application.class).setNode(node);
    	this.eventSubscription = node.subscribeToEvents(null, this::publishEvent);
        LOGGER.info("Network server for Hotmoka node started");
    }

    @Override
    public void close() {
    	SpringApplication.exit(context);
    	eventSubscription.close();
    	LOGGER.info("Network server for Hotmoka node closed");
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