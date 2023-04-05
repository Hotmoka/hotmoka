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

package io.hotmoka.remote;

import java.io.IOException;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.remote.internal.http.HTTPRemoteNodeImpl;
import io.hotmoka.remote.internal.websockets.WebSocketsRemoteNodeImpl;
import io.hotmoka.nodes.Node;

/**
 * A node that forwards its calls to a remote network service.
 */
@ThreadSafe
public interface RemoteNode extends Node {

    /**
     * Yields a remote node with the given configuration.
     *
     * @param config the configuration
     * @return the remote node
     * @throws IOException 
     */
    static RemoteNode of(RemoteNodeConfig config) throws IOException {
        // there are two implementations: for websockets or for http connections
        return config.webSockets ? new WebSocketsRemoteNodeImpl(config) : new HTTPRemoteNodeImpl(config);
    }
}