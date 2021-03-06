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
import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.crypto.HashingAlgorithm;
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
	 * @param garbageCollected true if and only if unused nodes must be garbage collected; in general,
	 *                         this can be true if previous configurations of the trie needn't be
	 *                         rechecked out in the future
	 */
	public TrieOfErrors(Store store, Transaction txn, byte[] root, boolean garbageCollected) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, new HashingForTransactionReference(), hashingForNodes, MarshallableString::from, garbageCollected);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
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
	private static class MarshallableString extends Marshallable {
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
	}
}