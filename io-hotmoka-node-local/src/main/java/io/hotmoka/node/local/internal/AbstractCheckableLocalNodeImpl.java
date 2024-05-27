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

import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.Hex;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.AbstractStoreTranformation;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;

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

	private final static Logger LOGGER = Logger.getLogger(AbstractCheckableLocalNodeImpl.class.getName());

	/**
	 * Creates a new node.
	 * 
	 * @param consensus the consensus configuration of the node; if missing, this will be extracted
	 *                  from the saved state of the node
	 * @param config the configuration of the node
	 * @throws NodeException if the operation cannot be completed correctly
	 */
	protected AbstractCheckableLocalNodeImpl(Optional<ConsensusConfig<?,?>> consensus, C config) throws NodeException {
		super(consensus, config);

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
		try {
			super.initWithEmptyStore(ValidatorsConsensusConfigBuilders.defaults().build());
		}
		catch (NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}

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
			env.executeInTransaction(txn -> storeOfNode.put(txn, ROOT, rootAsBI));

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