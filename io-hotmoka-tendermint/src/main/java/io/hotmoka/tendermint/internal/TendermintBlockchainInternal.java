package io.hotmoka.tendermint.internal;

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.TendermintValidator;

/**
 * The view of the Tendermint blockchain that is used in the implementation of the module.
 * This allows one to enlarge the visibility of some methods, only for the implementation classes.
 */
public interface TendermintBlockchainInternal {

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration
	 */
	TendermintBlockchainConfig getConfig();

	/**
	 * Yields the store of the node.
	 * 
	 * @return the store
	 */
	Store getStore();

	/**
	 * Yields the proxy to the Tendermint process.
	 */
	Tendermint getTendermint();

	/**
	 * Ask the Tendermint process about the current validators of the Tendermint blockchain.
	 * 
	 * @return the current validators
	 */
	Stream<TendermintValidator> getTendermintValidators();

	/**
	 * Yields the error message trimmed to a maximal length, to avoid overflow.
	 *
	 * @param t the throwable whose error message is processed
	 * @return the resulting message
	 */
	String trimmedMessage(Throwable t);

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 */
	void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException;

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @return the response; if this node has a notion of commit, this response is typically
	 *         still uncommitted
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException;

	/**
	 * Extracts from store the validators that has been saved into the validators contract.
	 * 
	 * @return the validators, if the node has been initialized already
	 */
	Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException;

	void commitTransactionAndCheckout();

	/**
	 * Rewards the validators with the cost of the gas consumed by the
	 * transactions in the last block. This is meaningful only if the
	 * node has some form of commit.
	 * 
	 * @param behaving the space-separated sequence of identifiers of the
	 *                 validators that behaved correctly during the creation
	 *                 of the last block
	 * @param misbehaving the space-separated sequence of the identifiers that
	 *                    misbehaved during the creation of the last block
	 * @return true if and only if rewarding was performed; rewarding might not be
	 *         performed because the manifest is not yet installed or because
	 *         the code of the validators contract failed
	 */
	boolean rewardValidators(String behaving, String misbehaving);
}