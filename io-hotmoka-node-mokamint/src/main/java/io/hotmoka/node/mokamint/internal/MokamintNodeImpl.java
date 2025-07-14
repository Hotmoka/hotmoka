/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.mokamint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.GuardedBy;
import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.constants.Constants;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.AbstractTrieBasedLocalNode;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.mokamint.application.AbstractApplication;
import io.mokamint.application.api.ClosedApplicationException;
import io.mokamint.application.api.UnknownGroupIdException;
import io.mokamint.application.api.UnknownStateException;
import io.mokamint.node.Transactions;
import io.mokamint.node.api.ApplicationTimeoutException;
import io.mokamint.node.api.Block;
import io.mokamint.node.api.NonGenesisBlock;
import io.mokamint.node.api.Transaction;
import io.mokamint.node.local.AbstractLocalNode;
import io.mokamint.node.local.api.LocalNode;
import io.mokamint.node.local.api.LocalNodeConfig;
import io.mokamint.nonce.api.Deadline;

/**
 * An implementation of blockchain nodes that rely on the Mokamint proof of space engine.
 */
@ThreadSafe
public class MokamintNodeImpl extends AbstractTrieBasedLocalNode<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> implements MokamintNode {

	/**
	 * The underlying Mokamint engine.
	 */
	private final MyMokamintNode mokamintNode;

	/**
	 * A queue of blocks to publish. This gets enriched when the head changes and gets consumed
	 * by the {@link #publishBlocks()} background task.
	 */
	private final BlockingQueue<NonGenesisBlock> toPublish = new LinkedBlockingDeque<>();

	/**
	 * A lock for accessing {@link #lastHeadStateId} and {@link #storeOfHead}.
	 */
	private final Object headLock = new Object();

	/**
	 * A cache for the caches at a given state identifier. This allows to reuse
	 * a previous cache if an old state identifier is checked out. The alternative
	 * is to recompute the cache at the state identifier, which is expensive.
	 */
	private final LRUCache<StateId, StoreCache> lastCaches = new LRUCache<>(100, 1000);

	/**
	 * The last state identifier used in {@link #getStoreOfHead()}, for caching.
	 */
	@GuardedBy("headLock")
	private byte[] lastHeadStateId;

	/**
	 * The last store computed in {@link #getStoreOfHead()}, for caching.
	 */
	@GuardedBy("headLock")
	private MokamintStore storeOfHead;

	private final static Logger LOGGER = Logger.getLogger(MokamintNodeImpl.class.getName());

	/**
	 * Builds and starts a Mokamint node that uses an already existing store. The consensus
	 * parameters are recovered from the manifest in the store, hence the store must
	 * be that of an already initialized blockchain. It spawns the Mokamint engine
	 * and connects it to an application for handling its transactions.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param mokamintConfig the configuration of the underlying Mokamint engine
	 * @param keyPair the keys of the Mokamint node, used to sign the blocks that it mines
	 * @param init if true, the working directory of the node gets initialized
	 * @param createGenesis if true, creates a genesis block and starts mining on top of it
	 *                      (initial synchronization is consequently skipped), otherwise it
	 *                      synchronizes, waits for whispered blocks and then starts mining on top of them
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 * @throws TimeoutException if the application of the Mokamint node is unresponsive
	 */
	public MokamintNodeImpl(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair, boolean init, boolean createGenesis) throws InterruptedException, TimeoutException {
		super(config, init);

		this.mokamintNode = new MyMokamintNode(mokamintConfig, keyPair, createGenesis);
		// mokamintNode.addOnCloseHandler(this::close); // TODO

		getExecutors().execute(this::publishBlocks);
	}

	/**
	 * The Mokamint node that supports this Hotmoka node.
	 */
	private class MyMokamintNode extends AbstractLocalNode {

		private MyMokamintNode(LocalNodeConfig mokamintConfig, KeyPair keyPair, boolean createGenesis) throws InterruptedException {
			super(mokamintConfig, keyPair, new MokamintHotmokaApplication(), createGenesis);
		}

		@Override
		protected void onHeadChanged(Deque<Block> pathToNewHead) {
			super.onHeadChanged(pathToNewHead);

			for (Block added: pathToNewHead) // TODO: add an application method instead: publish(block)
				if (added instanceof NonGenesisBlock ngb)
					toPublish.offer(ngb);
		}
	}

