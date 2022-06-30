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

package io.hotmoka.takamaka.internal;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.nodes.NodeInfo;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.local.AbstractLocalNode;
import io.hotmoka.local.ResponseBuilder;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.hotmoka.takamaka.TakamakaBlockchainConfig;
import io.hotmoka.takamaka.beans.requests.MintTransactionRequest;
import io.hotmoka.takamaka.beans.responses.MintTransactionResponse;

/**
 * An implementation of the Takamaka blockchain node.
 */
@ThreadSafe
public class TakamakaBlockchainImpl extends AbstractLocalNode<TakamakaBlockchainConfig, Store> implements TakamakaBlockchain {

	/**
	 * The identifier of the execution currently being performed with this node.
	 * This contains {@code null} if no execution is being performed at the moment.
	 */
	private final AtomicReference<String> currentExecutionId = new AtomicReference<>();

	/**
	 * The function executed when a new transaction is ready
	 * to be added to the queue of the native Takamaka layer.
	 */
	private final Consumer<TransactionRequest<?>> postTransaction;

	/**
	 * The last hash at the end of an execute. This is an optimization to
	 * avoid invalidating caches if the computation continues on the same branch.
	 */
	private byte[] lastHash;

	/**
	 * Lock for accessing {@link #lastHash}.
	 */
	private final Object lastHashLock = new Object();

	/**
	 * Builds a brand new Takamaka blockchain node with the given configuration.
	 * 
	 * @param config the configuration
	 * @param consensus the consensus parameters of the node
	 * @param postTransaction the function executed when a new transaction is ready
	 *                        to be added to the queue of the native Takamaka layer
	 */
	public TakamakaBlockchainImpl(TakamakaBlockchainConfig config, ConsensusParams consensus, Consumer<TransactionRequest<?>> postTransaction) {
		super(config, consensus);

		this.postTransaction = postTransaction;
	}

	/**
	 * Builds a Takamaka blockchain with the given configuration, using an already
	 * existing store. The consensus paramters are recovered from the manifest in the store.
	 * 
	 * @param config the configuration
	 * @param postTransaction the function executed when a new transaction is ready
	 *                        to be added to the queue of the native Takamaka layer
	 */
	public TakamakaBlockchainImpl(TakamakaBlockchainConfig config, Consumer<TransactionRequest<?>> postTransaction) {
		super(config);

		this.postTransaction = postTransaction;
		caches.recomputeConsensus();
	}

	/**
	 * Builds a shallow clone of the given node.
	 * 
	 * @param parent the node to clone
	 */
	private TakamakaBlockchainImpl(TakamakaBlockchainImpl parent) {
		super(parent);

		this.postTransaction = parent.postTransaction;
	}

	@Override
	public void close() throws Exception {
		if (isNotYetClosed())
			super.close();
	}

	@Override
	public NodeInfo getNodeInfo() {
		return new NodeInfo(TakamakaBlockchain.class.getName(), "1.0.7", "");
	}

