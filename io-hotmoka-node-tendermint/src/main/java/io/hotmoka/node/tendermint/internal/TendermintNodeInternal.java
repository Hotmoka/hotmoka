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

package io.hotmoka.node.tendermint.internal;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;

/**
 * The view of the Tendermint node that is used in the implementation of the module.
 * This allows one to enlarge the visibility of some methods, only for the implementation classes.
 */
public interface TendermintNodeInternal {

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration
	 */
	TendermintNodeConfig getConfig();

	/**
	 * Yields the store of the node.
	 * 
	 * @return the store
	 */
	Store getStore();

	/**
	 * Yields an object that can be used to post requests to the Tendermint process.
	 * 
	 * @return the object for posting requests
	 */
	TendermintPoster getPoster();

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
	Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, NoSuchElementException;

	/**
	 * Commits the current transaction in the database of the state.
	 */
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

	/**
	 * 
	 * Sets the time to use for the currently executing transaction.
	 * 
	 * @param now the time
	 */
	void setNow(long now);
}