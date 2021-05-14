/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.service;

import io.hotmoka.service.internal.NodeServiceImpl;
import io.hotmoka.nodes.Node;

/**
 * A network service that exposes a REST API to a Hotmoka node.
 */
public interface NodeService extends AutoCloseable {

	/**
	 * Yields and starts network service that exposes a REST API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @return the network service
	 */
	static NodeService of(NodeServiceConfig config, Node node) {
		return new NodeServiceImpl(config, node);
	}

	/**
	 * Stops the service and releases its resources.
	 */
	@Override
	void close(); // no checked exceptions
}