/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.node.service;

import io.hotmoka.node.api.Node;
import io.hotmoka.node.service.api.NodeService;
import io.hotmoka.node.service.internal.NodeServiceImpl;
import io.hotmoka.websockets.api.FailedDeploymentException;

/**
 * Providers of services of Hotmoka nodes, that publish their API as a web service.
 */
public abstract class NodeServices {

	private NodeServices() {}

	/**
	 * Yields and starts a network service that exposes a REST API to a given Hotmoka node.
	 * 
	 * @param node the Hotmoka node
	 * @param port the port where the service should be opened
	 * @return the network service
	 * @throws FailedDeploymentException if the service cannot be deployed
	 */
	public static NodeService of(Node node, int port) throws FailedDeploymentException {
		return new NodeServiceImpl(node, port);
	}
}