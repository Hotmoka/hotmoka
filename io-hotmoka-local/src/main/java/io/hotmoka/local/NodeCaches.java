package io.hotmoka.local;

import java.math.BigInteger;
import java.util.Optional;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.ConsensusParams;

/**
 * The caches of a local node.
 */
public interface NodeCaches {

	/**
	 * Invalidates the information in this cache.
	 */
	void invalidate();

	/**
	 * Invalidates the information in this cache, after the execution of a transaction with the given classloader,
	 * that yielded the given response.
	 * 
	 * @param response the response
	 * @param classLoader the classloader
	 */
	void invalidateIfNeeded(TransactionResponse response, EngineClassLoader classLoader);

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
	 */
	EngineClassLoader getClassLoader(TransactionReference classpath);

	/**
	 * Checks that the given request is signed with the private key of its caller.
	 * It uses a cache to remember the last signatures already checked.
	 * 
	 * @param request the request
	 * @param signatureAlgorithm the algorithm that must have been used for signing the request
	 * @return true if and only if the signature of {@code request} is valid
	 * @throws Exception if the signature of the request could not be checked
	 */
	boolean signatureIsValid(SignedTransactionRequest request, SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm) throws Exception;

	/**
	 * Yields the consensus parameters of the node.
	 * 
	 * @return the consensus parameters
	 */
	ConsensusParams getConsensusParams();

	/**
	 * Yields the reference to the contract that collects the validators of the node.
	 * After each transaction that consumes gas, the price of the gas is sent to this
	 * contract, that can later redistribute the reward to all validators.
	 * This method uses a cache to avoid repeated computations.
	 * 
	 * @return the reference to the contract, if the node is already initialized
	 */
	Optional<StorageReference> getValidators();

	/**
	 * Yields the reference to the objects that keeps track of the
	 * versions of the modules of the node.
	 * 
	 * @return the reference to the object, if the node is already initialized
	 */
	Optional<StorageReference> getVersions();

	/**
	 * Yields the reference to the contract that keeps track of the gas cost.
	 * 
	 * @return the reference to the contract, if the node is already initialized
	 */
	Optional<StorageReference> getGasStation();

	/**
	 * Yields the current gas price of the node.
	 * 
	 * @return the current gas price of the node, if the node is already initialized
	 */
	Optional<BigInteger> getGasPrice();
}