	@Override
	public NodeInfo getInfo() throws ClosedNodeException, InterruptedException, TimeoutException {
		try (var scope = mkScope()) {
			return NodeInfos.of(MokamintNode.class.getName(), Constants.HOTMOKA_VERSION, mokamintNode.getInfo().getUUID().toString());
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			throw new ClosedNodeException(e);
		}
	}

	@Override
	public LocalNode getMokamintNode() {
		return mokamintNode;
	}

	@Override
	protected MokamintStore mkEmptyStore() {
		return new MokamintStore(this);
	}

	@Override
	protected MokamintStore getStoreOfHead() throws ClosedNodeException, InterruptedException {
		try {
			var maybeHeadStateId = mokamintNode.getChainInfo().getHeadStateId();
			if (maybeHeadStateId.isEmpty())
				return mkEmptyStore();

			synchronized (headLock) {
				if (lastHeadStateId != null && Arrays.equals(lastHeadStateId, maybeHeadStateId.get()))
					return storeOfHead;
			}

			var si = StateIds.of(maybeHeadStateId.get());
			MokamintStore result = mkStore(si, Optional.ofNullable(lastCaches.get(si))); // TODO: who guarantees that the store at si has not been garbage-collected?

			synchronized (headLock) {
				lastHeadStateId = maybeHeadStateId.get();
				storeOfHead = result;
			}

			return result;
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			throw new ClosedNodeException(e);
		}
		catch (UnknownStateIdException e) {
			// this should not happen...
			throw new LocalNodeException("The state of the head block is unknown or has been garbage-collected!", e);
		}
	}

	@Override
	protected void closeResources() {
		try {
			mokamintNode.close();
		}
		finally {
			super.closeResources();
		}
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) throws ClosedNodeException, InterruptedException {
		try {
			mokamintNode.add(Transactions.of(request.toByteArray()));
		}
		catch (io.mokamint.node.api.TransactionRejectedException e) {
			// the mempool of the Mokamint engine has rejected the transaction:
			// the node has been already signaled that it failed, so there is nothing to do here
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			throw new ClosedNodeException(e);
		}
		catch (ApplicationTimeoutException e) {
			throw new LocalNodeException("Unexpected exception: the application is local and its method never go into timeout", e);
		}
	}

