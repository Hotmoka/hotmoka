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
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionResponseWithInstrumentedJar;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * A Merkle-Patricia trie that maps references to transaction requests into their responses.
 * It optimizes the trie by sharing identical jars in responses containing an instrumented jar.
 * It uses sha256 as hashing algorithm for the trie's nodes and an array of 0's to represent
 * the empty trie.
 */
public class TrieOfResponses extends AbstractPatriciaTrie<TransactionReference, TransactionResponse, TrieOfResponses> {

	private final static Logger logger = Logger.getLogger(TrieOfResponses.class.getName());

	/**
	 * The hasher used for the jars in the responses that included a jar.
	 */
	private final Hasher<byte[]> hasherForJars;

	/**
	 * Builds a Merkle-Patricia trie that maps references to transaction requests into their responses.
	 * 
	 * @param store the supporting key/value store
	 * @param root the root of the trie to check out
	 * @throws UnknownKeyException if {@code root} cannot be found in the trie
	 */
	public TrieOfResponses(KeyValueStore store, byte[] root) throws UnknownKeyException {
		super(store, root, HashingAlgorithms.identity32().getHasher(TransactionReference::getHash),
			// we use a NodeUnmarshallingContext because that is the default used for marshalling responses
			mkSHA256(), new byte[32], TransactionResponse::toByteArray, bytes -> TransactionResponses.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))));

		this.hasherForJars = mkSHA256().getHasher(Function.identity());
	}

	private TrieOfResponses(TrieOfResponses cloned, byte[] root) throws UnknownKeyException {
		super(cloned, root);

		this.hasherForJars = cloned.hasherForJars;
	}

	private static HashingAlgorithm mkSHA256() {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new LocalNodeException(e);
		}
	}

	@Override
	protected void malloc() {
		super.malloc();
	}

	@Override
	protected void free() {
		super.free();
	}

	@Override
	public Optional<TransactionResponse> get(TransactionReference key) {
		return super.get(key).map(this::readTransformation);
	}

	@Override
	public TrieOfResponses put(TransactionReference key, TransactionResponse value) {
		return super.put(key, writeTransformation(value));
	}

	@Override
	public TrieOfResponses checkoutAt(byte[] root) throws UnknownKeyException {
		return new TrieOfResponses(this, root);
	}

	/**
	 * A function called on each value before being stored in the trie;
	 * the result is actually stored at its place; the goal is
	 * to implement an optimization that shares the jar in the response.
	 * 
	 * @param response the actual response inserted in this trie
	 * @return the response that is put in its place in the parent trie
	 */
	private TransactionResponse writeTransformation(TransactionResponse response) {
		if (response instanceof JarStoreTransactionResponseWithInstrumentedJar trwij) {
			byte[] jar = trwij.getInstrumentedJar();
			// we store the jar in the store: if it was already installed before, it gets shared
			byte[] hashedJar = hasherForJars.hash(jar);
			getStore().put(hashedJar, jar);

			// we replace the jar with its hash
			response = replaceJar(trwij, hashedJar);
		}

		return response;
	}

	/**
	 * A function called on each value read from the trie;
	 * the result is actually returned at its place; the goal is to
	 * recover a jar shared with other responses.
	 * 
	 * @param response the response read from the parent trie
	 * @return return the actual response returned by this trie
	 */
	private TransactionResponse readTransformation(TransactionResponse response) {
		if (response instanceof JarStoreTransactionResponseWithInstrumentedJar trwij) {
			// we replace the hash of the jar with the actual jar
			try {
				byte[] jar = getStore().get(trwij.getInstrumentedJar());
				response = replaceJar(trwij, jar);
			}
			catch (UnknownKeyException e) {
				logger.log(Level.SEVERE, "cannot find the jar for the transaction response");
				throw new TrieException("Cannot find the jar for the transaction response", e);
			}
		}

		return response;
	}

	private TransactionResponse replaceJar(JarStoreTransactionResponseWithInstrumentedJar response, byte[] newJar) {
		if (response instanceof JarStoreTransactionSuccessfulResponse jstsr)
			return TransactionResponses.jarStoreSuccessful
				(newJar, jstsr.getDependencies(), jstsr.getVerificationVersion(), jstsr.getUpdates(),
				jstsr.getGasConsumedForCPU(), jstsr.getGasConsumedForRAM(), jstsr.getGasConsumedForStorage());
		else if (response instanceof JarStoreInitialTransactionResponse jsitr)
			return TransactionResponses.jarStoreInitial(newJar, jsitr.getDependencies(), jsitr.getVerificationVersion());
		else
			throw new TrieException("Unexpected response containing a jar, of class " + response.getClass().getName());
	}
}