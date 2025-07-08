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

package io.hotmoka.node.local.internal.tries;

import java.io.ByteArrayInputStream;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * A Merkle-Patricia trie that maps references to transaction requests into their request itself.
 * It uses sha256 as hashing algorithm for the trie's nodes and an array of 0's to represent
 * the empty trie.
 */
public class TrieOfRequests extends AbstractPatriciaTrie<TransactionReference, TransactionRequest<?>, TrieOfRequests> {

	/**
	 * Builds a Merkle-Patricia trie that maps references to transaction requests into their responses.
	 * 
	 * @param store the supporting key/value store
	 * @param root the root of the trie to check out
	 * @param node the node for which the trie is being built
	 * @throws UnknownKeyException if {@code root} cannot be found in the trie
	 */
	public TrieOfRequests(KeyValueStore store, byte[] root, AbstractTrieBasedLocalNodeImpl<?,?,?,?> node) throws TrieException, UnknownKeyException {
		super(store, root, HashingAlgorithms.identity32().getHasher(TransactionReference::getHash),
			// we use a NodeUnmarshallingContext because that is the default used for marshalling requests
			node.mkSHA256(), new byte[32], TransactionRequest<?>::toByteArray, bytes -> TransactionRequests.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))));
	}

	private TrieOfRequests(TrieOfRequests cloned, byte[] root) throws TrieException, UnknownKeyException {
		super(cloned, root);
	}

	@Override
	protected void malloc() throws TrieException {
		super.malloc();
	}

	@Override
	protected void free() throws TrieException {
		super.free();
	}

	@Override
	public TrieOfRequests checkoutAt(byte[] root) throws TrieException, UnknownKeyException {
		return new TrieOfRequests(this, root);
	}
}