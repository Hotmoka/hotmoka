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

package io.hotmoka.stores;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.BeanUnmarshallingContexts;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.stores.internal.TrieOfInfo;
import io.hotmoka.stores.internal.TrieOfResponses;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests nor errors (for this reason it is <i>partial</i>).
 * Its implementation is based on Merkle-Patricia tries,
 * supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka request reference to the response computed for that request
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 * 
 * This class is meant to be subclassed by specifying where errors, requests and histories are kept.
 */
@ThreadSafe
public abstract class PartialTrieBasedStore extends AbstractStore {

	/**
	 * The Xodus environment that holds the store.
	 */
	protected final Environment env;

	/**
	 * The number of last commits that can be checked out, in order to
	 * change the world-view of the store (see {@link #checkout(byte[])}).
	 * This entails that such commits are not garbage-collected. until
	 * new commits get created on top and they end up being deeper.
	 * This is useful if we expect an old state to be checked out, for
	 * instance because a blockchain swaps to another history, but we can
	 * assume a reasonable depth for that to happen. Use 0 if the store
	 * is not checkable, so that all its successive commits can be immediately
	 * garbage-collected as soon as a new commit is created on top
	 * (which corresponds to a blockchain that never swaps to a previous
	 * state, because it has deterministic finality).
	 */
	private final long checkableDepth;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The Xodus store that holds miscellaneous information about the store.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

	/**
	 * The root of the trie of the responses. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfResponses = new byte[32];

	/**
	 * The root of the trie of the miscellaneous info. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfInfo = new byte[32];

	/**
	 * The key used inside {@link #storeOfInfo} to keep the root.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromBytes("root".getBytes());

	/**
	 * The transaction that accumulates all changes to commit.
	 */
	private Transaction txn;

	/**
     * The trie of the responses.
     */
	private TrieOfResponses trieOfResponses;

	/**
	 * The trie for the miscellaneous information.
	 */
	private TrieOfInfo trieOfInfo;

	private final static Logger logger = Logger.getLogger(PartialTrieBasedStore.class.getName());

	/**
	 * Creates a store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(byte[])} or {@link #setRootsAsCheckedOut()}
	 * should occur, to set the roots of the store.
	 * 
	 * @param getResponseUncommittedCached a function that yields the transaction response for the given transaction reference, if any, using a cache
 	 * @param dir the path where the database of the store gets created
	 * @param checkableDepth the number of last commits that can be checked out, in order to
	 *                       change the world-view of the store (see {@link #checkout(byte[])}).
	 *                       This entails that such commits are not garbage-collected, until
	 *                       new commits get created on top and they end up being deeper.
	 *                       This is useful if we expect an old state to be checked out, for
	 *                       instance because a blockchain swaps to another history, but we can
	 *                       assume a reasonable depth for that to happen. Use 0 if the store
	 *                       is not checkable, so that all its successive commits can be immediately
	 *                       garbage-collected as soon as a new commit is created on top
	 *                       (which corresponds to a blockchain that never swaps to a previous
	 *                       state, because it has deterministic finality). Use a negative
	 *                       number if all commits must be checkable (hence garbage-collection
	 *                       is disabled)
	 */
    protected PartialTrieBasedStore(Function<TransactionReference, Optional<TransactionResponse>> getResponseUncommittedCached, Path dir, long checkableDepth) {
    	super(getResponseUncommittedCached, dir);

    	this.checkableDepth = checkableDepth;
    	this.env = new Environment(dir + "/store");

    	var storeOfResponses = new AtomicReference<io.hotmoka.xodus.env.Store>();
    	var storeOfInfo = new AtomicReference<io.hotmoka.xodus.env.Store>();

    	env.executeInTransaction(txn -> {
    		storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
    		storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
    	});

    	this.storeOfResponses = storeOfResponses.get();
    	this.storeOfInfo = storeOfInfo.get();
    }

	@Override
    public void close() {
    	if (duringTransaction()) {
    		// store closed with yet uncommitted transactions: we abort them
    		logger.log(Level.WARNING, "store closed with uncommitted transactions: they are being aborted");
    		txn.abort();
    	}

    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		logger.log(Level.WARNING, "failed to close environment", e);
    	}

