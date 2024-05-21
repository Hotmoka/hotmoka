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

package io.hotmoka.node.local;

import java.util.Optional;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.internal.AbstractLocalNodeImpl;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * Specific implementations can subclass this and implement the abstract template methods.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 * @param <T> the type of the store transformations of the store of the node
 */
@ThreadSafe
public abstract class AbstractLocalNode<C extends LocalNodeConfig<C,?>, S extends AbstractStore<S, T>, T extends AbstractStoreTranformation<S, T>> extends AbstractLocalNodeImpl<C, S, T> {

	/**
	 * Creates a node.
	 * 
	 * @param consensus the consensus configuration of the node; if missing, this will be extracted
	 *                  from the database of the node
	 * @param config the local configuration of the node
	 * @throws NodeException if the node could not be created
	 */
	protected AbstractLocalNode(Optional<ConsensusConfig<?,?>> consensus, C config) throws NodeException {
		super(consensus, config);
	}
}