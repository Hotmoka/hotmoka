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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.Index;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.patricia.api.UnknownKeyException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * Partial implementation of a local (ie., non-remote) node based on tries.
 * 
 * @param <N> the type of this node
 * @param <C> the type of the configuration of this node
 * @param <S> the type of the store of this node
 * @param <T> the type of the store transformations that can be started from the store of this node
 */
@ThreadSafe
public abstract class AbstractTrieBasedLocalNodeImpl<N extends AbstractTrieBasedLocalNodeImpl<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractTrieBasedStoreImpl<N,C,S,T>, T extends AbstractTrieBasedStoreTransformationImpl<N,C,S,T>> extends AbstractLocalNode<N,C,S,T> {

	/**
	 * The Xodus environment used for storing information about the node, such as its store.
	 */
	private final Environment env;

	/**
	 * The Xodus store that holds the persistent information of the node.
	 */
	private final io.hotmoka.xodus.env.Store storeOfNode;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

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
	 * The hash of the empty node in the tries.
	 */
	private final byte[] hashOfEmpty = new byte[32]; // TODO: reuse in the tries

	private final Index index;

	/**
	 * A map from each state identifier of the store to the number of users of that store
	 * at that state identifier. The goal of this information is to avoid garbage-collecting
	 * store identifiers that are currently being used for some reason.
	 * A negative value for a state identifier means that the store with that state identifier
	 * is being garbage-collected and must be considered as missing already.
	 */
	private final ConcurrentMap<StateId, Integer> storeUsers = new ConcurrentHashMap<>();

	/**
	 * The key used inside {@link #storeOfNode} to keep the list of old stores
	 * that are candidate for garbage-collection, if they are not used by any running task.
	 */
	private final static ByteIterable STORES_TO_GC = ByteIterable.fromBytes("stores to gc".getBytes(StandardCharsets.UTF_8));

	/**
	 * The key used inside {@link #storeOfNode} to keep the list of stores that this node
	 * will not try to garbage collect. This list gets expanded when a new head is added to the
	 * node and gets shrunk by calls to {@link #keepPersistedOnly(Stream)}.
	 */
	private final static ByteIterable STORES_NOT_TO_GC = ByteIterable.fromBytes("stores not to gc".getBytes(StandardCharsets.UTF_8));

	private final static Logger LOGGER = Logger.getLogger(AbstractTrieBasedLocalNodeImpl.class.getName());

	/**
	 * Creates a new node.
	 * 
	 * @param config the local configuration of the node
	 * @param init if true, the working directory of the node gets initialized
	 */
	protected AbstractTrieBasedLocalNodeImpl(C config, boolean init) {
		super(config, init);

		var path = config.getDir().resolve("hotmoka").resolve("store");

		this.env = new Environment(path.toString());
		this.storeOfNode = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("node", txn));
    	this.storeOfResponses = env.computeInTransaction(txn -> env.openStoreWithoutDuplicatesWithPrefixing("responses", txn));
		this.storeOfRequests = env.computeInTransaction(txn -> env.openStoreWithoutDuplicatesWithPrefixing("requests", txn));
		this.storeOfHistories = env.computeInTransaction(txn -> env.openStoreWithoutDuplicatesWithPrefixing("histories", txn));
		this.index = new Index(storeOfNode, env, config.getIndexSize());

		// we start the garbage-collection task
		getExecutors().execute(this::gc);

