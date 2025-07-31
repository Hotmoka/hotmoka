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
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Store;
import io.mokamint.node.api.Block;
import io.mokamint.node.api.NonGenesisBlock;
import io.mokamint.node.api.Transaction;

public class Indexer {
	private final MokamintNodeImpl mokamintNode;
	private final Store store;
	private final Environment env;
	private final Hasher<byte[]> sha256;
	private final Index index;
	private final static int MAX_DEPTH_OF_HISTORY_CHANGE = 20;
	private final static int BLOCK_LOADING_CHUNK_SIZE = 10;
	private final static ByteIterable BASE = ByteIterable.fromBytes("base of definitely indexed chain".getBytes());
	private final static Logger LOGGER = Logger.getLogger(Indexer.class.getName());

	public Indexer(MokamintNodeImpl mokamintNode, Store store, Environment env, int size) {
		this.mokamintNode = mokamintNode;
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

	public void run() {
		try {
			while (true) {
				env.executeInTransaction(txn -> {
					long base = getBase(txn);
					long height = base;

					try {
						Iterator<byte[]> it = new BlockHashesIterator(base);
						while (it.hasNext()) {
							byte[] hash = it.next();
							height++;

							if (!containsBlockHash(hash, txn)) {
								// we remove all transactions form height upwards (if any) since
								// this might be a change in history
								for (long cursor = height; unbind(cursor, true, txn); cursor++)
									System.out.println(cursor + ": - [history change]");

								Optional<Block> maybeBlock = mokamintNode.getMokamintNode().getBlock(hash);
								if (maybeBlock.isEmpty())
									break; // we stop this indexing iteration

								Block block = maybeBlock.get();

								TransactionReference[] transactions;
								if (block instanceof NonGenesisBlock ngb)
									transactions = ngb.getTransactions()
										.map(Transaction::getBytes)
										.map(sha256::hash)
										.map(TransactionReferences::of)
										.toArray(TransactionReference[]::new);
								else
									transactions = new TransactionReference[0];

								var responses = new TransactionResponse[transactions.length];
								for (int pos = 0; pos < transactions.length; pos++)
									responses[pos] = mokamintNode.getResponse(transactions[pos]);

								// we update the database only if we are sure that we could load all responses
								bind(height, block.getHash(), transactions, responses, txn);

								// we remove old information
								if (height == base + MAX_DEPTH_OF_HISTORY_CHANGE + 1) {
									setBase(++base, txn);
									unbind(base, false, txn);
									System.out.println(base + ": - [old]");
								}
							}
						}
					}
					catch (io.mokamint.node.api.ClosedNodeException e) {
						e.printStackTrace();
						LOGGER.warning("cannot index the store of the node further since the underlying Mokamint node is closed");
					}
					catch (UnknownReferenceException e) {
						e.printStackTrace();
						LOGGER.warning("cannot index the store of the node further since it misses the response of a transaction");
					}
					catch (ClosedNodeException e) {
						e.printStackTrace();
						LOGGER.warning("cannot index the store of the node further since the node is closed");
					}
					catch (TimeoutException e) {
						LOGGER.warning("cannot index the store of the node further since the underlying Mokamint node is unresponsive");
					}
					catch (InterruptedException e) {
						Thread.currentThread().isInterrupted();
					}
				});

				Thread.sleep(20_000L);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warning("The indexer thread has been interrupted");
		}
		catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "The indexer thread exits because of an exception", e);
		}
	}

	private class BlockHashesIterator implements Iterator<byte[]> {
		private long start;
		private int pos;
		private byte[][] hashes;

		private BlockHashesIterator(long base) throws TimeoutException, InterruptedException, io.mokamint.node.api.ClosedNodeException {
			this.start = base + 1;
			this.hashes = mokamintNode.getMokamintNode().getChainPortion(start, BLOCK_LOADING_CHUNK_SIZE).getHashes().toArray(byte[][]::new);
			this.pos = 0;
		}

		@Override
		public boolean hasNext() {
			return pos < hashes.length;
		}

		@Override
		public byte[] next() {
			byte[] result = hashes[pos++];

			if (pos == hashes.length) {
				// we charge the next chunk of hashes
				try {
					byte[][] nextHashes = mokamintNode.getMokamintNode()
						.getChainPortion(start + BLOCK_LOADING_CHUNK_SIZE - 1, BLOCK_LOADING_CHUNK_SIZE).getHashes().toArray(byte[][]::new);

					if (nextHashes.length > 0 && Arrays.equals(nextHashes[0], result)) {
						hashes = nextHashes;
						start += BLOCK_LOADING_CHUNK_SIZE - 1;
						pos = 1;
					}
					// otherwise, if the next chunk of hashes does not start with the last hash of the previous chunk, there must have been
					// a history change and we do not proceed further: next indexing will have a chance to go further
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					LOGGER.warning("Indexing has been interrupted");
				}
				catch (TimeoutException | io.mokamint.node.api.ClosedNodeException e) {
					LOGGER.warning("Stopping indexing since the Mokamint node is unresponsive: " + e.getMessage());
				}
			}

			return result;
		}
	}

	private void setBase(long height, io.hotmoka.xodus.env.Transaction txn) {
		if (height >= 0)
			store.put(txn, BASE, longToByteIterable(height));
		else
			// no information means that the bease is -1
			store.delete(txn, BASE);
	}

	private long getBase(io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable baseBI = store.get(txn, BASE);
		if (baseBI == null)
			return -1L;
		else
			return byteIterableToLong(baseBI);
	}

	private void bind(long height, byte[] blockHash, TransactionReference[] transactions, TransactionResponse[] responses, io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable blockHashBI = ByteIterable.fromBytes(blockHash);
		store.put(txn, longToByteIterable(height), blockHashBI);
		store.put(txn, blockHashBI, new MarshallableArrayOfTransactionReferences(transactions).toByteIterable());

		// we also add the transactions in the index
		for (int pos = 0; pos < transactions.length; pos++)
			index.add(transactions[pos], responses[pos], txn);
	}

	private boolean containsBlockHash(byte[] hash, io.hotmoka.xodus.env.Transaction txn) {
		ByteIterable blockHashBI = ByteIterable.fromBytes(hash);
		return store.get(txn, blockHashBI) != null;
	}

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