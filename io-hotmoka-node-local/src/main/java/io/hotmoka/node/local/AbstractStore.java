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

import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.internal.store.AbstractStoreImpl;

@Immutable
public abstract class AbstractStore<S extends AbstractStore<S,T>, T extends AbstractStoreTransaction<S, T>> extends AbstractStoreImpl<S, T> {

	protected AbstractStore(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
		super(executors, consensus, config, hasher);
	}

	protected AbstractStore(AbstractStore<S, T> toClone, LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation) {
		super(toClone, checkedSignatures, classLoaders, consensus, gasPrice, inflation);
	}
}