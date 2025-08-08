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

package io.hotmoka.node.mokamint.internal;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.Index;
import io.hotmoka.node.local.Index.MarshallableArrayOfTransactionReferences;
import io.hotmoka.node.local.IndexException;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Store;
import io.mokamint.node.api.Block;
import io.mokamint.node.api.NonGenesisBlock;
import io.mokamint.node.api.PortionRejectedException;
import io.mokamint.node.api.Transaction;

/**
 * The indexer of a Hotmoka node based on Mokamint. The goal of this object is to fetch
 * blocks created by the underlying Mokamint engine and add their transactions to the index.
 * However, this is complicated because of two reasons: the underlying Mokamint engine
 * might be in another process or machine, therefore the API does not support a single
 * transaction on the databases of the engine and of the Hotmoka node; moreover,
 * the Mokamint engine might have history changes, which require to remove from the
 * index the transactions in the old history. The solution is to keep in the database
 * information about the uppermost blocks of the history, in order to spot history changes
 * and update the index accordingly, with repeated queries to the underlying Mokamint engine.
 */
public class Indexer {

	/**
	 * The node this indexer is working for.
	 */
	private final MokamintNode<?> node;

	/**
	 * The store of the database of {@code node} where indexing data can be kept.
	 */
	private final Store store;

	/**
	 * The environment of the database of {@code node} where indexing data can be kept.
	 */
	private final Environment env;

	/**
	 * The index created for the {@link #node}.
	 */
	private final Index index;

	/**
	 * The hashing algorithm to transform transactions into their transaction reference.
	 */
	private final Hasher<byte[]> sha256;

	/**
	 * The constant key bound, in {@link #store}, to (one less than) the base height of the portion of the blockchain
	 * from where indexing starts. This is not the top of the blockchain, but deeper in the chain,
	 * in order to accomodate potential history changes. This is -1 if the indexing has just started.
	 */
	private final static ByteIterable BASE = ByteIterable.fromBytes("base of definitely indexed chain".getBytes());

	private final static TransactionReference[] NO_TXS = new TransactionReference[0];

	private final static Logger LOGGER = Logger.getLogger(Indexer.class.getName());

	private final static String LOG_PREFIX = "index: ";

	/**
	 * Creates an indexer for the given node.
	 * 
	 * @param node the node
	 * @param store the database store of the node, where indexing data are kept
	 * @param env the database environment of the node, where indexing data are kept
	 * @param size the size of the index; this is the maximal number of affecting transactions
	 *             kept in the index for each object
	 */
	Indexer(MokamintNode<?> node, Store store, Environment env, int size) {
		this.node = node;
		this.store = store;
		this.env = env;
		this.index = new Index(store, env, size);

		try {
			this.sha256 = HashingAlgorithms.sha256().getHasher(bytes -> bytes);
		}
		catch (NoSuchAlgorithmException e) {
			throw new LocalNodeException(e);
		}
	}

