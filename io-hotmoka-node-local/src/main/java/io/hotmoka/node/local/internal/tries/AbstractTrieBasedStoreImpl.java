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
import io.hotmoka.exceptions.functions.ConsumerWithExceptions2;
import io.hotmoka.exceptions.functions.FunctionWithExceptions2;
import io.hotmoka.exceptions.functions.FunctionWithExceptions3;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
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
	 * Creates an empty store, with empty cache.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    protected AbstractTrieBasedStoreImpl(N node) throws StoreException {
    	super(node);

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
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<N,C,S,T> toClone, StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, StoreException {
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
	 * Creates a clone of a store, up to cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
     * @throws StoreException if the operation cannot be completed correctly
	 */
    protected AbstractTrieBasedStoreImpl(AbstractTrieBasedStoreImpl<N,C,S,T> toClone, StoreCache cache) throws StoreException {
    	super(toClone, Optional.of(cache));

    	this.rootOfResponses = toClone.rootOfResponses;
    	this.rootOfInfo = toClone.rootOfInfo;
    	this.rootOfHistories = toClone.rootOfHistories;
    	this.rootOfRequests = toClone.rootOfRequests;
    }

	protected final StateId addDelta(StoreCache cache, LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests,
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

	/**
	 * Yields a clone of this store, but for its cache, that is initialized with information extracted from this store.
	 * 
	 * @return the resulting store
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected final S withReloadedCache() throws StoreException, InterruptedException {
		StoreCache newCache = getCache();
	
		// if this store is already initialized, we can extract the cache information
		// from the store itself, otherwise the previous cache will be kept
		Optional<StorageReference> maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			StorageReference manifest = maybeManifest.get();

			newCache = newCache
				.setConfig(extractConsensus(manifest))
				.invalidateClassLoaders()
				.setValidators(extractValidators(manifest))
				.setGasStation(extractGasStation(manifest))
				.setVersions(extractVersions(manifest))
				.setGasPrice(extractGasPrice(manifest))
				.setInflation(extractInflation(manifest));
		}
	
		return withCache(newCache);
	}

    @Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	FunctionWithExceptions3<Transaction, Optional<TransactionRequest<?>>, TrieException, StoreException, UnknownKeyException> function = txn -> mkTrieOfRequests(txn).get(reference);
    	try {
    		return getNode().getEnvironment().computeInReadonlyTransaction(TrieException.class, StoreException.class, UnknownKeyException.class, function)
    			.orElseThrow(() -> new UnknownReferenceException(reference));
    	}
		catch (ExodusException | TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

    // TODO: this cache should be moved into the StoreCache, but only after the latter has become a store-local object, not shared among stores
    private final LRUCache<TransactionReference, TransactionResponse> getResponseCache = new LRUCache<>(100, 1_000);

    @Override
    public final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	// we use a cache since this is shown as a hotspot by the YourKit profiler
    	FunctionWithExceptions2<TransactionReference, TransactionResponse, UnknownReferenceException, StoreException> supplier = this::getResponseInternal;
		return getResponseCache.computeIfAbsent(reference, supplier, UnknownReferenceException.class, StoreException.class);
	}

    private TransactionResponse getResponseInternal(TransactionReference reference) throws UnknownReferenceException, StoreException {
    	FunctionWithExceptions3<Transaction, Optional<TransactionResponse>, TrieException, StoreException, UnknownKeyException> function = txn -> mkTrieOfResponses(txn).get(reference);

    	try {
    		var response = getNode().getEnvironment().computeInReadonlyTransaction(TrieException.class, StoreException.class, UnknownKeyException.class, function)
    			.orElseThrow(() -> new UnknownReferenceException(reference));

    		return response;
    	}
		catch (ExodusException | TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

    @Override
	public final Optional<StorageReference> getManifest() throws StoreException {
		FunctionWithExceptions3<Transaction, Optional<StorageReference>, TrieException, StoreException, UnknownKeyException> getManifest = txn -> mkTrieOfInfo(txn).getManifest();
		try {
			return getNode().getEnvironment().computeInReadonlyTransaction(TrieException.class, StoreException.class, UnknownKeyException.class, getManifest);
		}
		catch (ExodusException | TrieException | UnknownKeyException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final Stream<TransactionReference> getHistory(StorageReference object) throws StoreException, UnknownReferenceException {
		FunctionWithExceptions3<Transaction, Optional<Stream<TransactionReference>>, UnknownKeyException, StoreException, TrieException> getObject = txn -> mkTrieOfHistories(txn).get(object);
		try {
			return getNode().getEnvironment().computeInReadonlyTransaction(UnknownKeyException.class, StoreException.class, TrieException.class, getObject)
				.orElseThrow(() -> new UnknownReferenceException(object));
		}
		catch (ExodusException | UnknownKeyException | TrieException e) {
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

	private void checkExistence() throws UnknownStateIdException, StoreException {
		try {
			ConsumerWithExceptions2<Transaction, UnknownKeyException, StoreException> checkTriesExist = txn -> {
				mkTrieOfRequests(txn);
				mkTrieOfResponses(txn);
				mkTrieOfHistories(txn);
				mkTrieOfInfo(txn);
			};

			getNode().getEnvironment().executeInReadonlyTransaction(UnknownKeyException.class, StoreException.class, checkTriesExist);
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