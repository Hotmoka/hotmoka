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

package io.hotmoka.node.local.internal.store.trie;

import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.StoreCacheImpl;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * Partial implementation of a store of a node, based on tries. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe. Its states are arrays of 128 bytes.
 * It uses an array of 0's to represent the empty store.
 * 
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractTrieBasedStoreImpl<S extends AbstractTrieBasedStoreImpl<S, T>, T extends AbstractTrieBasedStoreTransformationImpl<S, T>> extends AbstractStore<S, T> implements CheckableStore<S, T> {

	/**
	 * The Xodus environment that holds the store.
	 */
	private final Environment env;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The Xodus store that holds miscellaneous information about the store.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfRequests;

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistories;

	/**
	 * The root of the trie of the responses.
	 */
	private final byte[] rootOfResponses;

	/**
	 * The root of the trie of the miscellaneous info.
	 */
	private final byte[] rootOfInfo;

	/**
	 * The root of the trie of the requests.
	 */
	private final byte[] rootOfRequests;

	/**
	 * The root of the trie of histories.
	 */
	private final byte[] rootOfHistories;

	/**
	 * Creates a store.
	 * 
	 * @param env the Xodus environment to use for creating the tries
	 * @param executors the executors to use for running transactions
	 * @param consensus the consensus configuration of the node having the store
	 * @param config the local configuration of the node having the store
	 * @param hasher the hasher for computing the transaction reference from the requests
	 */
    protected AbstractTrieBasedStoreImpl(Environment env, ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
    	super(executors, consensus, config, hasher);

    	this.env = env;

		var storeOfInfo = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfResponses = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfRequests = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfHistories = new AtomicReference<io.hotmoka.xodus.env.Store>();

		env.executeInTransaction(txn -> {
			storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
			storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
			storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			storeOfHistories.set(env.openStoreWithoutDuplicates("history", txn));
		});

    	this.storeOfResponses = storeOfResponses.get();
    	this.storeOfInfo = storeOfInfo.get();
		this.storeOfRequests = storeOfRequests.get();
		this.storeOfHistories = storeOfHistories.get();
		this.rootOfResponses = new byte[32];
		this.rootOfInfo = new byte[32];
		this.rootOfRequests = new byte[32];
		this.rootOfHistories = new byte[32];
    }

	/**
	 * Creates a clone of a store.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 * @param rootOfResponse the root to use for the tries of responses
	 * @param rootOfInfo the root to use for the tries of infos
	 * @param rootOfHistories the root to use for the tries of histories
	 * @param rootOfRequests the root to use for the tries of requests
	 */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<S, T> toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache);

    	this.env = toClone.env;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;
    	this.storeOfHistories = toClone.storeOfHistories;
    	this.storeOfRequests = toClone.storeOfRequests;
    	this.rootOfResponses = rootOfResponses;
    	this.rootOfInfo = rootOfInfo;
    	this.rootOfHistories = rootOfHistories;
    	this.rootOfRequests = rootOfRequests;
    }

	/**
	 * Creates a clone of store.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<S, T> toClone, StoreCache cache) {
    	super(toClone, cache);

    	this.env = toClone.env;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;
    	this.storeOfHistories = toClone.storeOfHistories;
    	this.storeOfRequests = toClone.storeOfRequests;
    	this.rootOfResponses = toClone.rootOfResponses;
    	this.rootOfInfo = toClone.rootOfInfo;
    	this.rootOfHistories = toClone.rootOfHistories;
    	this.rootOfRequests = toClone.rootOfRequests;
    }

    @Override
	protected S addDelta(StoreCache cache, LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest) throws StoreException {
	
		try {
			return CheckSupplier.check(StoreException.class, TrieException.class, () -> env.computeInTransaction(UncheckFunction.uncheck(txn -> {
				var trieOfRequests = mkTrieOfRequests(txn);
				for (var entry: addedRequests.entrySet())
					trieOfRequests = trieOfRequests.put(entry.getKey(), entry.getValue());
	
				var trieOfResponses = mkTrieOfResponses(txn);
				for (var entry: addedResponses.entrySet())
					trieOfResponses = trieOfResponses.put(entry.getKey(), entry.getValue());
	
				var trieOfHistories = mkTrieOfHistories(txn);
				for (var entry: addedHistories.entrySet())
					trieOfHistories = trieOfHistories.put(entry.getKey(), Stream.of(entry.getValue()));
	
				var trieOfInfo = mkTrieOfInfo(txn);
				trieOfInfo = trieOfInfo.increaseHeight();
				if (addedManifest.isPresent())
					trieOfInfo = trieOfInfo.setManifest(addedManifest.get());

				// we increment the reference count of the roots of the resulting tries, so that
				// they do not get garbage collected until this store is freed
				trieOfResponses.incrementReferenceCountOfRoot();
				trieOfInfo.incrementReferenceCountOfRoot();
				trieOfHistories.incrementReferenceCountOfRoot();
				trieOfRequests.incrementReferenceCountOfRoot();

				return make(cache, trieOfResponses.getRoot(), trieOfInfo.getRoot(), trieOfHistories.getRoot(), trieOfRequests.getRoot());
			})));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

    protected abstract S make(StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests);

    @Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	try {
    		return CheckSupplier.check(TrieException.class, StoreException.class, () ->
    			env.computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfRequests(txn).get(reference)))
    		)
    		.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
    public TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	try {
    		return CheckSupplier.check(TrieException.class, StoreException.class, () ->
    			env.computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfResponses(txn).get(reference)))
    		)
    		.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Optional<StorageReference> getManifest() throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, () ->
				env.computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getManifest())
			));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws StoreException, UnknownReferenceException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, () -> env.computeInReadonlyTransaction
				(UncheckFunction.uncheck(txn -> mkTrieOfHistories(txn).get(object))))
					.orElseThrow(() -> new UnknownReferenceException(object));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public byte[] getStateId() {
		var result = new byte[128];
		System.arraycopy(rootOfResponses, 0, result, 0, 32);
		System.arraycopy(rootOfInfo, 0, result, 32, 32);
		System.arraycopy(rootOfRequests, 0, result, 64, 32);
		System.arraycopy(rootOfHistories, 0, result, 96, 32);

		return result;
	}

	@Override
	public S checkoutAt(byte[] stateId) throws StoreException {
		var rootOfResponses = new byte[32];
		System.arraycopy(stateId, 0, rootOfResponses, 0, 32);
		var rootOfInfo = new byte[32];
		System.arraycopy(stateId, 32, rootOfInfo, 0, 32);
		var rootOfRequests = new byte[32];
		System.arraycopy(stateId, 64, rootOfRequests, 0, 32);
		var rootOfHistories = new byte[32];
		System.arraycopy(stateId, 96, rootOfHistories, 0, 32);

		try {
			return make(new StoreCacheImpl(ValidatorsConsensusConfigBuilders.defaults().build()), rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests)
					.initCaches();
		}
		catch (NoSuchAlgorithmException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public void free() throws StoreException {
		try {
			CheckRunnable.check(StoreException.class, TrieException.class, () -> env.executeInTransaction(UncheckConsumer.uncheck(txn -> {
				mkTrieOfRequests(txn).free();
				mkTrieOfResponses(txn).free();
				mkTrieOfHistories(txn).free();
				mkTrieOfInfo(txn).free();
			})));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public long getHeight() throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, () ->
				env.computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getHeight())
			));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfResponses mkTrieOfResponses(Transaction txn) throws StoreException {
		try {
			return new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfInfo mkTrieOfInfo(Transaction txn) throws StoreException {
		try {
			return new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfRequests mkTrieOfRequests(Transaction txn) throws StoreException {
		try {
			return new TrieOfRequests(new KeyValueStoreOnXodus(storeOfRequests, txn), rootOfRequests);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	protected TrieOfHistories mkTrieOfHistories(Transaction txn) throws StoreException {
		try {
			return new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Determines if all roots of the tries in this store are empty.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected boolean isEmpty() {
		return isEmpty(rootOfResponses) && isEmpty(rootOfInfo) && isEmpty(rootOfRequests) && isEmpty(rootOfHistories);
	}

	private static boolean isEmpty(byte[] hash) {
		for (byte b: hash)
			if (b != 0)
				return false;

		return true;
	}
}