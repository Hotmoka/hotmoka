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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.StoreCacheImpl;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Transaction;

/**
 * Partial implementation of a store of a node, based on tries. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe. Its states are arrays of 128 bytes.
 * It uses an array of 0's to represent the empty store.
 * 
 * @param <N> the type of the node having this store
 * @param <C> the type of the configuration of the node having this store
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractTrieBasedStoreImpl<N extends AbstractTrieBasedLocalNodeImpl<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractTrieBasedStoreImpl<N,C,S,T>, T extends AbstractTrieBasedStoreTransformationImpl<N,C,S,T>> extends AbstractStore<N,C,S,T> implements CheckableStore<S, T> {

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
	 * Creates an empty store.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    protected AbstractTrieBasedStoreImpl(N node) throws StoreException {
    	this(node, StateIds.of(new byte[128]));
    }

    /**
	 * Creates a store checked out at the given state identifier.
	 * 
	 * @param node the node for which the store is created
	 * @param stateId the state identifier
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    protected AbstractTrieBasedStoreImpl(N node, StateId stateId) throws StoreException {
    	super(node);

		byte[] bytes = stateId.getBytes();
		this.rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		this.rootOfInfo = new byte[32];
		System.arraycopy(bytes, 32, rootOfInfo, 0, 32);
		this.rootOfRequests = new byte[32];
		System.arraycopy(bytes, 64, rootOfRequests, 0, 32);
		this.rootOfHistories = new byte[32];
		System.arraycopy(bytes, 96, rootOfHistories, 0, 32);
    }

    /**
	 * Creates a clone of a store, up to cache and roots.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 * @param rootOfResponse the root to use for the tries of responses
	 * @param rootOfInfo the root to use for the tries of infos
	 * @param rootOfHistories the root to use for the tries of histories
	 * @param rootOfRequests the root to use for the tries of requests
	 */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<N,C,S,T> toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache);

    	this.rootOfResponses = rootOfResponses;
    	this.rootOfInfo = rootOfInfo;
    	this.rootOfHistories = rootOfHistories;
    	this.rootOfRequests = rootOfRequests;
    }

	/**
	 * Creates a clone of store, up to cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<N,C,S,T> toClone, StoreCache cache) {
    	this(toClone, cache, toClone.rootOfResponses, toClone.rootOfInfo, toClone.rootOfHistories, toClone.rootOfRequests);
    }

    @Override
	protected S addDelta(StoreCache cache, LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest) throws StoreException {
	
		try {
			return CheckSupplier.check(StoreException.class, TrieException.class, () -> getNode().getEnvironment().computeInTransaction(UncheckFunction.uncheck(txn -> {
				var trieOfRequests = mkTrieOfRequests(txn);
				for (var entry: addedRequests.entrySet()) {
					trieOfRequests.malloc();
					var old = trieOfRequests;
					trieOfRequests = trieOfRequests.put(entry.getKey(), entry.getValue());
					old.free();
				}
	
				var trieOfResponses = mkTrieOfResponses(txn);
				for (var entry: addedResponses.entrySet()) {
					trieOfResponses.malloc();
					var old = trieOfResponses;
					trieOfResponses = trieOfResponses.put(entry.getKey(), entry.getValue());
					old.free();
				}

				var trieOfHistories = mkTrieOfHistories(txn);
				for (var entry: addedHistories.entrySet()) {
					trieOfHistories.malloc();
					var old = trieOfHistories;
					trieOfHistories = trieOfHistories.put(entry.getKey(), Stream.of(entry.getValue()));
					old.free();
				}
	
				var trieOfInfo = mkTrieOfInfo(txn);
				trieOfInfo.malloc();
				var old = trieOfInfo;
				trieOfInfo = trieOfInfo.increaseHeight();
				old.free();
				if (addedManifest.isPresent()) {
					trieOfInfo.malloc();
					old = trieOfInfo;
					trieOfInfo = trieOfInfo.setManifest(addedManifest.get());
					old.free();
				}

				return mkStore(cache, trieOfResponses.getRoot(), trieOfInfo.getRoot(), trieOfHistories.getRoot(), trieOfRequests.getRoot());
			})));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

    protected abstract S mkStore(StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests);

    @Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	try {
    		return CheckSupplier.check(TrieException.class, StoreException.class, () ->
    			getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfRequests(txn).get(reference)))
    		)
    		.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
    public final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	try {
    		return CheckSupplier.check(TrieException.class, StoreException.class, () ->
    			getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfResponses(txn).get(reference)))
    		)
    		.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final Optional<StorageReference> getManifest() throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, () ->
				getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getManifest())
			));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final Stream<TransactionReference> getHistory(StorageReference object) throws StoreException, UnknownReferenceException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, () -> getNode().getEnvironment().computeInReadonlyTransaction
				(UncheckFunction.uncheck(txn -> mkTrieOfHistories(txn).get(object))))
					.orElseThrow(() -> new UnknownReferenceException(object));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final long getHeight() throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, () ->
				getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getHeight())
			));
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final StateId getStateId() {
		var result = new byte[128];
		System.arraycopy(rootOfResponses, 0, result, 0, 32);
		System.arraycopy(rootOfInfo, 0, result, 32, 32);
		System.arraycopy(rootOfRequests, 0, result, 64, 32);
		System.arraycopy(rootOfHistories, 0, result, 96, 32);

		return StateIds.of(result);
	}

	@Override
	public final S checkedOutAt(StateId stateId) throws StoreException {
		var bytes = stateId.getBytes();
		var rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		var rootOfInfo = new byte[32];
		System.arraycopy(bytes, 32, rootOfInfo, 0, 32);
		var rootOfRequests = new byte[32];
		System.arraycopy(bytes, 64, rootOfRequests, 0, 32);
		var rootOfHistories = new byte[32];
		System.arraycopy(bytes, 96, rootOfHistories, 0, 32);

		// we provide an empty cache and then ask to reload it from the state of the resulting store
		return mkStore(new StoreCacheImpl(), rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests).reloadCache();
	}

	/**
	 * Allocates the resources used for the checked-out vision of this store.
	 * 
	 * @param txn the database transaction where the operation is performed
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected final void malloc(Transaction txn) throws StoreException {
		var trieOfRequests = mkTrieOfRequests(txn);
		var trieOfResponses = mkTrieOfResponses(txn);
		var trieOfHistories = mkTrieOfHistories(txn);
		var trieOfInfo = mkTrieOfInfo(txn);

		try {
			// we increment the reference count of the roots of the resulting tries, so that
			// they do not get garbage collected until this store is freed
			trieOfResponses.malloc();
			trieOfInfo.malloc();
			trieOfHistories.malloc();
			trieOfRequests.malloc();
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Deallocates all resources used for the checked-out vision of this store.
	 * 
	 * @param txn the database transaction where the operation is performed
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected final void free(Transaction txn) throws StoreException {
		try {
			mkTrieOfRequests(txn).free();
			mkTrieOfResponses(txn).free();
			mkTrieOfHistories(txn).free();
			mkTrieOfInfo(txn).free();
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
	protected final boolean isEmpty() {
		return isEmpty(rootOfResponses) && isEmpty(rootOfInfo) && isEmpty(rootOfRequests) && isEmpty(rootOfHistories);
	}

	private TrieOfResponses mkTrieOfResponses(Transaction txn) throws StoreException {
		try {
			return new TrieOfResponses(new KeyValueStoreOnXodus(getNode().getStoreOfResponses(), txn), rootOfResponses);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	private TrieOfInfo mkTrieOfInfo(Transaction txn) throws StoreException {
		try {
			return new TrieOfInfo(new KeyValueStoreOnXodus(getNode().getStoreOfInfo(), txn), rootOfInfo);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	private TrieOfRequests mkTrieOfRequests(Transaction txn) throws StoreException {
		try {
			return new TrieOfRequests(new KeyValueStoreOnXodus(getNode().getStoreOfRequests(), txn), rootOfRequests);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	private TrieOfHistories mkTrieOfHistories(Transaction txn) throws StoreException {
		try {
			return new TrieOfHistories(new KeyValueStoreOnXodus(getNode().getStoreOfHistories(), txn), rootOfHistories);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	private static boolean isEmpty(byte[] hash) {
		for (byte b: hash)
			if (b != 0)
				return false;

		return true;
	}
}