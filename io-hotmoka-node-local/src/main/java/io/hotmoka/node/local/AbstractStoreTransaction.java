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

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.internal.store.AbstractStoreTransactionImpl;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
public abstract class AbstractStoreTransaction<S extends AbstractStore<S, T>, T extends AbstractStoreTransaction<S, T>> extends AbstractStoreTransactionImpl<S, T> {

	protected AbstractStoreTransaction(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		super(store, executors, consensus, now);
	}
}