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

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A map from transaction requests into their error, backed by a Merkle-Patricia trie.
 */
public class TrieOfErrors {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<TransactionReference, MarshallableString> parent;

	/**
	 * Builds a Merkle-Patricia trie that maps transaction requests into their errors.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfErrors(Store store, Transaction txn, byte[] root, long numberOfCommits) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithms.sha256(Marshallable::toByteArray);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, new HashingForTransactionReference(), hashingForNodes, MarshallableString::from, numberOfCommits);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("unexpected exception", e);
		}
	}

	public Optional<String> get(TransactionReference key) {
		Optional<MarshallableString> result = parent.get(key);
		return result.map(MarshallableString::toString);
	}

	public void put(TransactionReference key, String value) {
		parent.put(key, new MarshallableString(value));
	}

	public byte[] getRoot() {
		return parent.getRoot();
	}

	/**
	 * A string that can be marshalled into an object stream.
	 */
	private static class MarshallableString extends Marshallable<MarshallingContext> {
		private final String s;

		private MarshallableString(String s) {
			this.s = s;
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeUTF(s);
		}

		@Override
		public String toString() {
			return s;
		}

		/**
		 * Factory method that unmarshals a string from the given stream.
		 * 
		 * @param context the unmarshalling context
		 * @return the string
		 * @throws IOException if the string could not be unmarshalled
		 */
		private static MarshallableString from(UnmarshallingContext context) throws IOException {
			return new MarshallableString(context.readUTF());
		}

		@Override
		protected MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
			return new MarshallingContext(os);
		}
	}

	/**
	 * Garbage-collects all keys that have been updated during the given number of commit.
	 * 
	 * @param commitNumber the number of the commit to garbage collect
	 */
	public void garbageCollect(long commitNumber) {
		parent.garbageCollect(commitNumber);
	}
}