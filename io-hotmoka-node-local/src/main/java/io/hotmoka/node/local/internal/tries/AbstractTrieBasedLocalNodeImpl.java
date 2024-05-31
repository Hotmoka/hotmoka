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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * Partial implementation of a local (ie., non-remote) node.
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
	 * The queue of old stores to garbage-collect.
	 */
	private final BlockingQueue<S> storesToGC = new LinkedBlockingDeque<>(1_000);

	private final ConcurrentMap<StateId, Integer> storeUsers = new ConcurrentHashMap<>();

	/**
	 * The key used inside {@link #storeOfNode} to keep the root of the store of this node.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromBytes("root".getBytes());

	/**
	 * The key used inside {@link #storeOfNode} to keep the list of old stores
	 * that are candidate for garbage-collection, as soon as their height is sufficiently
	 * smaller than the height of the store of this node.
	 */
	private final static ByteIterable PAST_STORES = ByteIterable.fromBytes("past stores".getBytes());

	private final static Logger LOGGER = Logger.getLogger(AbstractTrieBasedLocalNodeImpl.class.getName());

	/**
	 * Creates a new node.
	 * 
	 * @param config the configuration of the node
	 * @param init if true, the working directory of the node gets initialized
	 * @throws NodeException if the operation cannot be completed correctly
	 */
	protected AbstractTrieBasedLocalNodeImpl(C config, boolean init) throws NodeException {
		super(config, init);

		this.env = new Environment(config.getDir() + "/node");
		this.storeOfNode = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("node", txn));
    	this.storeOfResponses = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("responses", txn));
    	this.storeOfInfo = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("info", txn));
		this.storeOfRequests = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("requests", txn));
		this.storeOfHistories = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("histories", txn));

		// we start the garbage-collection tasks
		getExecutors().execute(this::gc);
		getExecutors().execute(this::findPastStoresThatCanBeGarbageCollected);
	}

	protected final io.hotmoka.xodus.env.Store getStoreOfResponses() {
		return storeOfResponses;
	}

	protected final io.hotmoka.xodus.env.Store getStoreOfInfo() {
		return storeOfInfo;
	}

	protected final io.hotmoka.xodus.env.Store getStoreOfRequests() {
		return storeOfRequests;
	}

	protected final io.hotmoka.xodus.env.Store getStoreOfHistories() {
		return storeOfHistories;
	}

	protected final Environment getEnvironment() {
		return env;
	}

	@Override
	protected void initWithSavedStore() throws NodeException {
		// we start from the empty store
		super.initWithEmptyStore();

		// then we check it out at its store branch
		var root = env.computeInTransaction(txn -> Optional.ofNullable(storeOfNode.get(txn, ROOT)).map(ByteIterable::getBytes));
		if (root.isEmpty())
			throw new NodeException("Cannot find the root of the saved store of the node");

		try {
			setStore(getStore().checkedOutAt(StateIds.of(root.get())));
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void moveToFinalStoreOf(T transaction) throws NodeException {
		S oldStore = getStore();
		super.moveToFinalStoreOf(transaction);
		setRootBranch(oldStore);
	}

	@Override
	protected void closeResources() throws NodeException, InterruptedException {
		try {
			super.closeResources();
		}
		finally {
			try {
				env.close();
			}
			catch (ExodusException e) {
				throw new NodeException(e);
			}
		}
	}

	@Override
	protected void enter(S store) {
		super.enter(store);
		storeUsers.compute(store.getStateId(), (_store, old) -> old == null ? 1 : old + 1);
	}

	@Override
	protected void exit(S store) {
		storeUsers.compute(store.getStateId(), (_store, old) -> old - 1);
		super.exit(store);
	}

	/**
	 * Factory method for creating a store for this node, checked out at the given state identifier.
	 * 
	 * @param stateId the state identifier
	 * @return the resulting store
	 */
	protected abstract S mkStore(StateId stateId) throws NodeException;

	private boolean isUsed(StateId id) {
		var currentStore = getStore();
		// the current store might be null if the node has just restarted and its store has not been set yet 
		return currentStore != null && id.equals(currentStore.getStateId()) || storeUsers.getOrDefault(id, 0) > 0;
	}

	/**
	 * The garbage-collection routine. It takes stores to garbage-collect and frees them.
	 */
	private void gc()  {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				S next = storesToGC.take();
				StateId id = next.getStateId();

				try {
					CheckRunnable.check(StoreException.class, () -> env.executeInTransaction(UncheckConsumer.uncheck(txn -> gc(next, id, txn))));
					LOGGER.info("garbage-collected store " + id);
				}
				catch (StoreException e) {
					LOGGER.log(Level.SEVERE, "could not garbage-collect store " + id, e);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void gc(S store, StateId id, Transaction txn) throws StoreException {
		store.free(txn);
		removeFromPastStoresThatCanBeGarbageCollected(id, txn);
	}

	private void findPastStoresThatCanBeGarbageCollected() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				List<StateId> ids = env.computeInReadonlyTransaction(txn -> getPastStoresNotYetGarbageCollected(txn));

				for (StateId id: ids) {
					if (!isUsed(id)) {
						try {
							S oldStore = mkStore(id);
							if (!storesToGC.offer(oldStore))
								LOGGER.warning("could offer store " + id + " to the garbage-collector: the queue is full!");
						}
						catch (NodeException e) {
							LOGGER.log(Level.SEVERE, "cannot offer store " + id + " to the garbage-collector", e);
						}
					}
				}

				Thread.sleep(2000L);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void setRootBranch(S oldStore) throws NodeException {
		S currentStore = getStore();
		byte[] id = currentStore.getStateId().getBytes();
		var rootAsBI = ByteIterable.fromBytes(id);
	
		try {
			CheckRunnable.check(StoreException.class, () -> env.executeInTransaction(UncheckConsumer.uncheck(txn -> {
				storeOfNode.put(txn, ROOT, rootAsBI); // set the root branch
				currentStore.malloc(txn); // increment the reference count of the new store
				addPastStoreToListOfNotYetGarbageCollected(oldStore, txn); // add the old store to the past stores list
			})));
		}
		catch (ExodusException | StoreException e) {
			throw new NodeException(e);
		}
	}

	private List<StateId> getPastStoresNotYetGarbageCollected(Transaction txn) throws ExodusException {
		var ids = Optional.ofNullable(storeOfNode.get(txn, PAST_STORES)).map(ByteIterable::getBytes);
		byte[] bytes = ids.orElse(new byte[0]);
	
		// each store id consists of 128 bytes
		var result = new ArrayList<StateId>();
		for (int pos = 0; pos < bytes.length; pos += 128) {
			var id = new byte[128];
			System.arraycopy(bytes, pos, id, 0, 128);
			result.add(StateIds.of(id));
		}
	
		return result;
	}

	private void removeFromPastStoresThatCanBeGarbageCollected(StateId id, Transaction txn) throws ExodusException {
		List<StateId> ids = getPastStoresNotYetGarbageCollected(txn);
		ids.remove(id);
		byte[] reduced = new byte[128 * ids.size()];
		for (int pos = 0; pos < reduced.length; pos += 128)
			System.arraycopy(ids.get(pos / 128).getBytes(), 0, reduced, pos, 128);

		storeOfNode.put(txn, PAST_STORES, ByteIterable.fromBytes(reduced));
		System.out.println(getPastStoresNotYetGarbageCollected(txn).size());
	}

	private void addPastStoreToListOfNotYetGarbageCollected(S store, Transaction txn) throws ExodusException {
		var addedId = store.getStateId();
		var ids = Optional.ofNullable(storeOfNode.get(txn, PAST_STORES)).map(ByteIterable::getBytes);
		byte[] bytes = ids.orElse(new byte[0]);

		// each store id consists of 128 bytes
		for (int pos = 0; pos < bytes.length; pos += 128) {
			var id = new byte[128];
			System.arraycopy(bytes, pos, id, 0, 128);
			if (addedId.equals(StateIds.of(id)))
				return;
		}

		var expanded = new byte[bytes.length + 128];
		System.arraycopy(bytes, 0, expanded, 0, bytes.length);
		System.arraycopy(addedId.getBytes(), 0, expanded, bytes.length, 128);
		storeOfNode.put(txn, PAST_STORES, ByteIterable.fromBytes(expanded));
		System.out.println(getPastStoresNotYetGarbageCollected(txn).size());
	}
}