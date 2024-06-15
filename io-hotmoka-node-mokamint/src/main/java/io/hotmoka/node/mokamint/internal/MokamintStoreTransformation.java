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

package io.hotmoka.node.mokamint.internal;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractTrieBasedStoreTransformation;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;

/**
 * A transformation of a store of a Tendermint node.
 */
public class MokamintStoreTransformation extends AbstractTrieBasedStoreTransformation<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> {

	/**
	 * Creates a transformation whose transaction are executed with the given executors.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of transactions in the transformation
	 */
	protected MokamintStoreTransformation(MokamintStore store, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, consensus, now);
	}
}