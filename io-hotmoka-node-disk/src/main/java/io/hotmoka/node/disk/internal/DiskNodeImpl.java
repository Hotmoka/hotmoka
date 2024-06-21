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

package io.hotmoka.node.disk.internal;

import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.disk.api.DiskNode;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.api.StoreException;

/**
 * An implementation of a node that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining,
 * nor transactions. Updates are stored in files, rather than in an external database.
 */
@ThreadSafe
public class DiskNodeImpl extends AbstractLocalNode<DiskNodeImpl, DiskNodeConfig, DiskStore, DiskStoreTransformation> implements DiskNode {

	/**
	 * The mempool where transaction requests are stored and eventually executed.
	 */
	private final Mempool mempool;

	/**
	 * Builds a new disk memory node.
	 * 
	 * @param config the configuration of the node
	 * @throws NodeException if the operation cannot be completed correctly
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public DiskNodeImpl(DiskNodeConfig config) throws NodeException, InterruptedException {
		super(config, true);

		try {
			this.mempool = new Mempool(this, config.getTransactionsPerBlock());
		}
		catch (NodeException e) {
			close();
			throw e;
		}
	}

	@Override
	public NodeInfo getNodeInfo() throws ClosedNodeException {
		try (var scope = mkScope()) {
			return NodeInfos.of(DiskNode.class.getName(), HOTMOKA_VERSION, "");
		}
	}

	@Override
	protected DiskStore mkStore() throws NodeException {
		try {
			return new DiskStore(this);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void closeResources() throws NodeException, InterruptedException {
		try {
			mempool.stop();
		}
		finally {
			super.closeResources();
		}
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) throws InterruptedException, TimeoutException {
		mempool.add(request);
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
	protected void moveToFinalStoreOf(DiskStoreTransformation transaction) throws NodeException {
		super.moveToFinalStoreOf(transaction);
	}

	@Override
	protected DiskStore getStore() {
		return super.getStore();
	}
}