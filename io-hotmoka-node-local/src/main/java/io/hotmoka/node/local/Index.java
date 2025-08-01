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

package io.hotmoka.node.local;

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

/**
 * An index of a Hotmoka node, kept in a database. It provides, for each object, a list of transactions
 * that have affected that object, of a given maximal length provided at the time of creation
 * of the index object. This list can be expanded with the addition of new transactions
 * or shrunk with the removal of transactions. Internally, this object keeps, in the database,
 * also a map from transactions to affected objects, which is useful only to implement
 * the {@link #remove(TransactionReference, Transaction)} method. The two maps are kept
 * consistent among them.
 */
public class Index {

	/**
	 * The store of the database where the index is kept.
	 */
	private final Store store;

	/**
	 * The environment of the database where the index is kept.
	 */
	private final Environment env;

	/**
	 * The maximal number of affecting transactions kept in the index for each given object.
	 */
	private final int size;

	/**
	 * Creates an index object. If its database already exists, if will
	 * use the data contained in that database.
	 * 
	 * @param store the store of the database where the index is kept
	 * @param env the environment of the database where the index is kept
	 * @param size the maximal number of affecting transactions kept in the index for each given object
	 */
	public Index(Store store, Environment env, int size) {
		this.store = store;
		this.env = env;
		this.size = size;
	}

	/**
	 * Fetches from the index information about the transactions that have affected the given object.
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
	 * Stores into the index the fact that {@code transaction} has been executed and provided the
	 * given response. All objects modified in that response (if any) get marked in the index as
	 * affected by {@code transaction}. The index is updated inside the database transaction {@code txn}.
	 * 
	 * @param transaction the reference of the Hotmoka transaction that has modified the objects
	 * @param response the response of {@code transaction}
	 * @param txn the database transaction where the database updates get accumulated
	 */
	public void add(TransactionReference transaction, TransactionResponse response, Transaction txn) {
		// if size is 0, there is no need to store data in the database
		if (size > 0 && response instanceof TransactionResponseWithUpdates trwu) {
			var affectedObjects = trwu.getUpdates().map(Update::getObject).distinct().toArray(StorageReference[]::new);

			for (var object: affectedObjects)
				bind(object, transaction, txn);

			bind(transaction, affectedObjects, txn);
		}
	}

	/**
	 * Removes the given Hotmoka transaction from the index.
	 * 
	 * @param transaction the reference of the Hotmoka transaction that must be removed from the index; information
	 *                    about the objects affected by that transaction gets removed from the index
	 * @param txn the database transaction where the database updates get accumulated
	 */
	public void remove(TransactionReference transaction, Transaction txn) {
		var transactionBI = new MarshallableTransactionReference(transaction).toByteIterable();
		var objectsBI = store.get(txn, transactionBI);
		if (objectsBI != null) {
			for (var object: new MarshallableArrayOfStorageReferences(objectsBI).references)
				unbind(object, transaction, transactionBI, txn);

			store.delete(txn, transactionBI);
		}
	}

	/**
	 * Removes the given transaction from those affecting the given object.
	 * 
	 * @param object the object
	 * @param transaction the reference of the transaction
	 * @param transactionBI the same as {@code transaction}, but already as a byte iterable
	 * @param txn the database transaction where the database updates get accumulated
	 */
	private void unbind(StorageReference object, TransactionReference transaction, ByteIterable transactionBI, Transaction txn) {
		var objectBI = new MarshallableStorageReference(object).toByteIterable();
		var oldTransactionsBI = store.get(txn, objectBI);

		if (oldTransactionsBI != null) {
			var result = new MarshallableArrayOfTransactionReferences(oldTransactionsBI).remove(transaction);
			if (result.references.length == 0)
				store.delete(txn, objectBI);
			else
				store.put(txn, objectBI, result.toByteIterable());
		}
		else
			throw new IndexException("Object " + object + " was expected to be in the index");
	}

	/**
	 * Adds the given transaction to those affecting the given object.
	 * 
	 * @param object the object
	 * @param transaction the reference to the transaction
	 * @param txn the database transaction where the database updates get accumulated
	 */
	private void bind(StorageReference object, TransactionReference transaction, Transaction txn) {
		var objectBI = new MarshallableStorageReference(object).toByteIterable();
		var oldTxs = store.get(txn, objectBI);

		MarshallableArrayOfTransactionReferences result;
		if (oldTxs == null)
			// here size > 0, therefore length 1 is OK
			result = new MarshallableArrayOfTransactionReferences(new TransactionReference[] { transaction });
		else {
			MarshallableArrayOfTransactionReferences oldArray = new MarshallableArrayOfTransactionReferences(oldTxs);
			// add() will, internally, impose size as upper bound to the length of the array
			result = oldArray.add(transaction, size);

			// if transactions overflow on the left, they get forgotten, so that we keep {@link #size}
			// as maximal length of the list of transaction references in the index for object;
			// note that we need a for rather than a simple if, since the length of the old list
			// might actually be larger than size if the size parameter was changed between
			// successive creations of the Index object
			for (int pos = oldArray.references.length - size; pos >= 0; pos--)
				unbind(result.references[pos], object, txn);
		}

		store.put(txn, objectBI, result.toByteIterable());
	}