    	super.close();
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
    	synchronized (lock) {
    		return env.computeInReadonlyTransaction
    			(txn -> new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses), -1L).get(reference));
    	}
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		synchronized (lock) {
			return duringTransaction() ? trieOfResponses.get(reference) : getResponse(reference);
		}
	}

	@Override
	public Optional<StorageReference> getManifest() {
		synchronized (lock) {
			return env.computeInReadonlyTransaction
				(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo), -1L).getManifest());
		}
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		synchronized (lock) {
			return duringTransaction() ? trieOfInfo.getManifest() : getManifest();
		}
	}

	/**
	 * Starts a transaction. Instance updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 * 
	 * @return the transaction
	 */
	public Transaction beginTransaction() {
		synchronized (lock) {
			txn = env.beginTransaction();
			long numberOfCommits = getNumberOfCommits();
			trieOfResponses = new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses), numberOfCommits);
			trieOfInfo = new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo), numberOfCommits);
			return txn;
		}
	}

	/**
	 * Commits to the database all data written from the last call to {@link #beginTransaction(long)}.
	 * This does not change the view of the store, since its roots are not updated,
	 * unless the hash returned by this method gets later checked out to update the roots.
	 * 
	 * @return the hash of the store resulting at the end of all updates performed during the transaction;
	 *         if this gets checked out, the view of the store becomes that at the end of the transaction
	 */
	public byte[] commitTransaction() {
		synchronized (lock) {
			long newCommitNumber = trieOfInfo.increaseNumberOfCommits();
	
			// a negative number means that garbage-collection is disabled
			if (checkableDepth >= 0L) {
				long commitToGarbageCollect = newCommitNumber - 1 - checkableDepth;
				if (commitToGarbageCollect >= 0L)
					garbageCollect(commitToGarbageCollect);
			}
	
			if (!txn.commit())
				logger.info("transaction's commit failed");
	
			return mergeRootsOfTries();
		}
	}

	/**
	 * Yields the number of commits already performed over this store.
	 * 
	 * @return the number of commits
	 */
	public long getNumberOfCommits() {
		return env.computeInReadonlyTransaction
			(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo), -1L).getNumberOfCommits());
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		trieOfResponses.put(reference, response);
	}

	@Override
	protected void setManifest(StorageReference manifest) {
		trieOfInfo.setManifest(manifest);
	}

	/**
	 * Garbage-collects all keys updated during the given commit.
	 * 
	 * @param commitNumber the number of the commit
	 */
	protected void garbageCollect(long commitNumber) {
		trieOfResponses.garbageCollect(commitNumber);
		trieOfInfo.garbageCollect(commitNumber);
	}

	/**
	 * Resets the store to the given root. This is just the concatenation of the roots
	 * of the tries in this store. For instance, as returned by a previous {@link #commitTransaction()}.
	 * 
	 * @param root the root to reset to
	 */
	protected void checkout(byte[] root) {
		setRootsTo(root);
		var rootAsBI = ByteIterable.fromBytes(root);
		env.executeInTransaction(txn -> storeOfInfo.put(txn, ROOT, rootAsBI));
	}

	/**
	 * Yields the Xodus transaction active between {@link #beginTransaction(long)} and
	 * {@link #commitTransaction()}. This is where store updates must be written.
	 * 
	 * @return the transaction
	 */
	protected final Transaction getCurrentTransaction() {
		return txn;
	}

	/**
	 * Determines if the store is between a {@link #beginTransaction(long)} and a
	 * {@link #commitTransaction()}.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean duringTransaction() {
		return txn != null && !txn.isFinished();
	}

	/**
	 * Sets the roots of the tries in this store to the previously checked out ones.
	 */
	protected final void setRootsAsCheckedOut() {
		ByteIterable root = env.computeInReadonlyTransaction(txn -> storeOfInfo.get(txn, ROOT));
		setRootsTo(root == null ? null : root.getBytes());
	}

	/**
	 * Sets the roots of this store to the given (merged) root.
	 * 
	 * @param root the merged root
	 */
	protected void setRootsTo(byte[] root) {
		if (root == null) {
			Arrays.fill(rootOfResponses, (byte) 0);
			Arrays.fill(rootOfInfo, (byte) 0);
		}
		else {
			System.arraycopy(root, 0, rootOfResponses, 0, 32);
			System.arraycopy(root, 32, rootOfInfo, 0, 32);
		}
	}

	/**
	 * Yields the concatenation of the roots of the tries in this store,
	 * resulting after all updates performed to the store. Hence, they point
	 * to the latest view of the store.
	 * 
	 * @return the concatenation
	 */
	protected byte[] mergeRootsOfTries() {
		// this can be null if this is called before any new transaction has been executed over this store
		if (trieOfResponses == null)
			return env.computeInReadonlyTransaction(txn -> storeOfInfo.get(txn, ROOT).getBytes());

		byte[] result = new byte[64];

		byte[] rootOfResponses = trieOfResponses.getRoot();
		if (rootOfResponses != null)
			System.arraycopy(rootOfResponses, 0, result, 0, 32);
	
		byte[] rootOfInfo = trieOfInfo.getRoot();
		if (rootOfInfo != null)
			System.arraycopy(rootOfInfo, 0, result, 32, 32);
	
		return result;
	}

	/**
	 * Determines if all roots of the tries in this store are empty
	 * (sequence of 0's).
	 * 
	 * @return true if and only if that condition holds
	 */
	protected boolean isEmpty() {
		return isEmpty(rootOfResponses) && isEmpty(rootOfInfo);
	}

	/**
	 * Yields the given hash, if non-empty, or otherwise {@code null}.
	 * 
	 * @param hash the hash
	 * @return {@code hash}, if non-empty, or otherwise {@code null}
	 */
	protected static byte[] nullIfEmpty(byte[] hash) {
		return isEmpty(hash) ? null : hash;
	}

	/**
	 * Checks that all positions of the given array hold 0.
	 * 
	 * @param array the array
	 * @return true if and only if that condition holds
	 */
	protected static boolean isEmpty(byte[] array) {
		for (byte b: array)
			if (b != (byte) 0)
				return false;

		return true;
	}

	protected static ByteIterable intoByteArray(StorageReference reference) throws UncheckedIOException {
		return ByteIterable.fromBytes(reference.toByteArrayWithoutSelector()); // more optimized than a normal marshallable
	}

	protected static TransactionReference[] fromByteArray(ByteIterable bytes) throws UncheckedIOException {
		try (var context = BeanUnmarshallingContexts.of(new ByteArrayInputStream(bytes.getBytes()))) {
			return context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}