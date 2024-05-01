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

package io.hotmoka.stores.internal;

import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;

/**
 * A Merkle-Patricia trie that maps references to transaction requests into their request itself.
 */
public class TrieOfRequests extends AbstractPatriciaTrie<TransactionReference, TransactionRequest<?>, TrieOfRequests> {

	/**
	 * Builds a Merkle-Patricia trie that maps references to transaction requests into their responses.
	 * 
	 * @param store the supporting key/value store
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 */
	public TrieOfRequests(KeyValueStore store, Optional<byte[]> root) throws TrieException {
		super(store, root, HashingAlgorithms.identity32().getHasher(TransactionReference::getHash),
			sha256(), TransactionRequest<?>::toByteArray, bytes -> TransactionRequests.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))), -1L);
	}

	private TrieOfRequests(TrieOfRequests cloned, byte[] root) {
		super(cloned, root);
	}

	private TrieOfRequests(TrieOfRequests cloned, KeyValueStore store) {
		super(cloned, store);
	}

	@Override
	public TrieOfRequests checkoutAt(byte[] root) {
		return new TrieOfRequests(this, root);
	}

	private static HashingAlgorithm sha256() throws TrieException {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new TrieException(e);
		}
	}
}