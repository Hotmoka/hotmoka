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

package io.hotmoka.node.local.api;

import java.io.IOException;
import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.Optional;

import io.hotmoka.beans.api.nodes.ConsensusConfig;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;

/**
 * The cache of a local node.
 */
public interface NodeCache {

	/**
	 * Invalidates the information in this cache, after the execution of a transaction with the given classloader,
	 * that yielded the given response.
	 * 
	 * @param response the response
	 * @param classLoader the classloader
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	void invalidateIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException;

	/**
	 * Reconstructs the consensus parameters from information in the manifest.
	 */
	void recomputeConsensus();

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If the node has some form of commit, then this method can only succeed
	 * when the transaction has been definitely committed in the node.
	 * Nodes are allowed to keep in store all, some or none of the requests
	 * that they received during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request, if any
	 */
	Optional<TransactionRequest<?>> getRequest(TransactionReference reference);

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method can only succeed
	 * when the transaction has been definitely committed in the node.
	 * Nodes are allowed to keep in store all, some or none of the responses
	 * that they computed during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponse(TransactionReference reference);

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method succeeds
	 * also if the transaction has not been committed yet in the node.
	 * Nodes are allowed to keep in store all, some or none of the responses
	 * that they computed during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference);

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws UnsupportedVerificationVersionException if the verification version is not supported
	 * @throws IOException if there was an I/O error while accessing some jar
	 */
	EngineClassLoader getClassLoader(TransactionReference classpath) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, NoSuchElementException, UnknownReferenceException, NodeException;

	/**
	 * Checks that the given request is signed with the private key of its caller.
	 * It uses a cache to remember the last signatures already checked.
	 * 
	 * @param request the request
	 * @param signatureAlgorithm the algorithm that must have been used for signing the request
	 * @return true if and only if the signature of {@code request} is valid
	 * @throws Exception if the signature of the request could not be checked
	 */
	boolean signatureIsValid(SignedTransactionRequest<?> request, SignatureAlgorithm signatureAlgorithm) throws Exception;

	/**
	 * Yields the consensus parameters of the node.
	 * 
	 * @return the consensus parameters
	 */
	ConsensusConfig<?,?> getConsensusParams();

	/**
	 * Yields the reference to the gamete account of the node.
	 * This method uses a cache to avoid repeated computations.
	 * 
	 * @return the reference to the gamete account, if the node is already initialized
	 * @throws NodeException if the node is not able to complete the operation
	 */
	Optional<StorageReference> getGamete() throws NodeException;

	/**
	 * Yields the reference to the contract that collects the validators of the node.
	 * After each transaction that consumes gas, the price of the gas is sent to this
	 * contract, that can later redistribute the reward to all validators.
	 * This method uses a cache to avoid repeated computations.
	 * 
	 * @return the reference to the contract, if the node is already initialized
	 * @throws NodeException if the node is not able to complete the operation
	 */
	Optional<StorageReference> getValidators() throws NodeException;

	/**
	 * Yields the reference to the objects that keeps track of the
	 * versions of the modules of the node.
	 * 
	 * @return the reference to the object, if the node is already initialized
	 * @throws NodeException if the node is not able to complete the operation
	 */
	Optional<StorageReference> getVersions() throws NodeException;

	/**
	 * Yields the reference to the contract that keeps track of the gas cost.
	 * 
	 * @return the reference to the contract, if the node is already initialized
	 * @throws NodeException if the node is not able to complete the operation
	 */
	Optional<StorageReference> getGasStation() throws NodeException;

	/**
	 * Yields the current gas price of the node.
	 * 
	 * @return the current gas price of the node, if the node is already initialized
	 */
	Optional<BigInteger> getGasPrice();

	/**
	 * Yields the current inflation of the node.
	 * 
	 * @return the current inflation of the node, if the node is already initialized
	 */
	Optional<Long> getCurrentInflation();
}