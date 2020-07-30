package io.hotmoka.takamaka;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.nodes.Node;
import io.hotmoka.takamaka.internal.TakamakaBlockchainImpl;

/**
 * An implementation of a blockchain that relies on a Takamaka process.
 */
public interface TakamakaBlockchain extends Node {

	/**
	 * Yields a Takamaka blockchain.
	 * 
	 * @param config the configuration of the blockchain
	 * @param postTransaction the function executed when a new transaction is ready
	 *                        to be added to the queue of the native Takamaka layer
	 * @return the Takamaka blockchain
	 */
	static TakamakaBlockchain of(TakamakaBlockchainConfig config, Consumer<TransactionRequest<?>> postTransaction) {
		return new TakamakaBlockchainImpl(config, postTransaction);
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
	 * @param inclusionCosts the costs of inclusion in blockchain of the requests
	 * @param id an identifier of the execution, that will be reported inside the result
	 *           and allows to distinguish different executions
	 * @return the result of the execution
	 */
	DeltaGroupExecutionResult execute(byte[] hash, long now,
		Stream<TransactionRequest<?>> requests, Stream<BigInteger> inclusionCosts, String id);

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