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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.AbstractTrieBasedLocalNode;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.mokamint.application.AbstractApplication;
import io.mokamint.application.api.ApplicationException;
import io.mokamint.application.api.UnknownGroupIdException;
import io.mokamint.application.api.UnknownStateException;
import io.mokamint.node.Transactions;
import io.mokamint.node.api.Block;
import io.mokamint.node.api.NonGenesisBlock;
import io.mokamint.node.api.Transaction;
import io.mokamint.node.local.AbstractLocalNode;
import io.mokamint.node.local.AlreadyInitializedException;
import io.mokamint.node.local.api.LocalNode;
import io.mokamint.node.local.api.LocalNodeConfig;
import io.mokamint.nonce.api.Deadline;

/**
 * An implementation of a blockchain nodes that rely on the Mokamint proof of space engine.
 */
@ThreadSafe
public class MokamintNodeImpl extends AbstractTrieBasedLocalNode<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> implements MokamintNode {

	private final LocalNode mokamintNode;

	private final static Logger LOGGER = Logger.getLogger(MokamintNodeImpl.class.getName());

	/**
	 * Builds a new Hotmoka node based on the Mokamint engine.
	 * 
	 * @param config the configuration of the node
	 * @param mokamintConfig the configuration of the underlying Mokamint node
	 * @param init true if and only if the working directory of the node must be initialized
	 * @param createGenesis if true, creates a genesis block and starts mining on top
	 *                   (initial synchronization is consequently skipped)
	 * @throws NodeException if the operation cannot be completed correctly
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public MokamintNodeImpl(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair, boolean init, boolean createGenesis) throws InvalidKeyException, SignatureException, NodeException, InterruptedException {
		super(config, init);

		try {
			this.mokamintNode = new AbstractLocalNode(mokamintConfig, keyPair, new MokamintHotmokaApplication(), createGenesis) {

				@Override
				protected void onHeadChanged(Block newHead) {
					StateId idOfStoreOfHead = StateIds.of(newHead.getStateId());

					try {
						setStoreOfHead(mkStore(idOfStoreOfHead));
						CheckRunnable.check(NodeException.class, () -> getEnvironment().executeInTransaction(UncheckConsumer.uncheck(
							txn -> keepPersistedOnly(Set.of(idOfStoreOfHead), txn)
						)));
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						LOGGER.log(Level.SEVERE, "could not set the head to " + idOfStoreOfHead, e);
						return;
					}
					catch (NodeException | UnknownStateIdException e) {
						LOGGER.log(Level.SEVERE, "could not set the head to " + idOfStoreOfHead, e);
						return;
					}

					super.onHeadChanged(newHead);
					
					if (newHead instanceof NonGenesisBlock ngb)
						toPublish.offer(ngb);
				}
			};

			var maybeHeadHash = mokamintNode.getChainInfo().getHeadHash();

			if (maybeHeadHash.isPresent()) {
				var head = mokamintNode.getBlock(maybeHeadHash.get())
						.orElseThrow(() -> new NodeException("The node has a head set but it cannot be found in its database"));

				setStoreOfHead(mkStore(StateIds.of(head.getStateId())));
			}
		}
		catch (AlreadyInitializedException | TimeoutException | ApplicationException | io.mokamint.node.api.NodeException | UnknownStateIdException e) {
			throw new NodeException(e);
		}

		getExecutors().execute(this::publishBlocks);
	}

	private final BlockingQueue<NonGenesisBlock> toPublish = new LinkedBlockingDeque<>();

	private void publishBlocks() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				NonGenesisBlock next = toPublish.take();

				try {
					MokamintStore store = mkStore(StateIds.of(next.getStateId()));

					for (var tx: next.getTransactions().toArray(Transaction[]::new))
						publish(TransactionReferences.of(getHasher().hash(intoHotmokaRequest(tx))), store);
				}
				catch (ApplicationException | NodeException | io.mokamint.node.api.TransactionRejectedException | UnknownStateIdException e) {
					LOGGER.log(Level.SEVERE, "failed to publish the transactions in a block", e);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, InterruptedException, TimeoutException {
		try (var scope = mkScope()) {
			return NodeInfos.of(MokamintNode.class.getName(), HOTMOKA_VERSION, mokamintNode.getInfo().getUUID().toString());
		}
		catch (io.mokamint.node.api.NodeException e) {
			throw new NodeException(e);
		}
	}

	public LocalNode getMokamintNode() {
		return mokamintNode;
	}

	@Override
	protected MokamintStore mkStore() throws NodeException {
		try {
			return new MokamintStore(this);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void closeResources() throws NodeException, InterruptedException {
		try {
			mokamintNode.close();
		}
		catch (io.mokamint.node.api.NodeException e) {
			throw new NodeException(e);
		}
		finally {
			super.closeResources();
		}
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) throws NodeException, InterruptedException, TimeoutException {
		try {
			mokamintNode.add(Transactions.of(request.toByteArray()));
		}
		catch (io.mokamint.node.api.TransactionRejectedException e) {
			// the mempool of the Mokamint engine has rejected the transaction:
			// the node has been already signaled that it failed, so there is nothing to do here
		}
		catch (io.mokamint.node.api.NodeException e) {
			throw new NodeException(e);
		}
	}

	private TransactionRequest<?> intoHotmokaRequest(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ApplicationException {
		try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
			try {
        		return TransactionRequests.from(context);
        	}
        	catch (IOException e) {
        		throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
        	}
        }
		catch (IOException e) {
			throw new ApplicationException(e);
		}
	}

	private class MokamintHotmokaApplication extends AbstractApplication {

		/**
		 * The current store transformations, for each group id of Hotmoka transactions.
		 */
		private final ConcurrentMap<Integer, MokamintStoreTransformation> transformations = new ConcurrentHashMap<>();

