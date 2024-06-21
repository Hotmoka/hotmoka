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
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.AbstractTrieBasedLocalNode;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.mokamint.application.AbstractApplication;
import io.mokamint.application.api.ApplicationException;
import io.mokamint.application.api.UnknownGroupIdException;
import io.mokamint.application.api.UnknownStateException;
import io.mokamint.node.Transactions;
import io.mokamint.node.api.Transaction;
import io.mokamint.node.local.AlreadyInitializedException;
import io.mokamint.node.local.LocalNodes;
import io.mokamint.node.local.api.LocalNode;
import io.mokamint.node.local.api.LocalNodeConfig;
import io.mokamint.nonce.api.Deadline;

/**
 * An implementation of a blockchain nodes that rely on the Mokamint proof of space engine.
 */
@ThreadSafe
public class MokamintNodeImpl extends AbstractTrieBasedLocalNode<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> implements MokamintNode {

	private final LocalNode mokamintNode;

	//private final static Logger LOGGER = Logger.getLogger(MokamintNodeImpl.class.getName());


	/**
	 * Builds a new disk memory node.
	 * 
	 * @param config the configuration of the node
	 * @throws NodeException if the operation cannot be completed correctly
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public MokamintNodeImpl(MokamintNodeConfig config, LocalNodeConfig mokamintConfig, KeyPair keyPair, boolean init) throws InvalidKeyException, SignatureException, NodeException, InterruptedException {
		super(config, init);

		try {
			this.mokamintNode = LocalNodes.of(mokamintConfig, keyPair, new MokamintHotmokaApplication(), init);
		}
		catch (AlreadyInitializedException | TimeoutException | ApplicationException | io.mokamint.node.api.NodeException e) {
			throw new NodeException(e);
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
	protected MokamintStore mkStore(StateId stateId) throws NodeException {
		try {
			return new MokamintStore(this, stateId);
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

	private class MokamintHotmokaApplication extends AbstractApplication {

		/**
		 * The current store transactions, for each group id of Hotmoka transactions.
		 */
		//private final ConcurrentMap<Integer, MokamintStoreTransformation> transactions = new ConcurrentHashMap<>();
		private volatile MokamintStoreTransformation transformation;

		@Override
		public boolean checkPrologExtra(byte[] extra) throws ApplicationException {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void checkTransaction(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ApplicationException, TimeoutException, InterruptedException {
			try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
				TransactionRequest<?> hotmokaRequest;

				try {
	        		hotmokaRequest = TransactionRequests.from(context);
	        	}
	        	catch (IOException e) {
	        		throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
	        	}

				try {
					MokamintNodeImpl.this.checkTransaction(hotmokaRequest);
				}
				catch (TransactionRejectedException e) {
					signalRejected(hotmokaRequest, e);
					throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
				}
	        }
			catch (IOException | NodeException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public long getPriority(Transaction transaction) {
			return 0;
		}

		@Override
		public String getRepresentation(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ApplicationException {
			try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
				try {
					return TransactionRequests.from(context).toString();
				}
				catch (IOException e) {
					throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
				}
	        }
			catch (IOException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public byte[] getInitialStateId() {
			return new byte[128];
		}

		@Override
		public int beginBlock(long height, LocalDateTime when, byte[] stateId) throws UnknownStateException, ApplicationException {
			try {
				transformation = getStore() //.checkedOutAt(StateIds.of(stateId))
					.beginTransaction(when.toInstant(ZoneOffset.UTC).toEpochMilli());
			}
			catch (StoreException e) {
	    		throw new ApplicationException(e);
	    	}

			return 0;
		}

		@Override
		public void deliverTransaction(int groupId, Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, UnknownGroupIdException, ApplicationException {
			try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
	        	TransactionRequest<?> hotmokaRequest;

	        	try {
	        		hotmokaRequest = TransactionRequests.from(context);
	        	}
	        	catch (IOException e) {
	        		throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
	        	}

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
			catch (IOException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public byte[] endBlock(int groupId, Deadline deadline) throws ApplicationException, UnknownGroupIdException {
			return getStore().getStateId().getBytes();
		}

		@Override
		public void commitBlock(int groupId) throws ApplicationException, UnknownGroupIdException {
			try {
				moveToFinalStoreOf(transformation);
			}
			catch (NodeException e) {
				throw new ApplicationException(e);
			}
		}

		@Override
		public void abortBlock(int groupId) throws ApplicationException, UnknownGroupIdException, TimeoutException, InterruptedException {
			// TODO Auto-generated method stub
		}
	}
}