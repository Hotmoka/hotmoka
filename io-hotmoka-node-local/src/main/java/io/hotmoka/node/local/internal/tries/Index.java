/*
Copyright 2025 Fausto Spoto

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
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.UnmarshallingContexts;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

public class Index {

	private final Store store;
	private final Environment env;
	private final static int MAX = 10; // TODO: introduce configuration parameter

	public Index(Store store, Environment env) {
		this.store = store;
		this.env = env;
	}

	/**
	 * Yields, from the index, information about the transactions that have affected the given {@code object}.
	 * 
	 * @param object the object
	 * @return the transactions that have affected {@code object}, in chronological order of inclusion
	 *         in the store of the node; this is empty if there is no information about {@code object}
	 */
	public Optional<Stream<TransactionReference>> get(StorageReference object) {
		ByteIterable bi = env.computeInReadonlyTransaction(txn -> store.get(txn, new MarshallableStorageReference(object).toByteIterable()));
		return Optional.ofNullable(bi).map(MarshallableArrayOfTransactionReferences::new).map(MarshallableArrayOfTransactionReferences::stream);
	}

	/**
	 * Adds to the index the fact that {@code transaction} has been executed and provided the
	 * given response. All objects modified in that response (if any) get marked in the index as
	 * modified during {@code transaction}. The index is updated inside the database transaction {@code txn}.
	 * 
	 * @param transaction the Hotmoka transaction that has modified the objects
	 * @param response the response of {@code transaction}
	 * @param txn the database transaction where the update occurs
	 */
	public void add(TransactionReference transaction, TransactionResponse response, Transaction txn) {
		if (MAX > 0 && response instanceof TransactionResponseWithUpdates trwu) {
			var affectedObjects = trwu.getUpdates().map(Update::getObject).distinct().toArray(StorageReference[]::new);

			for (var object: affectedObjects)
				bind(object, transaction, txn);

			bind(transaction, affectedObjects, txn);
		}
	}

	/**
	 * Removes the given Hotmoka transaction from the index.
	 * 
	 * @param transaction the Hotmoka transaction that must be removed from the index; information
	 *                    about the objects modified by this transaction gets removed from the index
	 * @param txn the database transaction where the update occurs
	 */
	public void remove(TransactionReference transaction, Transaction txn) {
		
	}

	private void bind(StorageReference object, TransactionReference transaction, Transaction txn) {
		var objectBI = new MarshallableStorageReference(object).toByteIterable();
		var oldTxs = store.get(txn, objectBI);

		MarshallableArrayOfTransactionReferences result;
		if (oldTxs == null)
			// MAX > 0, therefore length 1 is OK
			result = new MarshallableArrayOfTransactionReferences(new TransactionReference[] { transaction });
		else {
			// add() will, internally, impose MAX as upper bound to the length of the array
			var oldArray = new MarshallableArrayOfTransactionReferences(oldTxs);
			result = oldArray.add(transaction);

			// if transactions of the history of object overflow on the left and consequently get
			// forgotten, the object gets unbound from the map (transaction -> objects)
			for (int pos = oldArray.references.length - MAX; pos >= 0; pos--)
				unbind(result.references[pos], object, txn);
		}

		store.put(txn, objectBI, result.toByteIterable());
	}

	private void bind(TransactionReference transaction, StorageReference[] objects, Transaction txn) {
		var transactionBI = new MarshallableTransactionReference(transaction).toByteIterable();
		store.put(txn, transactionBI, new MarshallableArrayOfStorageReferences(objects).toByteIterable());
	}

	private void unbind(TransactionReference transaction, StorageReference object, Transaction txn) {
		var transactionBI = new MarshallableTransactionReference(transaction).toByteIterable();
		var result = new MarshallableArrayOfStorageReferences(store.get(txn, transactionBI)).remove(object);

		if (result.references.length == 0)
			store.delete(txn, transactionBI);
		else
			store.put(txn, transactionBI, result.toByteIterable());
	}

	private abstract static class MyMarshallable extends AbstractMarshallable {

		public ByteIterable toByteIterable() {
			return ByteIterable.fromBytes(toByteArray());
		}
	}

	/**
	 * A marshallable transaction reference. Transaction references are already marshallable, but
	 * we redefine their marshalling behavior here since, by default, they use a compact
	 * shared representation for data that contains many of them, which would be wrong here,
	 * since they are marshalled in isolation.
	 */
	private static class MarshallableTransactionReference extends MyMarshallable {
		private final TransactionReference reference;
	
		private MarshallableTransactionReference(TransactionReference reference) {
			this.reference = reference;
		}
	
		/**
		 * Creates a transaction reference unmarshalled from the given byte iterable.
		 * 
		 * @param bi the byte iterable
		 */
		private MarshallableTransactionReference(ByteIterable bi) {
			try (var bais = new ByteArrayInputStream(bi.getBytes()); var context = UnmarshallingContexts.of(bais)) {
				this.reference = new MarshallableTransactionReference(context).reference;
			}
			catch (IOException e) {
				throw new IndexException(e);
			}
		}

		private MarshallableTransactionReference(UnmarshallingContext context) {
			try {
				this.reference = TransactionReferences.of
					(context.readBytes(TransactionReference.REQUEST_HASH_LENGTH, "mismatched transaction hash length"));
			}
			catch (IOException e) {
				throw new IndexException(e);
			}
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeBytes(reference.getHash());
		}
	}

	/**
	 * A marshallable storage reference. Storage references are already marshallable, but
	 * we redefine their marshalling behavior here since, by default, they use a compact
	 * shared representation for data that contains many of them, which would be wrong here,
	 * since they are marshalled in isolation.
	 */
	private static class MarshallableStorageReference extends MyMarshallable {
		private final StorageReference reference;
	
		private MarshallableStorageReference(StorageReference reference) {
			this.reference = reference;
		}
	
		/**
		 * Creates a storage reference unmarshalled from the given byte iterable.
		 * 
		 * @param bi the byte iterable
		 */
		private MarshallableStorageReference(ByteIterable bi) {
			try (var bais = new ByteArrayInputStream(bi.getBytes()); var context = UnmarshallingContexts.of(bais)) {
				this.reference = new MarshallableStorageReference(context).reference;
			}
			catch (IOException e) {
				throw new IndexException(e);
			}
		}

		private MarshallableStorageReference(UnmarshallingContext context) {
			try {
				TransactionReference transaction = TransactionReferences.of
						(context.readBytes(TransactionReference.REQUEST_HASH_LENGTH, "mismatched transaction hash length"));
				BigInteger progressive = context.readBigInteger();

				this.reference = StorageValues.reference(transaction, progressive);
			}
			catch (IOException e) {
				throw new IndexException(e);
			}
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeBytes(reference.getTransaction().getHash());
			context.writeBigInteger(reference.getProgressive());
		}
	}

	/**
	 * A marshallable array of transaction references.
	 */
	private static class MarshallableArrayOfTransactionReferences extends MyMarshallable {
		private final TransactionReference[] references;
	
		private MarshallableArrayOfTransactionReferences(Stream<TransactionReference> references) {
			this.references = references.toArray(TransactionReference[]::new);
		}
	
		private MarshallableArrayOfTransactionReferences(TransactionReference[] references) {
			this.references = references;
		}

		/**
		 * Creates an array of transaction references by unmarshalling them from the given byte iterable.
		 * 
		 * @param bi the byte iterable
		 */
		private MarshallableArrayOfTransactionReferences(ByteIterable bi) {
			try (var bais = new ByteArrayInputStream(bi.getBytes()); var context = UnmarshallingContexts.of(bais)) {
				this.references = context.readLengthAndArray(_context -> new MarshallableTransactionReference(_context).reference, TransactionReference[]::new);
			}
			catch (IOException e) {
				throw new IndexException(e);
			}
		}

		/**
		 * Adds the given reference at the end of this array.
		 * 
		 * @param reference the reference
		 * @return the resulting array of transaction references, in unchanged order
		 */
		private MarshallableArrayOfTransactionReferences add(TransactionReference reference) {
			TransactionReference[] result;
			if (references.length < MAX) {
				result = new TransactionReference[references.length + 1];
				System.arraycopy(references, 0, result, 0, references.length);
			}
			else {
				result = new TransactionReference[MAX];
				// we only keep the last MAX-1 elements
				System.arraycopy(references, references.length - (MAX - 1), result, 0, MAX - 1);
			}

			result[result.length - 1] = reference;

			return new MarshallableArrayOfTransactionReferences(result);
		}

		/**
		 * Removes the given reference from this array. This method assumes that
		 * the this array actually contains the reference.
		 * 
		 * @param reference the reference; this must belong to this array
		 * @return the resulting array of transaction references, in unchanged order
		 */
		private MarshallableArrayOfTransactionReferences remove(TransactionReference reference) {
			var result = new TransactionReference[references.length - 1];
			int pos = 0;
			for (var reference2: references)
				if (!reference2.equals(reference))
					result[pos++] = reference2;

			return new MarshallableArrayOfTransactionReferences(result);
		}

		private Stream<TransactionReference> stream() {
			return Stream.of(references);
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeLengthAndArray(Stream.of(references).map(MarshallableTransactionReference::new).toArray(MarshallableTransactionReference[]::new));
		}
	}

	/**
	 * A marshallable array of storage references.
	 */
	private static class MarshallableArrayOfStorageReferences extends MyMarshallable {
		private final StorageReference[] references;
	
		private MarshallableArrayOfStorageReferences(Stream<StorageReference> references) {
			this.references = references.toArray(StorageReference[]::new);
		}
	
		private MarshallableArrayOfStorageReferences(StorageReference[] references) {
			this.references = references;
		}

		/**
		 * Creates an array of storage references by unmarshalling them from the given byte iterable.
		 * 
		 * @param bi the byte iterable
		 */
		private MarshallableArrayOfStorageReferences(ByteIterable bi) {
			try (var bais = new ByteArrayInputStream(bi.getBytes()); var context = UnmarshallingContexts.of(bais)) {
				this.references = context.readLengthAndArray(_context -> new MarshallableStorageReference(_context).reference, StorageReference[]::new);
			}
			catch (IOException e) {
				throw new IndexException(e);
			}
		}

		/**
		 * Removes the given reference from this array. This method assumes that
		 * the this array actually contains the reference.
		 * 
		 * @param reference the reference; this must belong to this array
		 * @return the resulting array of storage references, in unchanged order
		 */
		private MarshallableArrayOfStorageReferences remove(StorageReference reference) {
			var result = new StorageReference[references.length - 1];
			int pos = 0;
			for (var reference2: references)
				if (!reference2.equals(reference))
					result[pos++] = reference2;

			return new MarshallableArrayOfStorageReferences(result);
		}

		private Stream<StorageReference> stream() {
			return Stream.of(references);
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeLengthAndArray(Stream.of(references).map(MarshallableStorageReference::new).toArray(MarshallableStorageReference[]::new));
		}
	}
}
