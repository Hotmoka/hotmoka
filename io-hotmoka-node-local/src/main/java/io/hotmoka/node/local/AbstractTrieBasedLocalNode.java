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

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.internal.tries.AbstractTrieBasedLocalNodeImpl;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 * @param <T> the type of the store transformations that can be started from this store
 */
@ThreadSafe
public abstract class AbstractTrieBasedLocalNode<N extends AbstractTrieBasedLocalNode<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractTrieBasedStore<N,C,S,T>, T extends AbstractTrieBasedStoreTransformation<N,C,S,T>> extends AbstractTrieBasedLocalNodeImpl<N,C,S,T> {

	/**
	 * Creates a new node.
	 * 
	 * @param config the configuration of the node
	 * @param init if true, the working directory of the node gets initialized
	 * @throws NodeException if the operation cannot be completed correctly
	 */
	protected AbstractTrieBasedLocalNode(C config, boolean init) throws NodeException {
		super(config, init);
	}
}