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
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.local.internal.StoreCacheImpl;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.api.UnknownKeyException;
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
    protected AbstractTrieBasedStoreImpl(N node, StoreCache cache) throws StoreException {
    	super(node, cache);

    	this.rootOfResponses = new byte[32];
    	this.rootOfInfo = new byte[32];
    	this.rootOfRequests = new byte[32];
    	this.rootOfHistories = new byte[32];
    }

    /**
     * Creates a clone of a store, up to cache and roots. It checks for the existence of the state.
     * 
     * @param toClone the store to clone
     * @param cache the cache to use in the cloned store
     * @param stateId the state identifier of the store to create
     * @throws UnknownStateIdException if the required state does not exist
     * @throws StoreException if the operation could not be completed correctly
     */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<N,C,S,T> toClone, StoreCache cache, StateId stateId) throws UnknownStateIdException, StoreException {
    	super(toClone, cache);
  
    	var bytes = stateId.getBytes();
		this.rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		this.rootOfInfo = new byte[32];
		System.arraycopy(bytes, 32, rootOfInfo, 0, 32);
		this.rootOfRequests = new byte[32];
		System.arraycopy(bytes, 64, rootOfRequests, 0, 32);
		this.rootOfHistories = new byte[32];
		System.arraycopy(bytes, 96, rootOfHistories, 0, 32);

    	checkExistence();
    }

    /**
     * Creates a clone of a store, up to cache and roots. It does not check for the existence of the state.
     * 
     * @param toClone the store to clone
     * @param cache the cache to use in the cloned store
     * @param rootOfResponse the root to use for the tries of responses
     * @param rootOfInfo the root to use for the tries of info
     * @param rootOfHistories the root to use for the tries of histories
     * @param rootOfRequests the root to use for the tries of requests
     */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<N,C,S,T> toClone, StoreCache cache,
    		byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {

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

	protected StateId addDelta(StoreCache cache, LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest, Transaction txn) throws StoreException {

		try {
			var rootOfRequests = addDeltaOfRequests(mkTrieOfRequests(txn), addedRequests);
			var rootOfResponses = addDeltaOfResponses(mkTrieOfResponses(txn), addedResponses);
			var rootOfHistories = addDeltaOfHistories(mkTrieOfHistories(txn), addedHistories);
			var rootOfInfo = addDeltaOfInfos(mkTrieOfInfo(txn), addedManifest);

			var result = new byte[128];
			System.arraycopy(rootOfResponses, 0, result, 0, 32);
			System.arraycopy(rootOfInfo, 0, result, 32, 32);
			System.arraycopy(rootOfRequests, 0, result, 64, 32);
			System.arraycopy(rootOfHistories, 0, result, 96, 32);

			return StateIds.of(result);
		}
		catch (TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

	protected void checkExistence() throws UnknownStateIdException, StoreException {
		try {
			CheckRunnable.check(UnknownKeyException.class, StoreException.class, () -> getNode().getEnvironment().executeInReadonlyTransaction(UncheckConsumer.uncheck(txn -> {
				mkTrieOfRequests(txn);
				mkTrieOfResponses(txn);
				mkTrieOfHistories(txn);
				mkTrieOfInfo(txn);
			})));
		}
		catch (UnknownKeyException e) {
			throw new UnknownStateIdException();
		}
		catch (ExodusException e) {
			throw new StoreException(e);
		}
	}

	private byte[] addDeltaOfInfos(TrieOfInfo trieOfInfo, Optional<StorageReference> addedManifest) throws TrieException {
		if (addedManifest.isPresent()) {
			trieOfInfo.malloc();
			var old = trieOfInfo;
			trieOfInfo = trieOfInfo.setManifest(addedManifest.get());
			old.free();
		}

		return trieOfInfo.getRoot();
	}

	private byte[] addDeltaOfHistories(TrieOfHistories trieOfHistories, Map<StorageReference, TransactionReference[]> addedHistories) throws TrieException {
		for (var entry: addedHistories.entrySet()) {
			trieOfHistories.malloc();
			var old = trieOfHistories;
			trieOfHistories = trieOfHistories.put(entry.getKey(), Stream.of(entry.getValue()));
			old.free();
		}

		return trieOfHistories.getRoot();
	}

	private byte[] addDeltaOfResponses(TrieOfResponses trieOfResponses, Map<TransactionReference, TransactionResponse> addedResponses) throws TrieException {
		for (var entry: addedResponses.entrySet()) {
			trieOfResponses.malloc();
			var old = trieOfResponses;
			trieOfResponses = trieOfResponses.put(entry.getKey(), entry.getValue());
			old.free();
		}

		return trieOfResponses.getRoot();
	}

	private byte[] addDeltaOfRequests(TrieOfRequests trieOfRequests, LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests) throws TrieException {
		for (var entry: addedRequests.entrySet()) {
			trieOfRequests.malloc();
			var old = trieOfRequests;
			trieOfRequests = trieOfRequests.put(entry.getKey(), entry.getValue());
			old.free();
		}

		return trieOfRequests.getRoot();
	}

    protected abstract S mkStore(StoreCache cache, StateId stateId) throws StoreException, UnknownStateIdException;

    @Override
	protected abstract S setCache(StoreCache cache);

    @Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	try {
    		return CheckSupplier.check(TrieException.class, StoreException.class, UnknownKeyException.class, () ->
    			getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfRequests(txn).get(reference)))
    		)
    		.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

	@Override
    public final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	try {
    		return CheckSupplier.check(TrieException.class, StoreException.class, UnknownKeyException.class, () ->
    			getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfResponses(txn).get(reference)))
    		)
    		.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final Optional<StorageReference> getManifest() throws StoreException {
		try {
			return CheckSupplier.check(TrieException.class, StoreException.class, UnknownKeyException.class, () ->
				getNode().getEnvironment().computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> mkTrieOfInfo(txn).getManifest())
			));
		}
		catch (ExodusException | TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final Stream<TransactionReference> getHistory(StorageReference object) throws StoreException, UnknownReferenceException {
		try {
			return CheckSupplier.check(UnknownKeyException.class, StoreException.class, () -> getNode().getEnvironment().computeInReadonlyTransaction
				(UncheckFunction.uncheck(txn -> mkTrieOfHistories(txn).get(object))))
					.orElseThrow(() -> new UnknownReferenceException(object));
		}
		catch (ExodusException | UnknownKeyException e) {
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
	public final S checkedOutAt(StateId stateId) throws UnknownStateIdException, StoreException, InterruptedException {
		// we provide an empty cache and then ask to reload it from the state of the resulting store
		return checkedOutAt(stateId, new StoreCacheImpl()).reloadCache();
	}

	@Override
	public final S checkedOutAt(StateId stateId, StoreCache cache) throws UnknownStateIdException, StoreException {
		return mkStore(cache, stateId);
	}

	private TrieOfResponses mkTrieOfResponses(Transaction txn) throws StoreException, UnknownKeyException {
		return getNode().mkTrieOfResponses(txn, rootOfResponses);
	}

	private TrieOfInfo mkTrieOfInfo(Transaction txn) throws StoreException, UnknownKeyException {
		return getNode().mkTrieOfInfo(txn, rootOfInfo);
	}

	private TrieOfRequests mkTrieOfRequests(Transaction txn) throws StoreException, UnknownKeyException {
		return getNode().mkTrieOfRequests(txn, rootOfRequests);
	}

	private TrieOfHistories mkTrieOfHistories(Transaction txn) throws StoreException, UnknownKeyException {
		return getNode().mkTrieOfHistories(txn, rootOfHistories);
	}
}