/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.local.api;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 */
@ThreadSafe
public interface LocalNode<C extends LocalNodeConfig<C,?>> extends Node {

	/**
	 * Yields the local configuration of this node.
	 * 
	 * @return the local configuration
	 * @throws NodeException if this node cannot complete the operation correctly
	 */
	C getLocalConfig() throws ClosedNodeException;
}