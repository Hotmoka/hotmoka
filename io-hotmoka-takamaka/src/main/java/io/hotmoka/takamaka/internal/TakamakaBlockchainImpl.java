package io.hotmoka.takamaka.internal;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.takamaka.TakamakaBlockchainConfig;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.takamaka.code.engine.AbstractNode;

/**
 * An implementation of the Takamaka blockchain node.
 */
public class TakamakaBlockchainImpl extends AbstractNode<TakamakaBlockchainConfig, Store> implements TakamakaBlockchain {

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
	 * Builds a Takamaka blockchain node with the given configuration.
	 * 
	 * @param config the configuration
	 * @param postTransaction the function executed when a new transaction is ready
	 *                        to be added to the queue of the native Takamaka layer
	 */
	public TakamakaBlockchainImpl(TakamakaBlockchainConfig config, Consumer<TransactionRequest<?>> postTransaction) {
		super(config);

		this.postTransaction = postTransaction;
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
	public DeltaGroupExecutionResultImpl execute(byte[] hash, long now, Stream<TransactionRequest<?>> requests, Stream<BigInteger> inclusionCosts, String id) {
		if (currentExecutionId.compareAndExchange(null, id) != null)
			throw new IllegalStateException("cannot execute a delta group while another is still under execution");

		// the execution must be performed in a node whose "view of the world" is
		// that at the given hash, not necessarily at the current, checked out hash;
		// hence, we create another object, that shares the same store as this
		// (same persistent files) but checked out at hash

		class ViewAtHash extends TakamakaBlockchainImpl {

			private ViewAtHash() {
				super(TakamakaBlockchainImpl.this);
				// the cloned store is checked out at hash
				if (hash != null)
					getStore().checkout(hash);
			}

			@Override
			protected Store mkStore() {
				// we use a clone of the store
				return new Store(TakamakaBlockchainImpl.this.getStore());
			}

			@Override
			public void close() {
				// we disable the closing of the store, since otherwise also the parent of the clone would be closed
			}
		}

		try (TakamakaBlockchainImpl viewAtHash = new ViewAtHash()) {
			viewAtHash.getStore().beginTransaction(now);
			List<TransactionResponse> responses = requests.map(viewAtHash::process).collect(Collectors.toList());
			// by committing all updates, they become visible in the store, also
			// from the store of "this", since they share the same persistent files;
			// the resultingHash is the new root of the resulting store, that "points"
			// to the final, updated view of the store; however, note that the store of this object
			// has been expanded with new updates and its root is unchanged, hence these updates
			// are not visible from it unless a subsequent checkOut() moves the root to resultingHash
			byte[] resultingHash = viewAtHash.getStore().commitTransaction();

			return new DeltaGroupExecutionResultImpl(resultingHash, responses.stream(), id);
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
		finally {
			currentExecutionId.set(null);
		}
	}

	@Override
	public void checkOut(byte[] hash) {
		// we invalidate the caches since they might remember information from the previous history
		invalidateCaches();
		getStore().checkout(hash);
	}

	@Override
	public Optional<String> getCurrentExecutionId() {
		return Optional.ofNullable(currentExecutionId.get());
	}

	@Override
	protected Store mkStore() {
		return new Store(this);
	}

	@Override
	protected void postTransaction(TransactionRequest<?> request) {
		postTransaction.accept(request);
	}

	private TransactionResponse process(TransactionRequest<?> request) {
		try {
			checkTransaction(request);
			deliverTransaction(request);
			return getStore().getResponseUncommitted(referenceOf(request)).orElse(null);
		}
		catch (Exception e) {
			return null;
		}
	}
}