		LOGGER.info("opened the store database at " + path);
	}

	@Override
	public final Stream<TransactionReference> getIndex(StorageReference object) throws UnknownReferenceException, ClosedNodeException {
		try (var scope = mkScope()) {
			return index.get(object).orElseThrow(() -> new UnknownReferenceException(object));
		}
	}

	protected void addToIndex(TransactionReference transaction, TransactionResponse response, Transaction txn) {
		index.add(transaction, response, txn);
	}

	protected void removeFromIndex(TransactionReference transaction, Transaction txn) {
		index.remove(transaction, txn);
	}

	protected final io.hotmoka.xodus.env.Store getStoreOfNode() {
		return storeOfNode;
	}

	protected Environment getEnvironment() {
		return env;
	}

	@Override
	protected void closeResources() {
		try {
			super.closeResources();
		}
		finally {
			try {
				env.close();
			}
			catch (ExodusException e) {
				LOGGER.log(Level.SEVERE, "Failed to close the Exodus environment", e);
			}
		}
	}

	@Override
	protected final S enterHead() throws ClosedNodeException, InterruptedException, TimeoutException {
		while (true) {
			S head = getStoreOfHead();

			// if the store of the head is currently being garbage-collected (negative mark), it means that the
			// garbage-collection has been so aggressive to garbage-collect the head while we were trying
			// to enter it; this is unrealistic but possible in theory; if that is the case, we just
			// grasp the new head and try to access it, until we eventually succeed
			if (storeUsers.compute(head.getStateId(), (_id, old) -> old == null ? 1 : (old < 0 ? old : (old + 1))) > 0)
				return head;
		}
	}

	protected abstract S getStoreOfHead() throws ClosedNodeException, InterruptedException, TimeoutException;

	/**
	 * Called when this node is executing something that needs the store with the given state identifier.
	 * It can be used, for instance, to take note that that store cannot be garbage-collected from that moment.
	 * 
	 * @param stateId the state identifier of the store to enter
	 * @param the cache to use for the store; if missing, it gets extracted from the store itself (which might be expensive)
	 * @return the entered store
	 * @throws UnknownStateIdException if the required state identifier does not exist
	 *                                 (for instance also if it has been garbage-collected already)
	 * @throws InterruptedException if the operation has been interrupted before being completed
	 */
	protected S enter(StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, InterruptedException {
		// if the store identified by stateId is currently being garbage-collected (negative mark), we consider it as already missing
		if (storeUsers.compute(stateId, (_id, old) -> old == null ? 1 : (old < 0 ? old : (old + 1))) < 0)
			throw new UnknownStateIdException(stateId);
		else
			return mkStore(stateId, cache);
	}

	@Override
	protected void exit(S store) {
		storeUsers.compute(store.getStateId(), (_id, old) -> old == 1 ? null : (old - 1));
		super.exit(store);
	}

	/**
	 * Takes note that the given store must be persisted, that is, must not be garbage-collected.
	 * A store can be persisted more than once, in which case it must be garbage-collected the
	 * same amount of times in order to be actually removed from the database of stores.
	 * 
	 * @param stateId the identifier of the store to persist
	 * @param now the current time used for delivering the transactions that led to the
	 *            store to persist
	 * @param txn the Xodus transaction where the operation is performed
	 * @throws UnknownStateIdException if {@code stateId} is unknown or has been garbage-collected
	 */
	protected void persist(StateId stateId, long now, Transaction txn) throws UnknownStateIdException {
		addToStoresNotToGC(new StateIdAndTime(stateId, now), txn);
	}

	/**
	 * Takes note that only stores not older than the given creation time limit remain persisted,
	 * while the other stores, persisted up to now, can be garbage-collected.
	 * 
	 * @param limitCreationTime the time limit: stores creates at this time or later
	 *                          are retained, the others are marked as potentially garbage-collectable;
	 *                          this is expressed in milliseconds after the Unix epoch
	 * @param txn the Xodus transaction where the operation is performed
	 */
	protected void keepPersistedOnlyNotOlderThan(long limitCreationTime, Transaction txn) {
		Set<StateIdAndTime> removedIds = retainOnlyNotOlderThan(limitCreationTime, txn);
		addToStores(STORES_TO_GC, removedIds, txn);
	}

	
	/**
	 * Factory method for creating a store for this node, checked out at the given state identifier.
	 * If the cache is missing, it gets extracted from the store itself (which might be expensive).
	 * 
	 * @param stateId the state identifier
	 * @param cache the cache to use for the store; if missing, it will get extracted from the store
	 * @return the resulting store
	 */
	protected final S mkStore(StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, InterruptedException {
		return mkEmptyStore().checkedOutAt(stateId, cache);
	}

	private void gc(StateIdAndTime stateIdAndTime) {
		Function<Transaction, Optional<UnknownStateIdException>> gc = txn -> {
			try {
				free(stateIdAndTime.stateId, txn);
				removeFromStores(STORES_TO_GC, stateIdAndTime, txn);
				return Optional.empty();
			}
			catch (UnknownStateIdException e) {
				return Optional.of(e);
			}
		};

		env.computeInTransaction(gc).ifPresentOrElse(
			e -> LOGGER.log(Level.SEVERE, "cannot garbage-collect store " + stateIdAndTime.stateId, e),
			() -> LOGGER.fine(() -> "garbage-collected store " + stateIdAndTime.stateId)
		);
	}

	/**
	 * Decrements the reference counter for the resources used for a given vision of the store.
	 * This might lead to the deallocation of some data from the database.
	 * 
	 * @param stateId the identifier of the vision of the store to decrement
	 * @param txn the database transaction where the operation is performed
	 * @param UnknownStateIdException if {@code stateId} cannot be found in store
	 */
	protected void free(StateId stateId, Transaction txn) throws UnknownStateIdException {
		var bytes = stateId.getBytes();
		var rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		var rootOfRequests = new byte[32];
		System.arraycopy(bytes, 32, rootOfRequests, 0, 32);
		var rootOfHistories = new byte[32];
		System.arraycopy(bytes, 64, rootOfHistories, 0, 32);

		try {
			mkTrieOfRequests(txn, rootOfRequests).free();
			mkTrieOfResponses(txn, rootOfResponses).free();
			mkTrieOfHistories(txn, rootOfHistories).free();
		}
		catch (UnknownKeyException e) {
			throw new UnknownStateIdException(stateId);
		}
	}

	/**
	 * Increments the reference counter of the resources used for a given vision of the store.
	 * 
	 * @param stateId the identifier of the vision of the store to deallocate
	 * @param txn the database transaction where the operation is performed
	 * @param UnknownStateIdException if {@code stateId} cannot be found in store
	 */
	protected void malloc(StateId stateId, Transaction txn) throws UnknownStateIdException {
		var bytes = stateId.getBytes();
		var rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		var rootOfRequests = new byte[32];
		System.arraycopy(bytes, 32, rootOfRequests, 0, 32);
		var rootOfHistories = new byte[32];
		System.arraycopy(bytes, 64, rootOfHistories, 0, 32);

		try {
			mkTrieOfRequests(txn, rootOfRequests).malloc();
			mkTrieOfResponses(txn, rootOfResponses).malloc();
			mkTrieOfHistories(txn, rootOfHistories).malloc();
		}
		catch (UnknownKeyException e) {
			throw new UnknownStateIdException(stateId);
		}
	}

	protected TrieOfResponses mkTrieOfResponses(Transaction txn, byte[] rootOfResponses) throws UnknownKeyException {
		return new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses);
	}

	protected TrieOfRequests mkTrieOfRequests(Transaction txn, byte[] rootOfRequests) throws UnknownKeyException {
		return new TrieOfRequests(new KeyValueStoreOnXodus(storeOfRequests, txn), rootOfRequests);
	}

	protected void checkExistenceOfRootOfRequests(Transaction txn, byte[] rootOfRequests) throws UnknownKeyException {
		if (!Arrays.equals(rootOfRequests, hashOfEmpty))
			new KeyValueStoreOnXodus(storeOfRequests, txn).get(rootOfRequests);
	}

	protected void checkExistenceOfRootOfResponses(Transaction txn, byte[] rootOfResponses) throws UnknownKeyException {
		if (!Arrays.equals(rootOfResponses, hashOfEmpty))
			new KeyValueStoreOnXodus(storeOfResponses, txn).get(rootOfResponses);
	}

	protected void checkExistenceOfRootOfHistories(Transaction txn, byte[] rootOfHistories) throws UnknownKeyException {
		if (!Arrays.equals(rootOfHistories, hashOfEmpty))
			new KeyValueStoreOnXodus(storeOfHistories, txn).get(rootOfHistories);
	}

	protected TrieOfHistories mkTrieOfHistories(Transaction txn, byte[] rootOfHistories) throws UnknownKeyException {
		return new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories);
	}

	/**
	 * The garbage-collection routine. It takes stores to garbage-collect and frees them.
	 */
	private void gc() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				Set<StateIdAndTime> toGC = env.computeInReadonlyTransaction(txn -> getStores(STORES_TO_GC, txn));
				LOGGER.info("#toGC: " + toGC.size() + " with users " + storeUsers);
				for (var stateIdAndTime: toGC) {
					StateId stateId = stateIdAndTime.stateId;
					// if nobody is using the store identified by stateId, then
					// we mark it with -1, so that it is considered as being garbage-collected
					if (storeUsers.compute(stateId, (_id, old) -> old == null ? -1 : old) < 0) {
						gc(stateIdAndTime);
						// after garbage-collection, we remove the mark for stateId
						storeUsers.remove(stateId);
					}
				}

				Thread.sleep(5000L);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "The garbage-collection thread dies because of an exception", e);
		}
	}

	private SortedSet<StateIdAndTime> getStores(ByteIterable which, Transaction txn) {
		byte[] bytes = Optional.ofNullable(storeOfNode.get(txn, which)).map(ByteIterable::getBytes).orElse(new byte[0]);

		var result = new TreeSet<StateIdAndTime>();
		int size = StateIdAndTime.SIZE_IN_BYTES;
		for (int pos = 0; pos < bytes.length; pos += size) {
			var snippet = new byte[size];
			System.arraycopy(bytes, pos, snippet, 0, size);
			result.add(new StateIdAndTime(snippet));
		}

		return result;
	}

	/**
	 * Removes the given store from the set identified by {@code which}.
	 * 
	 * @param which the identifier of the set
	 * @param toRemove the store to remove
	 * @param txn the Xodus transaction where the operation is performed
	 */
	private void removeFromStores(ByteIterable which, StateIdAndTime toRemove, Transaction txn) {
		SortedSet<StateIdAndTime> ids = getStores(which, txn);

		if (ids.remove(toRemove))
			storeStateIdsAndTimes(which, ids, txn);
	}

	/**
	 * Retains only stores that are not older than the given time in the set identified by {@code which}.
	 * 
	 * @param limitCreationTime the time limit: stores created at that time or later are retained;
	 *                          this is in milliseconds from the Unix epoch
	 * @param txn the Xodus transaction where the operation is performed
	 * @return the stores that have been removed from the set, because they were older than {@code limitCreationTime}
	 */
	private Set<StateIdAndTime> retainOnlyNotOlderThan(long limitCreationTime, Transaction txn) {
		SortedSet<StateIdAndTime> ids = getStores(STORES_NOT_TO_GC, txn);
		Set<StateIdAndTime> removedIds = new HashSet<>();

		for (var id: ids)
			if (id.time < limitCreationTime)
				removedIds.add(id);
			else
				break; // since id's are sorted in non-decreasing creation time

		if (!removedIds.isEmpty()) {
			ids.removeAll(removedIds);
			storeStateIdsAndTimes(STORES_NOT_TO_GC, ids, txn);
		}

		return removedIds;
	}

	private void storeStateIdsAndTimes(ByteIterable which, Set<StateIdAndTime> ids, Transaction txn) {
		var reduced = new byte[StateIdAndTime.SIZE_IN_BYTES * ids.size()];
		int pos = 0;
		for (var id: ids) {
			System.arraycopy(id.getBytes(), 0, reduced, pos, StateIdAndTime.SIZE_IN_BYTES);
			pos += StateIdAndTime.SIZE_IN_BYTES;
		}

		storeOfNode.put(txn, which, ByteIterable.fromBytes(reduced));
	}

	/**
	 * Adds the given stores to the given set.
	 * 
	 * @param which the identifier of the set
	 * @param toAdd the stores to add
	 * @param txn the Xodus transaction where the operation is performed
	 */
	private void addToStores(ByteIterable which, Set<StateIdAndTime> toAdd, Transaction txn) {
		SortedSet<StateIdAndTime> ids = getStores(which, txn);
		if (ids.addAll(toAdd))
			storeStateIdsAndTimes(which, ids, txn);
	}

	private void addToStoresNotToGC(StateIdAndTime toAdd, Transaction txn) throws UnknownStateIdException {
		SortedSet<StateIdAndTime> ids = getStores(STORES_NOT_TO_GC, txn);
		if (ids.add(toAdd)) {
			storeStateIdsAndTimes(STORES_NOT_TO_GC, ids, txn);
			malloc(toAdd.stateId, txn);
		}
	}

	/**
	 * A pair of a store identifier and of its creation time. They are ordered by increasing creation time.
	 */
	private static class StateIdAndTime implements Comparable<StateIdAndTime> {
		private final StateId stateId;
		private final long time;
		private final static int SIZE_IN_BYTES = 96 + 8;

		private StateIdAndTime(StateId stateId, long time) {
			this.stateId = stateId;
			this.time = time;
		}

		private StateIdAndTime(byte[] bytes) {
			var bytesForStateId = new byte[96];
			System.arraycopy(bytes, 0, bytesForStateId, 0, 96);
			this.stateId = StateIds.of(bytesForStateId);

			long t = 0;
		    for (int i = 0; i < 8; i++) {
		        t <<= 8;
		        t |= (bytes[96 + i] & 0xFF);
		    }

		    this.time = t;
		}

		private byte[] getBytes() {
			var result = new byte[SIZE_IN_BYTES];
			System.arraycopy(stateId.getBytes(), 0, result, 0, 96);

			long l = time;
			for (int i = 96 + 7; i >= 96; i--) {
		        result[i] = (byte) (l & 0xFF);
		        l >>= 8;
		    }

			return result;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof StateIdAndTime siat && siat.time == time && siat.stateId.equals(stateId);
		}

		@Override
		public int hashCode() {
			return (int) time;
		}

		@Override
		public int compareTo(StateIdAndTime other) {
			int diff = Long.compare(time, other.time);
			if (diff != 0)
				return diff;
			else
				return stateId.compareTo(other.stateId);
		}

		@Override
		public String toString() {
			return stateId + "@" + time;
		}
	}
}