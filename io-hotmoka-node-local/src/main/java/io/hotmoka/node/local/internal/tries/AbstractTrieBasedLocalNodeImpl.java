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

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.GuardedBy;
import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.LocalNodeException;
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
	 * The lock object used to avoid garbage-collecting stores that are currently used.
	 */
	private final Object lockGC = new Object();

	/**
	 * A map from each state identifier of the store to the number of users of that store
	 * at that state identifier. The goal of this information is to avoid garbage-collecting
	 * store identifiers that are currently being used for some reason.
	 */
	@GuardedBy("lockGC")
	private final Map<StateId, Integer> storeUsers = new HashMap<>();

	/**
	 * The SHA256 algorithm used for hashing the nodes of the tries.
	 * We store it here so that we can clone it wherever we need it, instead of
	 * creating it every time and deal with a potential NoSuchAlgorithmException.
	 */
	private final HashingAlgorithm sha256;

	/**
	 * The hash of the empty node in the tries.
	 */
	private final byte[] hashOfEmpty = new byte[32]; // TODO: reuse in the tries

	/**
	 * The key used inside {@link #storeOfNode} to keep the list of old stores
	 * that are candidate for garbage-collection, if they are not used by any running task.
	 */
	private final static ByteIterable STORES_TO_GC = ByteIterable.fromBytes("stores to gc".getBytes());

	/**
	 * The key used inside {@link #storeOfNode} to keep the list of stores that this node
	 * will not try to garbage collect. This list gets expanded when a new head is added to the
	 * node and gets shrunk by calls to {@link #keepPersistentedOnly(Stream)}.
	 */
	private final static ByteIterable STORES_NOT_TO_GC = ByteIterable.fromBytes("stores not to gc".getBytes());

	private final static Logger LOGGER = Logger.getLogger(AbstractTrieBasedLocalNodeImpl.class.getName());

	/**
	 * Creates a new node.
	 * 
	 * @param config the configuration of the node
	 * @param init if true, the working directory of the node gets initialized
	 */
	protected AbstractTrieBasedLocalNodeImpl(C config, boolean init) {
		super(config, init);

		var path = config.getDir().resolve("hotmoka").resolve("store");

		try {
			this.sha256 = HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new LocalNodeException(e);
		}

		this.env = new Environment(path.toString());
		this.storeOfNode = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("node", txn));
    	this.storeOfResponses = env.computeInTransaction(txn -> env.openStoreWithoutDuplicatesWithPrefixing("responses", txn));
    	this.storeOfInfo = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("info", txn));
		this.storeOfRequests = env.computeInTransaction(txn -> env.openStoreWithoutDuplicatesWithPrefixing("requests", txn));
		this.storeOfHistories = env.computeInTransaction(txn -> env.openStoreWithoutDuplicatesWithPrefixing("histories", txn));

		// we start the garbage-collection task
		getExecutors().execute(this::gc);

		LOGGER.info("opened the store database at " + path);
	}

	/**
	 * Yields a clone of the SHA256 hashing algorithm.
	 * 
	 * @return the clone
	 */
	protected HashingAlgorithm mkSHA256() {
		return sha256.clone();
	}

	protected final io.hotmoka.xodus.env.Store getStoreOfNode() {
		return storeOfNode;
	}

	protected final Environment getEnvironment() {
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
	protected final S enterHead() throws ClosedNodeException, InterruptedException {
		synchronized (lockGC) {
			S head = getStoreOfHead();
			enter(head, head.getStateId());
			return head;
		}
	}

	@GuardedBy("lockGC")
	protected abstract S getStoreOfHead() throws ClosedNodeException, InterruptedException;

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
		synchronized (lockGC) {
			S result = mkStore(stateId, cache);
			enter(result, stateId);
			return result;
		}
	}

	/**
	 * Called when this node is executing something that needs the given store with the given state identifier.
	 * It can be used, for instance, to take note that that store cannot be garbage-collected from that moment.
	 */
	@GuardedBy("lockGC")
	private void enter(S store, StateId stateId) {
		storeUsers.compute(stateId, (_id, old) -> old == null ? 1 : (old + 1));
	}

	@Override
	protected void exit(S store) {
		synchronized (lockGC) {
			storeUsers.compute(store.getStateId(), (_id, old) -> old == 1 ? null : (old - 1));
		}

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
	 * @param UnknownStateIdException if {@code stateId} cannot be found in store
	 */
	protected void persist(StateId stateId, long now, Transaction txn) throws UnknownStateIdException {
		malloc(stateId, txn);
		addToStores(STORES_NOT_TO_GC, Set.of(new StateIdAndTime(stateId, now)), txn);
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
		Set<StateIdAndTime> removedIds = retainOnlyNotOlderThan(STORES_NOT_TO_GC, limitCreationTime, txn);
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
	 * Deallocates all resources used for a given vision of the store.
	 * 
	 * @param stateId the identifier of the vision of the store to deallocate
	 * @param txn the database transaction where the operation is performed
	 * @param UnknownStateIdException if {@code stateId} cannot be found in store
	 */
	private void free(StateId stateId, Transaction txn) throws UnknownStateIdException {
		var bytes = stateId.getBytes();
		var rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		var rootOfInfo = new byte[32];
		System.arraycopy(bytes, 32, rootOfInfo, 0, 32);
		var rootOfRequests = new byte[32];
		System.arraycopy(bytes, 64, rootOfRequests, 0, 32);
		var rootOfHistories = new byte[32];
		System.arraycopy(bytes, 96, rootOfHistories, 0, 32);

		try {
			mkTrieOfRequests(txn, rootOfRequests).free();
			mkTrieOfResponses(txn, rootOfResponses).free();
			mkTrieOfHistories(txn, rootOfHistories).free();
			mkTrieOfInfo(txn, rootOfInfo).free();
		}
		catch (UnknownKeyException e) {
			throw new UnknownStateIdException(stateId);
		}
	}

	/**
	 * Allocates the resources used for the given vision of the store.
	 * 
	 * @param stateId the identifier of the vision of the store to allocate
	 * @param txn the database transaction where the operation is performed
	 * @param UnknownStateIdException if {@code stateId} cannot be found in store
	 */
	private void malloc(StateId stateId, Transaction txn) throws UnknownStateIdException {
		var bytes = stateId.getBytes();
		var rootOfResponses = new byte[32];
		System.arraycopy(bytes, 0, rootOfResponses, 0, 32);
		var rootOfInfo = new byte[32];
		System.arraycopy(bytes, 32, rootOfInfo, 0, 32);
		var rootOfRequests = new byte[32];
		System.arraycopy(bytes, 64, rootOfRequests, 0, 32);
		var rootOfHistories = new byte[32];
		System.arraycopy(bytes, 96, rootOfHistories, 0, 32);

		try {
			var trieOfRequests = mkTrieOfRequests(txn, rootOfRequests);
			var trieOfResponses = mkTrieOfResponses(txn, rootOfResponses);
			var trieOfHistories = mkTrieOfHistories(txn, rootOfHistories);
			var trieOfInfo = mkTrieOfInfo(txn, rootOfInfo);

			// we increment the reference count of the roots of the resulting tries, so that
			// they do not get garbage collected until this store is freed
			trieOfResponses.malloc();
			trieOfInfo.malloc();
			trieOfHistories.malloc();
			trieOfRequests.malloc();
		}
		catch (UnknownKeyException e) {
			throw new UnknownStateIdException(e);
		}
	}

	protected TrieOfResponses mkTrieOfResponses(Transaction txn, byte[] rootOfResponses) throws UnknownKeyException {
		return new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses, this);
	}

	protected TrieOfInfo mkTrieOfInfo(Transaction txn, byte[] rootOfInfo) throws UnknownKeyException {
		return new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo, this);
	}

	protected TrieOfRequests mkTrieOfRequests(Transaction txn, byte[] rootOfRequests) throws UnknownKeyException {
		return new TrieOfRequests(new KeyValueStoreOnXodus(storeOfRequests, txn), rootOfRequests, this);
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

	protected void checkExistenceOfRootOfInfo(Transaction txn, byte[] rootOfInfo) throws UnknownKeyException {
		if (!Arrays.equals(rootOfInfo, hashOfEmpty))
			new KeyValueStoreOnXodus(storeOfInfo, txn).get(rootOfInfo);
	}

	protected TrieOfHistories mkTrieOfHistories(Transaction txn, byte[] rootOfHistories) throws UnknownKeyException {
		return new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories, this);
	}

	/**
	 * The garbage-collection routine. It takes stores to garbage-collect and frees them.
	 */
	private void gc() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				Set<StateIdAndTime> toGC = env.computeInReadonlyTransaction(txn -> getStores(STORES_TO_GC, txn));

				for (var stateIdAndTime: toGC)
					synchronized (lockGC) {
						if (storeUsers.getOrDefault(stateIdAndTime.stateId, 0) == 0)
							gc(stateIdAndTime);
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
		for (int pos = 0; pos < bytes.length; pos += StateIdAndTime.SIZE_IN_BYTES) {
			var snippet = new byte[StateIdAndTime.SIZE_IN_BYTES];
			System.arraycopy(bytes, pos, snippet, 0, StateIdAndTime.SIZE_IN_BYTES);
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
	 * @param which the identifier of the set
	 * @param limitCreationTime the time limit: stores created at that time or later are retained;
	 *                          this is in milliseconds from the Unix epoch
	 * @param txn the Xodus transaction where the operation is performed
	 * @return the stores that have been removed from the set, because they were older than {@code limitCreationTime}
	 */
	private Set<StateIdAndTime> retainOnlyNotOlderThan(ByteIterable which, long limitCreationTime, Transaction txn) {
		SortedSet<StateIdAndTime> ids = getStores(which, txn);
		Set<StateIdAndTime> removedIds = new HashSet<>();

		for (var id: ids)
			if (id.time < limitCreationTime)
				removedIds.add(id);
			else
				break; // since id's are sorted in non-decreasing creation time

		if (!removedIds.isEmpty()) {
			ids.removeAll(removedIds);
			storeStateIdsAndTimes(which, ids, txn);
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

	/**
	 * A pair of a store identifier and of its creation time. They are ordered by increasing creation time.
	 */
	private static class StateIdAndTime implements Comparable<StateIdAndTime> {
		private final StateId stateId;
		private final long time;
		private final static int SIZE_IN_BYTES = 128 + 8;

		private StateIdAndTime(StateId stateId, long time) {
			this.stateId = stateId;
			this.time = time;
		}

		private StateIdAndTime(byte[] bytes) {
			var bytesForStateId = new byte[128];
			System.arraycopy(bytes, 0, bytesForStateId, 0, 128);
			this.stateId = StateIds.of(bytesForStateId);

			long t = 0;
		    for (int i = 0; i < 8; i++) {
		        t <<= 8;
		        t |= (bytes[128 + i] & 0xFF);
		    }

		    this.time = t;
		}

		private byte[] getBytes() {
			var result = new byte[SIZE_IN_BYTES];
			System.arraycopy(stateId.getBytes(), 0, result, 0, 128);

			long l = time;
			for (int i = 128 + 7; i >= 128; i--) {
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