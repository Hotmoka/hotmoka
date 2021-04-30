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

package io.hotmoka.takamaka.internal;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.local.CheckableStore;
import io.hotmoka.stores.FullTrieBasedStore;
import io.hotmoka.takamaka.TakamakaBlockchainConfig;

/**
 * A full trie-based store for the Takamaka blockchain. By using a full store,
 * that keeps also requests and errors, there is less burden on the Takamaka blockchain,
 * hence the integration is easier than with a partial store.
 */
@ThreadSafe
class Store extends FullTrieBasedStore<TakamakaBlockchainConfig> implements CheckableStore {

	/**
     * Creates a store for the Takamaka blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node having this store
     */
    Store(TakamakaBlockchainImpl node) {
    	super(node);

    	setRootsAsCheckedOut();
    }

    /**
     * Creates a clone of the given store.
     * 
	 * @param parent the store to clone
     */
    Store(Store parent) {
    	super(parent);
    }

    @Override
    public void checkout(byte[] root) {
    	super.checkout(root);
	}
}