	private void publishBlocks() {
		var hasher = getHasher();

		try {
			while (true) {
				NonGenesisBlock next = toPublish.take();
				var si = StateIds.of(next.getStateId());
	
				try {
					MokamintStore store = mkStore(si, Optional.ofNullable(lastCaches.get(si)));
	
					for (var tx: next.getTransactions().toArray(Transaction[]::new)) {
						try {
							publish(TransactionReferences.of(hasher.hash(intoHotmokaRequest(tx))), store);
						}
						catch (UnknownReferenceException e) { // TODO: are you sure?
							// the transactions have been delivered, if they cannot be found then there is a problem in the database
							throw new LocalNodeException("Already delivered transactions should be in store", e);
						}
						catch (io.mokamint.node.api.TransactionRejectedException e) {
							// the transactions have been delivered, they must be legal
							throw new LocalNodeException("Already delivered transactions should not be rejected", e);
						}
					}
				}
				catch (UnknownStateIdException e) { // TODO: would this be a bug? in that case it should exit the thread
					// it happens for blocks arrived through whispering during a long synchronization and added much
					// after their arrival: why?
					LOGGER.log(Level.SEVERE, "failed to publish the transactions in block " + next.getHexHash(), e);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warning("The block publishing thread has been interrupted");
		}
		catch (RuntimeException | IOException e) {
			LOGGER.log(Level.SEVERE, "The block publishing thread exits because of an exception", e);
		}
	}

	private TransactionRequest<?> intoHotmokaRequest(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, IOException {
		try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
			try {
        		return TransactionRequests.from(context);
        	}
        	catch (IOException e) {
        		throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
        	}
        }
	}

	private class MokamintHotmokaApplication extends AbstractApplication {

		/**
		 * The current store transformations, for each group id of Hotmoka transactions.
		 */
		private final ConcurrentMap<Integer, MokamintStoreTransformation> transformations = new ConcurrentHashMap<>();

		/**
		 * The next group id to use for the next transformation that will be started with this application.
		 */
		private final AtomicInteger nextId = new AtomicInteger();

		@Override
		public boolean checkPrologExtra(byte[] extra) throws ClosedApplicationException {
			try (var scope = mkScope()) {
				return extra.length == 0; // no extra used by this application
			}
		}

		@Override
		public void checkTransaction(Transaction transaction) throws ClosedApplicationException {
			try (var scope = mkScope()) {
				// nothing, there is nothing we can check for Hotmoka
			}
		}

		@Override
		public long getPriority(Transaction transaction) throws ClosedApplicationException {
			try (var scope = mkScope()) {
				return 0;
			}
		}

		@Override
		public String getRepresentation(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ClosedApplicationException {
			try (var scope = mkScope()) {
				return intoHotmokaRequest(transaction).toString();
			}
			catch (IOException e) { // TODO
				throw new RuntimeException(e);
			}
		}

		@Override
		public byte[] getInitialStateId() throws ClosedApplicationException {
			try (var scope = mkScope()) {
				return new byte[128];
			}
		}

		@Override
		public int beginBlock(long height, LocalDateTime when, byte[] stateId) throws UnknownStateException, ClosedApplicationException, InterruptedException {
			var si = StateIds.of(stateId);

			MokamintStore start;

			try {
				// if we have information about the cache at the requested state id, we use it for better efficiency
				start = enter(si, Optional.ofNullable(lastCaches.get(si)));
			}
			catch (UnknownStateIdException e) {
				throw new UnknownStateException(e);
			}

			int groupId = nextId.getAndIncrement();
			transformations.put(groupId, start.beginTransformation(when.toInstant(ZoneOffset.UTC).toEpochMilli()));

			return groupId;
		}

		@Override
		public void deliverTransaction(int groupId, Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, UnknownGroupIdException, ClosedApplicationException, InterruptedException {
			try (var scope = mkScope()) {
				TransactionRequest<?> hotmokaRequest;

				try {
					hotmokaRequest = intoHotmokaRequest(transaction);
				}
				catch (IOException e) { // TODO
					throw new RuntimeException(e);
				}

				MokamintStoreTransformation transformation = getTransformation(groupId);

				try {
					transformation.deliverTransaction(hotmokaRequest);
				}
				catch (TransactionRejectedException e) {
					signalRejected(hotmokaRequest, e);
					throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
				}
			}
		}

		@Override
		public byte[] endBlock(int groupId, Deadline deadline) throws ClosedApplicationException, UnknownGroupIdException, InterruptedException {
			try (var scope = mkScope()) {
				MokamintStoreTransformation transformation = getTransformation(groupId);

				transformation.deliverCoinbaseTransactions(deadline.getProlog());

				return getEnvironment().computeInTransaction(txn -> {
					StateId stateIdOfFinalStore = transformation.getIdOfFinalStore(txn);

					if (lastCaches.get(stateIdOfFinalStore) == null) {
						lastCaches.put(stateIdOfFinalStore, transformation.getCache());

						try {
							persist(stateIdOfFinalStore, transformation.getNow(), txn);
						}
						catch (UnknownStateIdException e) {
							// impossible, we have just computed this id for the final store
							throw new LocalNodeException("State id " + stateIdOfFinalStore + " has been just computed: it must have existed", e);
						}
					}

					return stateIdOfFinalStore.getBytes();
				});
			}
		}

		@Override
		public void commitBlock(int groupId) throws UnknownGroupIdException, ClosedApplicationException {
			try (var scope = mkScope()) {
				var transformation = getTransformation(groupId);
				exit(transformation.getInitialStore());
				transformations.remove(groupId);
			}
		}

		@Override
		public void abortBlock(int groupId) throws UnknownGroupIdException, ClosedApplicationException {
			try (var scope = mkScope()) {
				var transformation = getTransformation(groupId);
				exit(transformation.getInitialStore());
				transformations.remove(groupId);
			}
		}

		@Override
		public void keepFrom(LocalDateTime start) throws ClosedApplicationException {
			try (var scope = mkScope()) {
				long limitOfTimeForGC = start.toInstant(ZoneOffset.UTC).toEpochMilli();
				getEnvironment().executeInTransaction(txn -> keepPersistedOnlyNotOlderThan(limitOfTimeForGC, txn));
			}
		}

		/**
		 * Yields the transformation with the given group id, if it is currently under execution with this application.
		 * 
		 * @param groupId the group id of the transformation
		 * @return the transformation
		 * @throws UnknownGroupIdException if no transformation for the given group id is currently under execution with this application
		 */
		private MokamintStoreTransformation getTransformation(int groupId) throws UnknownGroupIdException {
			MokamintStoreTransformation transformation = transformations.get(groupId);

			if (transformation != null)
				return transformation;
			else
				throw new UnknownGroupIdException("Group id " + groupId + " is unknown");
		}
	}
}