	@Override
	public DeltaGroupExecutionResultImpl execute(byte[] hash, long now, Stream<TransactionRequest<?>> requests, Stream<BigInteger> inclusionCosts, String id) {
		if (currentExecutionId.compareAndExchange(null, id) != null)
			throw new IllegalStateException("cannot execute a delta group while another is still under execution");

		List<TransactionRequest<?>> requestsAsList = requests.collect(Collectors.toList());
		List<BigInteger> inclusionCostsAsList = inclusionCosts.collect(Collectors.toList());
		ConcurrentMap<TransactionRequest<?>, BigInteger> costOfRequests = new ConcurrentHashMap<>();
		Iterator<TransactionRequest<?>> itRequests = requestsAsList.iterator();
		Iterator<BigInteger> itInclusionCosts = inclusionCostsAsList.iterator();
		while (itRequests.hasNext())
			costOfRequests.put(itRequests.next(), itInclusionCosts.next());

		// the execution must be performed in a node whose "view of the world" is
		// that at the given hash, not necessarily at the current, checked out hash;
		// hence, we create another object, that shares the same store as this
		// (same persistent files) but checked out at hash

		@ThreadSafe
		class ViewAtHash extends TakamakaBlockchainImpl {

			private ViewAtHash() {
				super(TakamakaBlockchainImpl.this);

				// the cloned store is checked out at hash
				if (hash != null)
					store.checkout(hash);
			}

			@Override
			protected Store mkStore() {
				// we use a clone of the store
				return new Store(TakamakaBlockchainImpl.this.store);
			}

			@Override
			protected BigInteger getRequestStorageCost(NonInitialTransactionRequest<?> request) {
				BigInteger costOfRequest = costOfRequests.get(request);
				if (costOfRequest != null)
					// we add the inclusion cost in the Takamaka blockchain
					return super.getRequestStorageCost(request).add(costOfRequest);
				else
					// we cost of request is null for run transactions
					return super.getRequestStorageCost(request);
			}

			@Override
			public void close() {
				// we disable the closing of the store, since otherwise also the parent of the clone would be closed
			}

			private TransactionResponse process(TransactionRequest<?> request) {
				try {
					checkTransaction(request);
					return deliverTransaction(request);
				}
				catch (Exception e) {
					return null;
				}
			}
		}

		try (ViewAtHash viewAtHash = new ViewAtHash()) {
			viewAtHash.store.beginTransaction(now);
			List<TransactionResponse> responses = requestsAsList.stream().map(viewAtHash::process).collect(Collectors.toList());
			// by committing all updates, they become visible in the store, also
			// from the store of "this", since they share the same persistent files;
			// the lastHash is the new root of the resulting store, that "points"
			// to the final, updated view of the store; however, note that the store of this object
			// has been expanded with new updates and its root is unchanged, hence these updates
			// are not visible from it until a subsequent checkOut() moves the root to lastHash
			synchronized (lastHashLock) {
				lastHash = viewAtHash.store.commitTransaction();
			}

			return new DeltaGroupExecutionResultImpl(lastHash, responses.stream(), id);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
		finally {
			currentExecutionId.set(null);
		}
	}

	@Override
	public void checkOut(byte[] hash) {
		// we invalidate the caches if we are switching branch,
		// since they might remember information from the previous branch
		synchronized (lastHashLock) {
			if (lastHash == null || !Arrays.equals(lastHash, hash))
				invalidateCaches();
		}

		store.checkout(hash);
	}

	@Override
	protected void scheduleForNotificationOfEvents(TransactionResponseWithEvents response) {
		notifyEventsOf(response);
	}

	@Override
	public Optional<String> getCurrentExecutionId() {
		return Optional.ofNullable(currentExecutionId.get());
	}

	@Override
	public final void addMintTransaction(MintTransactionRequest request) throws TransactionRejectedException, TransactionException {
		wrapInCaseOfExceptionMedium(() -> { postMintTransaction(request).get(); return null; });
	}

	@Override
	public final MintSupplier postMintTransaction(MintTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = post(request);
			return mintSupplierFor(reference, () -> { ((MintTransactionResponse) getPolledResponse(reference)).getOutcome(); return null; });
		});
	}

	@Override
	protected Store mkStore() {
		return new Store(this);
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) {
		postTransaction.accept(request);
	}

	@Override
	protected ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws TransactionRejectedException {
		// we redefine this method in order to deal with the following extra type of requests, that are specific to this node
		if (request instanceof MintTransactionRequest)
			return new MintResponseBuilder(reference, (MintTransactionRequest) request, this);
		else
			return super.responseBuilderFor(reference, request);
	}

	@Override
	protected boolean admitsAfterInitialization(InitialTransactionRequest<?> request) {
		// we allow the creation of gametes, which is how wallets can create their account
		// without the help from other already existing accounts
		return super.admitsAfterInitialization(request) || request instanceof GameteCreationTransactionRequest;
	}

	/**
	 * Adapts a callable into a mint supplier.
	 * 
	 * @param reference the reference of the request whose future is being built
	 * @param task the callable
	 * @return the mint supplier
	 */
	protected final MintSupplier mintSupplierFor(TransactionReference reference, Callable<Void> task) {
		return new MintSupplier() {
	
			@Override
			public TransactionReference getReferenceOfRequest() {
				return reference;
			}
	
			@Override
			public void get() throws TransactionRejectedException, TransactionException {
				wrapInCaseOfExceptionMedium(task);
			}
		};
	}
}