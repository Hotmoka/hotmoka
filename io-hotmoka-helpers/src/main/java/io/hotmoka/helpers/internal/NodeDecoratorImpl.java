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

package io.hotmoka.helpers.internal;

import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A partial implementation of a decorator of a node. It forwards all calls to the decorated node.
 * Subclasses will add the decoration.
 * 
 * @param <N> the type of the decorated node
 */
public abstract class NodeDecoratorImpl<N extends Node> implements Node {

	/**
	 * The node that is decorated.
	 */
	private final N parent;

	/**
	 * True if and only if this node has been closed already.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

	/**
	 * Creates a decorated node.
	 * 
	 * @param parent the node to decorate
	 */
	protected NodeDecoratorImpl(N parent) {
		this.parent = parent;

		// when the parent is closed, this decorator will be closed as well
		parent.addOnCloseHandler(this_close);
	}

	/**
	 * Yields the decorated node.
	 * 
	 * @return the decorated node
	 */
	protected final N getParent() {
		return parent;
	}

	/**
	 * Ensures that this node is not closed yet. This node might be closed after
	 * {@link #close()} has been called on it or after the parent node has been closed.
	 * 
	 * @throws ClosedNodeException if this node is already closed
	 */
	protected final void ensureNotClosed() throws ClosedNodeException {
		if (isClosed.get())
			throw new ClosedNodeException();
	}

	@Override
	public void close() throws InterruptedException, NodeException {
		if (!isClosed.getAndSet(true))
			parent.close();
	}

	@Override
	public StorageReference getManifest() throws NodeException, TimeoutException, InterruptedException {
		return parent.getManifest();
	}

	@Override
	public TransactionReference getTakamakaCode() throws NodeException, TimeoutException, InterruptedException {
		return parent.getTakamakaCode();
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, TimeoutException, InterruptedException {
		return parent.getNodeInfo();
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		return parent.getClassTag(reference);
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getState(reference);
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return parent.addJarStoreInitialTransaction(request);
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return parent.addGameteCreationTransaction(request);
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, NodeException, TimeoutException, InterruptedException {
		return parent.addJarStoreTransaction(request);
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.addConstructorCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.addInstanceMethodCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.addStaticMethodCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.runInstanceMethodCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.runStaticMethodCallTransaction(request);
	}

	@Override
	public JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postJarStoreTransaction(request);
	}

	@Override
	public ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postConstructorCallTransaction(request);
	}

	@Override
	public MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postInstanceMethodCallTransaction(request);
	}

	@Override
	public MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postStaticMethodCallTransaction(request);
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		parent.addInitializationTransaction(request);
	}

	@Override
	public ConsensusConfig<?,?> getConfig() throws NodeException, TimeoutException, InterruptedException {
		return parent.getConfig();
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getRequest(reference);
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getResponse(reference);
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		return parent.getPolledResponse(reference);
	}

	@Override
	public Subscription subscribeToEvents(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) throws NodeException {
		return parent.subscribeToEvents(key, handler);
	}

	@Override
	public void addOnCloseHandler(OnCloseHandler handler) {
		parent.addOnCloseHandler(handler);
	}

	@Override
	public void removeOnCloseHandler(OnCloseHandler handler) {
		parent.removeOnCloseHandler(handler);
	}
}