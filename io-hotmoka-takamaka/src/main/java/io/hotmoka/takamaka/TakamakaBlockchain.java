package io.hotmoka.takamaka;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.nodes.NodeWithRequestsAndResponses;
import io.hotmoka.takamaka.internal.TakamakaBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Takamaka process.
 */
public interface TakamakaBlockchain extends NodeWithRequestsAndResponses {

	/**
	 * Yields a Takamaka blockchain.
	 * 
	 * @param config the configuration of the blockchain
	 * @return the Takamaka blockchain
	 */
	static TakamakaBlockchain of(Config config) {
		return new TakamakaBlockchainImpl(config);
	}

	/**
	 * Yields a Takamaka blockchain whose {@code postTransaction()} method
	 * is stubbed with the given implementation. This is useful for testing
	 * without the implementation of the Takamaka chain.
	 * 
	 * @param config the configuration of the blockchain
	 * @param postTransaction the implementation to use for the {@code postTransaction()} method
	 * @return the Takamaka blockchain
	 */
	static TakamakaBlockchain simulation(Config config, BiConsumer<TakamakaBlockchain, TransactionRequest<?>> postTransaction) {
		return new TakamakaBlockchainImpl(config) {

			@Override
			protected void postTransaction(TransactionRequest<?> request) {
				postTransaction.accept(this, request);
			}
		};
	}

	/**
	 * Runs the given requests in order, assuming the current time is {@code now} and
	 * the view of the store is that pointed by {@code hash}.
	 * 
	 * @param hash the pointer to the view of the world to use for the execution of the
	 *             requests; use {@code null} for the first execution, to mean that the
	 *             store is still empty
	 * @param now the moment of the execution. This value will be used for {@code now()}
	 *            in the code of the smart contracts
	 * @param requests the requests to execute, in order
	 * @param id an identifier of the execution, that will be reported inside the result
	 *           and allows to distinguish different executions
	 * @return the result of the execution
	 */
	DeltaGroupExecutionResult execute(byte[] hash, long now, Stream<TransactionRequest<?>> requests, String id);

	/**
	 * Moves the current view of the store of this blockchain to the given pointer.
	 * 
	 * @param hash the hash that points to the view that must become current for this node
	 */
	void checkOut(byte[] hash);

	/**
	 * Yields the identifier provided for the current execution, if any is being performed.
	 * 
	 * @return the identifier, if an execution is being performed
	 */
	Optional<String> getCurrentExecutionId();
}