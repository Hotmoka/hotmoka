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

import java.net.URI;

import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.node.remote.internal.RemoteNodeImpl;
import io.hotmoka.websockets.api.FailedDeploymentException;

/**
 * Providers of nodes that forward their calls to a remote network service.
 */
public abstract class RemoteNodes {

	private RemoteNodes() {}

	/**
     * Yields a remote node with the given configuration.
     *
	 * @param uri the URI of the network service that gets bound to the remote node
	 * @param timeout the time (in milliseconds) allowed for a call to the network service;
	 *                beyond that threshold, a timeout exception is thrown
     * @return the remote node
	 * @throws FailedDeploymentException if the remote node could not be deployed
     */
	public static RemoteNode of(URI uri, int timeout) throws FailedDeploymentException {
        return new RemoteNodeImpl(uri, timeout);
    }
}