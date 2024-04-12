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

package io.hotmoka.node.remote;

import java.io.IOException;

import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.remote.internal.AbstractRemoteNode;
import jakarta.websocket.DeploymentException;

/**
 * Providers of nodes that forward their calls to a remote network service.
 */
public abstract class RemoteNodes {

	private RemoteNodes() {}

	/**
     * Yields a remote node with the given configuration.
     *
     * @param config the configuration
     * @return the remote node
	 * @throws DeploymentException if the remote node could not be deployed
	 * @throws IOException if the remote node could not be created
     */
	public static RemoteNode of(RemoteNodeConfig config) throws IOException, DeploymentException {
        // there are two implementations: for websockets or for http connections
        return new AbstractRemoteNode(config);
    }
}