	/**
	 * Binds in the index the given transaction reference to the given list of objects that it might affect.
	 * 
	 * @param transaction the reference to the transaction
	 * @param objects the reference to the objects
	 * @param txn the database transaction where the database updates get accumulated
	 */
	private void bind(TransactionReference transaction, StorageReference[] objects, Transaction txn) {
		var transactionBI = new MarshallableTransactionReference(transaction).toByteIterable();
		store.put(txn, transactionBI, new MarshallableArrayOfStorageReferences(objects).toByteIterable());
	}

	/**
	 * Removes from the index the given object from those affected by the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @param object the reference to the object
	 * @param txn the database transaction where the database updates get accumulated
	 */
	private void unbind(TransactionReference transaction, StorageReference object, Transaction txn) {
		var transactionBI = new MarshallableTransactionReference(transaction).toByteIterable();
		var oldObjectsBI = store.get(txn, transactionBI);

		if (oldObjectsBI != null) {
			var result = new MarshallableArrayOfStorageReferences(oldObjectsBI).remove(object);

			if (result.references.length == 0)
				store.delete(txn, transactionBI);
			else
				store.put(txn, transactionBI, result.toByteIterable());
		}
		else
			throw new IndexException("Transaction " + transaction + " was expected to be in the index");
	}

	/**
	 * A marshallable object with an extra method to transform it into a byte iterable.
	 */
	private abstract static class IndexMarshallable extends AbstractMarshallable {

		/**
		 * Yields a byte iterable for the marshalling of this object.
		 * 
		 * @return the byte iterable
		 */
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
	private static class MarshallableTransactionReference extends IndexMarshallable {
		private final TransactionReference reference;
	
		private MarshallableTransactionReference(TransactionReference reference) {
			this.reference = reference;
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
	private static class MarshallableStorageReference extends IndexMarshallable {
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
	public static class MarshallableArrayOfTransactionReferences extends IndexMarshallable {
		private final TransactionReference[] references;
	
		public MarshallableArrayOfTransactionReferences(TransactionReference[] references) {
			this.references = references;
		}

		/**
		 * Creates an array of transaction references by unmarshalling them from the given byte iterable.
		 * 
		 * @param bi the byte iterable
		 */
		public MarshallableArrayOfTransactionReferences(ByteIterable bi) {
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
		 * @param size the maximal size for the resulting array; beyond this threshold,
		 *             the first transaction in the array gets dropped
		 * @return the resulting array of transaction references, in unchanged order
		 */
		private MarshallableArrayOfTransactionReferences add(TransactionReference reference, int size) {
			TransactionReference[] result;
			if (references.length < size) {
				result = new TransactionReference[references.length + 1];
				System.arraycopy(references, 0, result, 0, references.length);
			}
			else {
				result = new TransactionReference[size];
				// we only keep the last size-1 elements
				System.arraycopy(references, references.length - (size - 1), result, 0, size - 1);
			}

			// we put the reference at the end of the array
			result[result.length - 1] = reference;

			return new MarshallableArrayOfTransactionReferences(result);
		}

		/**
		 * Removes the given reference from this array.
		 * 
		 * @param reference the reference
		 * @return the resulting array of transaction references, in unchanged order
		 */
		private MarshallableArrayOfTransactionReferences remove(TransactionReference reference) {
			return new MarshallableArrayOfTransactionReferences(Stream.of(references).filter(reference2 -> !reference2.equals(reference)).toArray(TransactionReference[]::new));
		}

		public Stream<TransactionReference> stream() {
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
	private static class MarshallableArrayOfStorageReferences extends IndexMarshallable {
		private final StorageReference[] references;
	
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
		 * Removes the given reference from this array.
		 * 
		 * @param reference the reference
		 * @return the resulting array of storage references, in unchanged order
		 */
		private MarshallableArrayOfStorageReferences remove(StorageReference reference) {
			return new MarshallableArrayOfStorageReferences(Stream.of(references).filter(reference2 -> !reference2.equals(reference)).toArray(StorageReference[]::new));
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeLengthAndArray(Stream.of(references).map(MarshallableStorageReference::new).toArray(MarshallableStorageReference[]::new));
		}
	}
}