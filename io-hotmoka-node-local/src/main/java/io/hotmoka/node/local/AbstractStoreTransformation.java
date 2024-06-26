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

package io.hotmoka.node.local;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.internal.AbstractStoreTransformationImpl;

/**
 * Abstract implementation of a store transformation, for extension. This is not thread-safe hence it must
 * be used by a thread at a time or shared under synchronization.
 * 
 * @param <N> the type of the node whose store performs this transformation
 * @param <C> the type of the configuration of the node whose store performs this transformation
 * @param <S> the type of the store that performs this transformation
 * @param <T> the type of this store transformation
 */
public abstract class AbstractStoreTransformation<N extends AbstractLocalNode<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractStore<N,C,S,T>, T extends AbstractStoreTransformation<N,C,S,T>> extends AbstractStoreTransformationImpl<N,C,S,T> {

	/**
	 * Creates a transformation whose transaction are executed with the given executors.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of transactions in the transformation
	 */
	protected AbstractStoreTransformation(S store, ConsensusConfig<?,?> consensus, long now) {
		super(store, consensus, now);
	}
}