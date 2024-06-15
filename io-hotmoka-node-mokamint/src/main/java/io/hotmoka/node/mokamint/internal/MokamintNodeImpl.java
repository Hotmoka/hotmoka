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
import io.hotmoka.node.ClosedNodeException;
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
import io.mokamint.node.api.Transaction;
import io.mokamint.node.local.AlreadyInitializedException;
import io.mokamint.node.local.LocalNodes;
import io.mokamint.node.local.api.LocalNode;
import io.mokamint.node.local.api.LocalNodeConfig;
import io.mokamint.node.Transactions;
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

		//try {
		if (init) {
			initWithEmptyStore();
		}
		else
			initWithSavedStore();

		try {
			this.mokamintNode = LocalNodes.of(mokamintConfig, keyPair, new MokamintHotmokaApplication(), init);
		}
		catch (AlreadyInitializedException | TimeoutException | ApplicationException | io.mokamint.node.api.NodeException e) {
			throw new NodeException(e);
		}

		//}
		/*catch (IOException | TimeoutException e) {
			LOGGER.log(Level.SEVERE, "the creation of the Mokamint node failed", e);
			close();
			throw new NodeException("The creation of the Mokamint node failed", e);
		}*/
	}

	@Override
	public NodeInfo getNodeInfo() throws ClosedNodeException {
		try (var scope = mkScope()) {
			return NodeInfos.of(MokamintNode.class.getName(), HOTMOKA_VERSION, "");
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
	protected void postRequest(TransactionRequest<?> request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		try {
			mokamintNode.add(Transactions.of(request.toByteArray()));
		}
		catch (io.mokamint.node.api.TransactionRejectedException e) {
			throw new TransactionRejectedException(e.getMessage());
		}
		catch (io.mokamint.node.api.NodeException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, NodeException {
		super.checkTransaction(request);
	}

	@Override
	protected void signalRejected(TransactionRequest<?> request, TransactionRejectedException e) {
		super.signalRejected(request, e);
	}

	@Override
	protected MokamintStoreTransformation beginTransaction(long now) throws NodeException {
		return super.beginTransaction(now);
	}

	@Override
	protected void moveToFinalStoreOf(MokamintStoreTransformation transaction) throws NodeException {
		super.moveToFinalStoreOf(transaction);
	}

	private class MokamintHotmokaApplication extends AbstractApplication {

		/**
		 * The current store transactions, for each group id of Hotmoka transactions.
		 */
		//private final ConcurrentMap<Integer, MokamintStoreTransformation> transactions = new ConcurrentHashMap<>();
		private volatile MokamintStoreTransformation transformation;

		@Override
		public boolean checkPrologExtra(byte[] extra) throws ApplicationException, TimeoutException, InterruptedException {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void checkTransaction(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ApplicationException, TimeoutException, InterruptedException {
			try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
				try {
					MokamintNodeImpl.this.checkTransaction(TransactionRequests.from(context));
				}
				catch (IOException e) {
					throw new io.mokamint.node.api.TransactionRejectedException("The transaction request cannot be computed", e);
				}
				catch (TransactionRejectedException e) {
					throw new io.mokamint.node.api.TransactionRejectedException("The transaction request has been rejected", e);
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
					throw new io.mokamint.node.api.TransactionRejectedException("The transaction request cannot be computed", e);
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
		public int beginBlock(long height, LocalDateTime when, byte[] stateId) throws UnknownStateException, ApplicationException, TimeoutException, InterruptedException {
			try {
				transformation = beginTransaction(when.toEpochSecond(ZoneOffset.UTC));
			}
	    	catch (NodeException e) {
	    		throw new ApplicationException(e);
	    	}

			return 0;
		}

		@Override
		public void deliverTransaction(int groupId, Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, UnknownGroupIdException, ApplicationException, TimeoutException, InterruptedException {
			try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
	        	TransactionRequest<?> hotmokaRequest;

	        	try {
	        		hotmokaRequest = TransactionRequests.from(context);
	        	}
	        	catch (IOException e) {
	        		throw new io.mokamint.node.api.TransactionRejectedException("The transaction request cannot be computed", e);
	        	}

	        	try {
	        		transformation.deliverTransaction(hotmokaRequest);
	        	}
	        	catch (TransactionRejectedException e) {
	        		signalRejected(hotmokaRequest, e);
	        		throw new io.mokamint.node.api.TransactionRejectedException("The transaction request has been rejected", e);
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
		public byte[] endBlock(int groupId, Deadline deadline) throws ApplicationException, UnknownGroupIdException, TimeoutException, InterruptedException {
			return getStore().getStateId().getBytes();
		}

		@Override
		public void commitBlock(int groupId) throws ApplicationException, UnknownGroupIdException, TimeoutException, InterruptedException {
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