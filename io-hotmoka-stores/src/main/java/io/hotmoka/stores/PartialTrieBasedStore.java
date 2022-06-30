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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.local.AbstractLocalNode;
import io.hotmoka.local.AbstractStore;
import io.hotmoka.local.CheckableStore;
import io.hotmoka.local.Config;
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
public abstract class PartialTrieBasedStore<C extends Config> extends AbstractStore<C> {

	/**
	 * The Xodus environment that holds the store.
	 */
	protected final Environment env;

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

	/**
	 * The time when {@link #txn} was started, in the same format as {@link System#currentTimeMillis()}.
	 */
	private long now;

	/**
	 * Creates a store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(byte[])} or {@link #setRootsAsCheckedOut()}
	 * should occur, to set the roots of the store.
	 * 
	 * @param node the node having this store
	 */
    protected PartialTrieBasedStore(AbstractLocalNode<? extends C, ? extends PartialTrieBasedStore<? extends C>> node) {
    	super(node);

    	this.env = new Environment(config.dir + "/store");

    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfResponses = new AtomicReference<>();
    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfInfo = new AtomicReference<>();

    	recordTime(() -> env.executeInTransaction(txn -> {
    		storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
    		storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
    	}));

    	this.storeOfResponses = storeOfResponses.get();
    	this.storeOfInfo = storeOfInfo.get();
    }

    /**
	 * Builds a clone of the given store.
	 * 
	 * @param parent the store to clone
	 */
	protected PartialTrieBasedStore(PartialTrieBasedStore<? extends C> parent) {
		super(parent);

		this.env = parent.env;
		this.storeOfResponses = parent.storeOfResponses;
		this.storeOfInfo = parent.storeOfInfo;
		this.now = parent.now;
		System.arraycopy(parent.rootOfResponses, 0, this.rootOfResponses, 0, 32);
		System.arraycopy(parent.rootOfInfo, 0, this.rootOfInfo, 0, 32);
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
	public final long getNow() {
		return now;
	}

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return recordTimeSynchronized(() -> env.computeInReadonlyTransaction
			(txn -> new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses), !(this instanceof CheckableStore)).get(reference)));
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		synchronized (lock) {
			return duringTransaction() ? recordTime(() -> trieOfResponses.get(reference)) : getResponse(reference);
		}
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return recordTimeSynchronized(() -> env.computeInReadonlyTransaction
			(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo), !(this instanceof CheckableStore)).getManifest()));
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		synchronized (lock) {
			return duringTransaction() ? recordTime(trieOfInfo::getManifest) : getManifest();
		}
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		recordTime(() -> trieOfResponses.put(reference, response));
	}

	@Override
	protected void setManifest(StorageReference manifest) {
		recordTime(() -> trieOfInfo.setManifest(manifest));
	}

	/**
	 * Starts a transaction. All updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 * 
	 * @param now the time to use as starting moment of the transaction
	 */
	public void beginTransaction(long now) {
		synchronized (lock) {
			txn = recordTime(env::beginTransaction);
			trieOfResponses = new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses), !(this instanceof CheckableStore));
			trieOfInfo = new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo), !(this instanceof CheckableStore));
			this.now = now;
		}
	}

	/**
	 * Commits to the database all data put from the last call to {@link #beginTransaction(long)}.
	 * This does not change the view of the store, since its roots are not updated,
	 * unless the hash returned by this method gets later checked out to update the roots.
	 * 
	 * @return the hash of the store resulting at the end of all updates performed during the transaction;
	 *         if this gets checked out, the view of the store becomes that at the end of the transaction
	 */
	protected byte[] commitTransaction() {
		return recordTime(() -> {
			trieOfInfo.increaseNumberOfCommits();

			if (!txn.commit())
				logger.info("transaction's commit failed");

			return mergeRootsOfTries();
		});
	}

	/**
	 * Resets the store to the given root. This is just the concatenation of the roots
	 * of the tries in this store. For instance, as returned by a previous {@link #commitTransaction()}.
	 * 
	 * @param root the root to reset to
	 */
	protected void checkout(byte[] root) {
		setRootsTo(root);
		recordTime(() -> env.executeInTransaction(txn -> storeOfInfo.put(txn, ROOT, ByteIterable.fromBytes(root))));
	}

	/**
	 * Yields the number of commits already performed over this store.
	 * 
	 * @return the number of commits
	 */
	public long getNumberOfCommits() {
		return recordTime(() -> env.computeInReadonlyTransaction
			(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo), !(this instanceof CheckableStore)).getNumberOfCommits()));
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
		recordTime(() -> env.executeInTransaction(txn -> {
			ByteIterable root = storeOfInfo.get(txn, ROOT);
			setRootsTo(root == null ? null : root.getBytes());
		}));
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
			return recordTime(() -> env.computeInReadonlyTransaction(txn -> storeOfInfo.get(txn, ROOT).getBytes()));

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
		try {
			return ByteIterable.fromBytes(reference.toByteArrayWithoutSelector()); // more optimized than a normal marshallable
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected static ByteIterable intoByteArray(Marshallable[] marshallables) throws UncheckedIOException {
		try {
			return ByteIterable.fromBytes(Marshallable.toByteArray(marshallables));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected static <T extends Marshallable> T[] fromByteArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier, ByteIterable bytes) throws UncheckedIOException {
		try (UnmarshallingContext context = new UnmarshallingContext(new ByteArrayInputStream(bytes.getBytes()))) {
			return context.readArray(unmarshaller, supplier);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}
}