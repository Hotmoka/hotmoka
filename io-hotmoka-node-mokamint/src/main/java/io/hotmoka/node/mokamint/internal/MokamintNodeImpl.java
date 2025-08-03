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
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import io.hotmoka.annotations.GuardedBy;
import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
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
// TODO: the (Mokamint) Hotmoka node and the underlying Mokamint engine are intertwined here.
// This should change: let the Hotmoka node receive a Mokamint application in its constructor
// and let the Mokamint application receive a supplier of a Hotmoka node
@ThreadSafe
public class MokamintNodeImpl extends AbstractTrieBasedLocalNode<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> implements MokamintNode {

	/**
	 * The underlying Mokamint engine.
	 */
	private final AbstractLocalNode mokamintNode;

	/**
	 * A lock for accessing {@link #lastHeadStateId} and {@link #storeOfHead}.
	 */
	private final Object headLock = new Object();

	/**
	 * A cache for the caches at a given state identifier. This allows to reuse
	 * a previous cache if an old state identifier is checked out. The alternative
	 * is to recompute the cache at the state identifier, which is expensive.
	 */
	private final LRUCache<StateId, StoreCache> stateIdentifiersCache = new LRUCache<>(100, 1000);

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

	/**
	 * The hashing algorithm to use for the bytes of a transaction, in order to get its reference.
	 */
	private final HashingAlgorithm sha256;

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

		try {
			this.sha256 = HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new LocalNodeException(e);
		}

		this.mokamintNode = new AbstractLocalNode(mokamintConfig, keyPair, new MokamintHotmokaApplication(), createGenesis) {};

		// if the supporting mokamint node gets closed, also this node gets closed;
		// normally, this is not expected to happen, but better consider this possibility
		this.mokamintNode.addOnCloseHandler(this::close);

		int size = getLocalConfig().getIndexSize();
		if (size > 0) {
			var indexer = new Indexer(this, getStoreOfNode(), getEnvironment(), size);
			getExecutors().execute(indexer::run);
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
			MokamintStore result = mkStore(si, Optional.ofNullable(stateIdentifiersCache.get(si))); // TODO: who guarantees that the store at si has not been garbage-collected?

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
			// the mempool of the Mokamint engine has rejected the transaction
			signalRejected(request, new TransactionRejectedException(e.getMessage()));
		}
		catch (io.mokamint.node.api.ClosedNodeException e) {
			throw new ClosedNodeException(e);
		}
		catch (ApplicationTimeoutException e) {
			throw new LocalNodeException("Unexpected exception: the application is local and its method never go into timeout", e);
		}
	}

	private class MokamintHotmokaApplication extends AbstractApplication {

		/**
		 * The current store transformations, for each group id of Hotmoka transactions.
		 */
		private final ConcurrentMap<Integer, MokamintStoreTransformation> transformations = new ConcurrentHashMap<>();

		/**
		 * The final state ids for the transformations in {@link #transformations}.
		 */
		private final ConcurrentMap<Integer, StateId> finalStateIds = new ConcurrentHashMap<>();

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
				start = enter(si, Optional.ofNullable(stateIdentifiersCache.get(si)));
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
				TransactionRequest<?> hotmokaRequest = intoHotmokaRequest(transaction); // TODO: should I signalRejected also if this fails?
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
				StateId idOfFinalStore = getEnvironment().computeInTransaction(transformation::getIdOfFinalStore);
				finalStateIds.put(groupId, idOfFinalStore);
				stateIdentifiersCache.put(idOfFinalStore, transformation.getCache());

				return idOfFinalStore.getBytes();
			}
		}

		@Override
		public void commitBlock(int groupId) throws UnknownGroupIdException, ClosedApplicationException {
			try (var scope = mkScope()) {
				var transformation = getTransformation(groupId);
				var idOfFinalStore = finalStateIds.get(groupId);
				exit(transformation.getInitialStore());
				getEnvironment().executeInTransaction(txn -> persist(idOfFinalStore, transformation.getNow(), txn));

				LOGGER.fine(() -> "persisted state " + idOfFinalStore);

				transformations.remove(groupId);
				finalStateIds.remove(groupId);
			}
		}

		@Override
		public void abortBlock(int groupId) throws UnknownGroupIdException, ClosedApplicationException {
			try (var scope = mkScope()) {
				var transformation = getTransformation(groupId);
				var idOfFinalStore = finalStateIds.get(groupId);
				exit(transformation.getInitialStore());

				// the final store of the transformation is not useful anymore
				getEnvironment().executeInTransaction(txn -> {
					try {
						free(idOfFinalStore, txn);
					}
					catch (UnknownStateIdException e) {
						// impossible, we have just computed this id inside endBlock(), which was meant to be called before this method
						throw new LocalNodeException("State id " + idOfFinalStore + " has been just computed: it must have existed", e);
					}
				});

				transformations.remove(groupId);
				finalStateIds.remove(groupId);
			}
		}

		@Override
		public void keepFrom(LocalDateTime start) throws ClosedApplicationException {
			try (var scope = mkScope()) {
				long limitOfTimeForGC = start.toInstant(ZoneOffset.UTC).toEpochMilli();
				getEnvironment().executeInTransaction(txn -> keepPersistedOnlyNotOlderThan(limitOfTimeForGC, txn));
			}
		}

		@Override
		public void publish(Block block) throws ClosedApplicationException, InterruptedException {
			try (var scope = mkScope()) {
				if (block instanceof NonGenesisBlock ngb) {
					var si = StateIds.of(block.getStateId());
					
					try {
						MokamintStore store = enter(si, Optional.ofNullable(stateIdentifiersCache.get(si)));

						try {
							ngb.getTransactions().forEachOrdered(tx -> publish(tx, store));
						}
						finally {
							exit(store);
						}
					}
					catch (UnknownStateIdException e) {
						// this might happen if publishing is very slow or garbage-collection is very aggressive
						LOGGER.warning("cannot publish the events in block " + block.getHexHash() + ": its state has been garbage-collected: " + e.getMessage());
					}
				}
			}
		}

		private void publish(Transaction tx, MokamintStore store) {
			try {
				var reference = TransactionReferences.of(tx.getHash(sha256));
				MokamintNodeImpl.this.publish(reference, store.getResponse(reference), store);
			}
			catch (UnknownReferenceException e) {
				// the transactions have been delivered and the store is immutable, if they cannot be found at the end of the block then there is a problem in the database
				throw new LocalNodeException("Already delivered transactions should be in store", e);
			}
		}

		private static TransactionRequest<?> intoHotmokaRequest(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException {
			try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
				try {
		    		return TransactionRequests.from(context);
		    	}
		    	catch (IOException e) {
		    		throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage());
		    	}
		    }
			catch (IOException e) {
				// this is thrown by implicit context.close() call; this it is not expected to happen,
				// since we are working on a ByteArrayInputStream, that is not a real file and whose close() throws no exception
				throw new LocalNodeException(e);
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