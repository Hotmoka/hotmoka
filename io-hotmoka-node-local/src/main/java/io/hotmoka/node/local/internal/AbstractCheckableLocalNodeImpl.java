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

package io.hotmoka.node.local.internal;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.Hex;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.AbstractStoreTranformation;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 * @param <T> the type of the store transformations that can be started from this store
 */
@ThreadSafe
public abstract class AbstractCheckableLocalNodeImpl<C extends LocalNodeConfig<C,?>, S extends AbstractStore<S, T> & CheckableStore<S, T>, T extends AbstractStoreTranformation<S, T>> extends AbstractLocalNode<C, S, T> {

	/**
	 * The Xodus environment used for storing information about the node, such as its store.
	 */
	private final Environment env;

	/**
	 * The Xodus store that holds the persistent information of the node.
	 */
	private final io.hotmoka.xodus.env.Store storeOfNode;

	/**
	 * The queue of old stores to garbage-collect.
	 */
	private final BlockingQueue<S> storesToGC = new LinkedBlockingDeque<>(1_000);

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

	private final static Logger LOGGER = Logger.getLogger(AbstractCheckableLocalNodeImpl.class.getName());

	/**
	 * Creates a new node.
	 * 
	 * @param config the configuration of the node
	 * @param init if true, the working directory of the node gets initialized
	 * @throws NodeException if the operation cannot be completed correctly
	 */
	protected AbstractCheckableLocalNodeImpl(C config, boolean init) throws NodeException {
		super(config, init);

		this.env = new Environment(config.getDir() + "/node");
		this.storeOfNode = env.computeInTransaction(txn -> env.openStoreWithoutDuplicates("node", txn));

		// we start the garbage-collection task
		getExecutors().execute(this::gc);
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
			setStore(getStore().checkoutAt(root.get()));
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void moveToFinalStoreOf(T transaction) throws NodeException {
		S oldStore = getStore();
		super.moveToFinalStoreOf(transaction);

		try {
			var rootAsBI = ByteIterable.fromBytes(getStore().getStateId());
			env.executeInTransaction(txn -> setRootBranch(oldStore, rootAsBI, txn));

			if (!isUsed(oldStore))
				if (!storesToGC.offer(oldStore))
					LOGGER.warning("could not enqueue old store for garbage collection: the queue is full!");
		}
		catch (StoreException | ExodusException e) {
			throw new NodeException(e);
		}
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

	private final ConcurrentMap<CheckableStore<?,?>, Integer> storeUsers = new ConcurrentHashMap<>();

	@Override
	protected void enter(S store) {
		super.enter(store);
		storeUsers.putIfAbsent(store, 0);
		storeUsers.compute(store, (_store, old) -> old + 1);
	}

	@Override
	protected void exit(S store) {
		storeUsers.compute(store, (_store, old) -> old - 1);

		if (!isUsed(store))
			if (!storesToGC.offer(store))
				LOGGER.warning("could not enqueue old store for garbage collection: the queue is full!");

		super.exit(store);
	}

	private boolean isUsed(S store) {
		return store == getStore() || storeUsers.getOrDefault(store, 0) > 0;
	}

	private void setRootBranch(S oldStore, ByteIterable rootAsBI, Transaction txn) {
		storeOfNode.put(txn, ROOT, rootAsBI); // we set the root branch
		//storeOfNode.put(txn, PAST_STORES, null); // we add the old store to the past stores list
	}

	/**
	 * The garbage-collection routine. It takes stores to garbage-collect and frees them.
	 */
	private void gc()  {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					S next = storesToGC.take();
					byte[] id = next.getStateId();
					next.free();
					LOGGER.info("garbage collected store " + Hex.toHexString(id));
				}
				catch (StoreException e) {
					LOGGER.log(Level.SEVERE, "could not garbage-collect a store", e);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}