		private final AtomicInteger nextId = new AtomicInteger();

		@Override
		public boolean checkPrologExtra(byte[] extra) throws ApplicationException {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void checkTransaction(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ApplicationException, TimeoutException, InterruptedException {
			TransactionRequest<?> hotmokaRequest = intoHotmokaRequest(transaction);

			try {
				MokamintNodeImpl.this.checkTransaction(hotmokaRequest);
			}
			catch (TransactionRejectedException e) {
				signalRejected(hotmokaRequest, e);
				throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
			}
			catch (NodeException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public long getPriority(Transaction transaction) {
			return 0;
		}

		@Override
		public String getRepresentation(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ApplicationException {
			return intoHotmokaRequest(transaction).toString();
		}

		@Override
		public byte[] getInitialStateId() {
			return new byte[128];
		}

		@Override
		public int beginBlock(long height, LocalDateTime when, byte[] stateId) throws UnknownStateException, ApplicationException, InterruptedException {
			MokamintStore start;

			try {
				start = enter(StateIds.of(stateId));
			}
			catch (NodeException e) {
				throw new ApplicationException(e);
			}
			catch (UnknownStateIdException e) {
				throw new UnknownStateException(e);
			}

			int groupId = nextId.getAndIncrement();

			try {
				transformations.put(groupId, start.beginTransformation(when.toInstant(ZoneOffset.UTC).toEpochMilli()));
			}
			catch (StoreException e) {
				try {
					exit(start);
				}
				catch (NodeException e2) {
					throw new ApplicationException(e2);
				}

				throw new ApplicationException(e);
	    	}

			return groupId;
		}

		@Override
		public void deliverTransaction(int groupId, Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, UnknownGroupIdException, ApplicationException, InterruptedException {
			TransactionRequest<?> hotmokaRequest = intoHotmokaRequest(transaction);

			MokamintStoreTransformation transformation = getTransformation(groupId);

			try {
				transformation.deliverTransaction(hotmokaRequest);
			}
			catch (TransactionRejectedException e) {
				signalRejected(hotmokaRequest, e);
				throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
			}
			catch (StoreException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public byte[] endBlock(int groupId, Deadline deadline) throws ApplicationException, UnknownGroupIdException {
			MokamintStoreTransformation transformation = getTransformation(groupId);
			persisted++;

			try {
				return CheckSupplier.check(NodeException.class, StoreException.class, () -> getEnvironment().computeInTransaction(UncheckFunction.uncheck(txn -> {
					StateId stateIdOfFinalStore = transformation.getIdOfFinalStore(txn);
					persist(stateIdOfFinalStore, txn);
					return stateIdOfFinalStore.getBytes();
				})));
			}
			catch (StoreException | NodeException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public void commitBlock(int groupId) throws UnknownGroupIdException, ApplicationException {
			var transformation = getTransformation(groupId);
			transformations.remove(groupId);

			try {
				exit(transformation.getInitialStore());
			}
			catch (NodeException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public void abortBlock(int groupId) throws UnknownGroupIdException, ApplicationException {
			var transformation = getTransformation(groupId);
			transformations.remove(groupId);

			try {
				exit(transformation.getInitialStore());
			}
			catch (NodeException e) {
				throw new ApplicationException(e);
			}
		}

		private MokamintStoreTransformation getTransformation(int groupId) throws UnknownGroupIdException {
			MokamintStoreTransformation transformation = transformations.get(groupId);
			if (transformation == null)
				throw new UnknownGroupIdException("Group id " + groupId + " is unknown");
			else
				return transformation;
		}
	}
}