	/**
	 * Performs the indexing task. This is a repeating, potentially infinite task,
	 * until thread interruption. Indexing gets repeated after a time interval.
	 */
	void run() {
		try {
			while (true) {
				env.executeInTransaction(this::indexing);
				Thread.sleep(node.getLocalConfig().getIndexingPause());
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warning(LOG_PREFIX + "the indexing thread has been interrupted");
		}
		catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, LOG_PREFIX + "the indexing thread exits because of an exception", e);
		}
	}

	/**
	 * The indexing transaction on the database.
	 * 
	 * @param txn the database transaction where database updates are accumulated.
	 */
	private void indexing(io.hotmoka.xodus.env.Transaction txn) {
		// the Mokamint node has not been set yet
		var maybeEngine = node.getMokamintEngine();
		if (maybeEngine.isEmpty())
			return;

		var engine = maybeEngine.get();
		long base = getBase(txn);
		long height = base;
		long depth = node.getLocalConfig().getIndexingDepth();

		try {
			// we iterate on the blocks of the blockchain, from height base + 1 upwards
			Iterator<byte[]> it = new BlockHashesIterator(base + 1);
			while (it.hasNext()) {
				byte[] hash = it.next();
				height++;

				if (!containsBlockHash(hash, txn)) {
					// we remove all transactions form height upwards (if any) since
					// this might be a change in history; we also require to delete data from the index
					for (long cursor = height; unbind(cursor, true, txn); cursor++) {
						final long cursorCopy = cursor;
						//System.out.println("unbound supporting data at height " + cursorCopy + " because of a history change");
						LOGGER.info(() -> LOG_PREFIX + "unbound supporting data at height " + cursorCopy + " because of a history change");
					}

					Optional<Block> maybeBlock = engine.getBlock(hash);
					if (maybeBlock.isEmpty())
						break; // we stop this indexing iteration since we miss information

					Block block = maybeBlock.get();

					TransactionReference[] transactions;

					if (block instanceof NonGenesisBlock ngb)
						transactions = ngb.getTransactions()
							.map(Transaction::getBytes)
							.map(sha256::hash)
							.map(TransactionReferences::of)
							.toArray(TransactionReference[]::new);
					else
						transactions = NO_TXS;

					// we fetch the responses computed for the transactions in the block
					var responses = new TransactionResponse[transactions.length];
					for (int pos = 0; pos < transactions.length; pos++)
						responses[pos] = node.getResponse(transactions[pos]);

					// we update the database only if we are sure that we could fetch all the responses;
					// this ensures that, in case of failure of getResponse(), the database remains consistent
					bind(height, block.getHash(), transactions, responses, txn);

					// if we have been indexing more than the maximal depth allowed for history changes,
					// we increase the base and remove old information, below the new base
					if (depth >= 0 && height == base + depth + 1) {
						setBase(++base, txn);

						// we do not require to delete data from the index, since this deep block is
						// considered as definitely indexed from now on
						unbind(base, false, txn);

						final long baseCopy = base;
						LOGGER.info(() -> LOG_PREFIX + "unbound supporting data at height " + baseCopy + " because it seems old enough to be stable");
						//System.out.println("unbound supporting data at height " + baseCopy + " because it seems old enough to be stable");
					}
				}
				// otherwise the block hash did not change wrt the previous indexing iteration
				// and we have nothing to do
			}
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			LOGGER.warning(LOG_PREFIX + "cannot index the store of the node further since the underlying Mokamint node is closed");
		}
		catch (PortionRejectedException e) {
			LOGGER.warning(LOG_PREFIX + "cannot index the store of the node further since the underlying Mokamint node rejected a request for a portion of its chain: " + e.getMessage());
		}
		catch (UnknownReferenceException e) {
			LOGGER.warning(LOG_PREFIX + "cannot index the store of the node further since it misses the response of a transaction: " + e.getMessage());
		}
		catch (ClosedNodeException e) {
			LOGGER.warning(LOG_PREFIX + "cannot index the store of the node further since the node is closed");
		}
		catch (TimeoutException e) {
			LOGGER.warning(LOG_PREFIX + "cannot index the store of the node further since the underlying Mokamint node is unresponsive");
		}
		catch (InterruptedException e) {
			Thread.currentThread().isInterrupted();
			LOGGER.warning(LOG_PREFIX + "the indexing transaction has been interrupted");
		}
	}

	/**
	 * An iterator over the hashes of the blocks, in blockchain, from a given height upwards.
	 * It queries the underlying Mokamint engine to load such hashes, as long as possible.
	 */
	private class BlockHashesIterator implements Iterator<byte[]> {

		/**
		 * The start height of the chunk of hashes in {@link #hashes}.
		 */
		private long start;

		/**
		 * The current cursor inside {@link #hashes}.
		 */
		private int pos;

		/**
		 * The last chunk of hashes that has been fetched from the underlying Mokamint engine.
		 */
		private byte[][] hashes;

		/**
		 * The number of block hashes that are fetched with a single call to the underlying Mokamint engine.
		 */
		private final int blockFetchingChunkSize;

		/**
		 * Creates the iterator, for the hashes of the blocks from {@code start} (included) upwards.
		 * 
		 * @param start the initial height of the required block hashes
		 */
		private BlockHashesIterator(long start) throws TimeoutException, InterruptedException, io.mokamint.node.api.ClosedNodeException, PortionRejectedException {
			this.start = start;
			this.pos = 0;
			this.blockFetchingChunkSize = Math.min(node.getMokamintEngine().get().getInfo().getMaxChainPortionLength(), 512);
			if (blockFetchingChunkSize < 2)
				throw new IndexException("The maximal chain portion length is too small for keeping an index");

			this.hashes = node.getMokamintEngine().get().getChainPortion(start, blockFetchingChunkSize).getHashes().toArray(byte[][]::new);
		}

		@Override
		public boolean hasNext() {
			return pos < hashes.length;
		}

		@Override
		public byte[] next() {
			byte[] result = hashes[pos++]; // safe because of hasNext()

			if (pos == hashes.length) {
				// we have reached the end of the current chunk of hashes: we fetch the next one;
				// we use an overlapping hash, so that we understand if we can trust the new chunk
				// to continue the previous one: otherwise, there has been a history change and we cannot proceed
				// further with this indexing iteration
				try {
					byte[][] nextHashes = node.getMokamintEngine().get().getChainPortion(start + blockFetchingChunkSize - 1, blockFetchingChunkSize).getHashes().toArray(byte[][]::new);

					if (nextHashes.length > 0 && Arrays.equals(nextHashes[0], result)) { // the chunks of hashes match over their overlapping
						hashes = nextHashes;
						start += blockFetchingChunkSize - 1;
						pos = 1; // we do not consider the overlapping hash, since we already did it
					}
					// otherwise, if the next chunk of hashes does not start with the last hash of the previous chunk, there must have been
					// a history change and we do not proceed further: next indexing will have a chance to go further
				}
				catch (PortionRejectedException e) {
					// this should not happen since we verified that blockFetchingChunkSize is not larger than the maximum allowed
					LOGGER.warning(LOG_PREFIX + "stopping indexing since it was impossible to fetch a chain portion from the Mokamint node: " + e.getMessage());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					LOGGER.warning(LOG_PREFIX + "indexing has been interrupted");
				}
				catch (TimeoutException | io.mokamint.node.api.ClosedNodeException e) {
					LOGGER.warning(LOG_PREFIX + "stopping indexing since the Mokamint node is unresponsive: " + e.getMessage());
				}
			}

			return result;
		}
	}

	private void setBase(long height, io.hotmoka.xodus.env.Transaction txn) {
		if (height >= 0)
			store.put(txn, BASE, longToByteIterable(height));
		else
			// no information means that the base is -1
			store.delete(txn, BASE);
	}

	private long getBase(io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable baseBI = store.get(txn, BASE);
		if (baseBI == null)
			return -1L;
		else
			return byteIterableToLong(baseBI);
	}

	/**
	 * Creates binding, in the database, from the height to the hash of the block at that height;
	 * and from the hash of the block to the transaction references inside that block;
	 * it also expands the index with information about the affected objects in such transactions.
	 * 
	 * @param height the height of the block
	 * @param blockHash the hash of the block
	 * @param transactions the transaction references of the transactions in the block
	 * @param responses the response of {@code transactions}
	 * @param txn the database transaction where the database updates get accumulated
	 */
	private void bind(long height, byte[] blockHash, TransactionReference[] transactions, TransactionResponse[] responses, io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable blockHashBI = ByteIterable.fromBytes(blockHash);
		store.put(txn, longToByteIterable(height), blockHashBI);
		store.put(txn, blockHashBI, new MarshallableArrayOfTransactionReferences(transactions).toByteIterable());

		// we also add the transactions in the index
		for (int pos = 0; pos < transactions.length; pos++)
			index.add(transactions[pos], responses[pos], txn);
	}

	/**
	 * Checks if there is a mapping, in the database, from the given block hash.
	 * 
	 * @param blockHash the block hash
	 * @param txn the database transaction where the check is performed
	 * @return true if and only that condition holds
	 */
	private boolean containsBlockHash(byte[] blockHash, io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable blockHashBI = ByteIterable.fromBytes(blockHash);
		return store.get(txn, blockHashBI) != null;
	}

	/**
	 * Forgets indexing information at the given block height.
	 * 
	 * @param height the height of the block
	 * @param alsoFromIndex if true, the index gets modified by removing information about
	 *                      the transactions that were performed by the block at that height
	 * @param txn the database transaction where the database updates get accumulated
	 * @return true if and only if there was actually a binding for the block at the given height
	 */
	private boolean unbind(long height, boolean alsoFromIndex, io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable heightBI = longToByteIterable(height);
		ByteIterable blockHashBI = store.get(txn, heightBI);

		if (blockHashBI != null) {
			store.delete(txn, heightBI);
			ByteIterable transactionsBI = store.get(txn, blockHashBI);
			store.delete(txn, blockHashBI);

			if (alsoFromIndex)
				// we also remove the transactions from the index
				new MarshallableArrayOfTransactionReferences(transactionsBI).stream()
					.forEach(transaction -> index.remove(transaction, txn));

			return true;
		}
		else
			return false;
	}

	private static ByteIterable longToByteIterable(long l) {
		var result = new byte[Long.BYTES];
	
		for (int i = Long.BYTES - 1; i >= 0; i--) {
	        result[i] = (byte) (l & 0xFF);
	        l >>= Byte.SIZE;
	    }
	
	    return ByteIterable.fromBytes(result);
	}

	private static long byteIterableToLong(ByteIterable bi) {
	    long result = 0;
	    byte[] b = bi.getBytes();

	    for (int i = 0; i < Long.BYTES; i++) {
	        result <<= Byte.SIZE;
	        result |= (b[i] & 0xFF);
	    }
	
	    return result;
	}
}