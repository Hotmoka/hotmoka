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

package io.hotmoka.node.local.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.GenericJarStoreTransactionRequest;
import io.hotmoka.node.api.requests.InitialTransactionRequest;
import io.hotmoka.node.api.responses.GenericJarStoreTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.UnsupportedVerificationVersionException;
import io.hotmoka.stores.StoreException;
import io.hotmoka.stores.StoreTransaction;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.VerifiedJar;

/**
 * A class used to perform a re-verification of jars already stored in the node.
 */
public class Reverification {
	protected final static Logger logger = Logger.getLogger(Reverification.class.getName());

	/**
	 * Responses that have been found to have
	 * a distinct verification version than that of the node and have been consequently reverified.
	 */
	private final ConcurrentMap<TransactionReference, TransactionResponse> reverified = new ConcurrentHashMap<>();

	/**
	 * The node whose responses are reverified.
	 */
	private final NodeInternal node;

	/**
	 * The consensus parameters to use for reverification. This might be {@code null} if the node is restarting,
	 * during the recomputation of its same consensus.
	 */
	private final ConsensusConfig<?,?> consensus;
	
	/**
	 * Reverifies the responses of the given transactions and of their dependencies.
	 * They must be transactions that installed jars in the store of the node.
	 * 
	 * @param transactions the transactions
	 * @param node the node
	 * @param consensus the consensus parameters to use for reverification
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be found
	 * @throws UnsupportedVerificationVersionException if the verification version is not available
	 * @throws IOException if there was an I/O error while accessing some jar
	 * @throws NodeException 
	 * @throws NoSuchElementException 
	 */
	public Reverification(Stream<TransactionReference> transactions, NodeInternal node, ConsensusConfig<?,?> consensus) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, NoSuchElementException, UnknownReferenceException, NodeException {
		this.node = node;
		this.consensus = consensus;

		var counter = new AtomicInteger();
		for (var dependency: transactions.toArray(TransactionReference[]::new))
			reverify(dependency, counter);
	}

	/**
	 * Yields the reverified response for the given transaction, if it has been reverified.
	 * 
	 * @param transaction the transaction
	 * @return the reverified response, if any
	 */
	public Optional<TransactionResponse> getReverifiedResponse(TransactionReference transaction) {
		return Optional.ofNullable(reverified.get(transaction));
	}

	/**
	 * Replaces all reverified responses into the store of the node whose jars have been reverified.
	 * 
	 * @throws NodeException if this node is not able to complete the operation correctly
	 */
	public void replace(StoreTransaction<?> transaction) throws NodeException {
		for (var entry: reverified.entrySet()) {
			var reference = entry.getKey();

			try {
				transaction.replace(reference, node.getRequest(reference), entry.getValue());
			}
			catch (StoreException | UnknownReferenceException e) {
				throw new NodeException(e);
			}

			logger.info(reference + ": updated after reverification");
		}

		// we clean the set of reverified responses, to avoid repeated pushing in the future, if
		// the class loader is recycled for other transactions
		reverified.clear();
	}

	/**
	 * Reverifies the jars installed by the given transaction and by its dependencies,
	 * if they have a verification version different from that of the node.
	 *
	 * @param transaction the transaction
	 * @return the responses of the requests that have tried to install classpath and all its dependencies;
	 *         this can either be made of successful responses only or it can contain a single failed response only
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws UnsupportedVerificationVersionException if the verification version is not available
	 * @throws IOException if there was an I/O error while accessing some jar
	 * @throws NodeException 
	 * @throws NoSuchElementException 
	 */
	private List<GenericJarStoreTransactionResponse> reverify(TransactionReference transaction, AtomicInteger counter) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, NoSuchElementException, UnknownReferenceException, NodeException {
		if (consensus != null && counter.incrementAndGet() > consensus.getMaxDependencies())
			throw new IllegalArgumentException("too many dependencies in classpath: max is " + consensus.getMaxDependencies());

		TransactionResponseWithInstrumentedJar response = getResponseWithInstrumentedJarAtUncommitted(transaction);
		List<GenericJarStoreTransactionResponse> reverifiedDependencies = reverifiedDependenciesOf(response, counter);

		if (anyFailed(reverifiedDependencies))
			return List.of(transformIntoFailed(response, transaction, "the reverification of a dependency failed"));

		if (!needsReverification(response))
			return union(reverifiedDependencies, (GenericJarStoreTransactionResponse) response); // by type hierarchy, this cast will always succeed

		// the dependencies have passed reverification successfully, but the transaction needs reverification
		VerifiedJar vj = recomputeVerifiedJarFor(transaction, reverifiedDependencies);
		if (vj.hasErrors())
			return List.of(transformIntoFailed(response, transaction, vj.getFirstError().get().getMessage()));
		else
			return union(reverifiedDependencies, updateVersion(response, transaction));
	}
	
	private VerifiedJar recomputeVerifiedJarFor(TransactionReference transaction, List<GenericJarStoreTransactionResponse> reverifiedDependencies) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, UnknownReferenceException, NodeException {
		// we get the original jar that classpath had requested to install; this cast will always
		// succeed if the implementation of the node is correct, since we checked already that the response installed a jar
		var jarStoreRequestOfTransaction = (GenericJarStoreTransactionRequest<?>) node.getRequest(transaction);

		// we build the classpath for the classloader: it includes the jar...
		byte[] jar = jarStoreRequestOfTransaction.getJar();
		var jars = new ArrayList<byte[]>();
		jars.add(jar);

		// ... and the instrumented jars of its dependencies: since we have already considered the case
		// when a dependency is failed, we can conclude that they must all have an instrumented jar
		reverifiedDependencies.stream()
			.map(dependency -> (TransactionResponseWithInstrumentedJar) dependency)
			.map(TransactionResponseWithInstrumentedJar::getInstrumentedJar)
			.forEachOrdered(jars::add);

		// consensus might be null if the node is restarting, during the recomputation of its consensus itself
		if (consensus != null && jars.stream().mapToLong(bytes -> bytes.length).sum() > consensus.getMaxCumulativeSizeOfDependencies())
			throw new IllegalArgumentException("Too large cumulative size of dependencies in classpath: max is " + consensus.getMaxCumulativeSizeOfDependencies() + " bytes");

		var tcl = TakamakaClassLoaders.of(jars.stream(), consensus != null ? consensus.getVerificationVersion() : 0);

		try {
			return VerifiedJars.of(jar, tcl, jarStoreRequestOfTransaction instanceof InitialTransactionRequest,
				consensus != null && consensus.skipsVerification());
		}
		catch (io.hotmoka.verification.UnsupportedVerificationVersionException e) {
			throw new UnsupportedVerificationVersionException(e.verificationVerification);
		}
	}

	private boolean needsReverification(TransactionResponseWithInstrumentedJar response) {
		return response.getVerificationVersion() != consensus.getVerificationVersion();
	}

	private List<GenericJarStoreTransactionResponse> reverifiedDependenciesOf(TransactionResponseWithInstrumentedJar response, AtomicInteger counter) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, NoSuchElementException, UnknownReferenceException, NodeException {
		var reverifiedDependencies = new ArrayList<GenericJarStoreTransactionResponse>();
		for (var dependency: response.getDependencies().toArray(TransactionReference[]::new))
			reverifiedDependencies.addAll(reverify(dependency, counter));

		return reverifiedDependencies;
	}

	private boolean anyFailed(List<GenericJarStoreTransactionResponse> responses) {
		return responses.stream().anyMatch(response -> response instanceof JarStoreTransactionFailedResponse);
	}

	private List<GenericJarStoreTransactionResponse> union(List<GenericJarStoreTransactionResponse> responses, GenericJarStoreTransactionResponse added) {
		responses.add(added);
		return responses;
	}

	private JarStoreTransactionFailedResponse transformIntoFailed(TransactionResponseWithInstrumentedJar response, TransactionReference transaction, String error) {
		if (response instanceof JarStoreInitialTransactionResponse)
			throw new RuntimeException("The reverification of the initial jar store transaction " + transaction + " failed: its jar cannot be used");

		// there remains only this possibility:
		var currentResponseAsNonInitial = (JarStoreTransactionSuccessfulResponse) response;

		var replacement = TransactionResponses.jarStoreFailed(
			VerificationException.class.getName(), error,
			currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.getGasConsumedForCPU(),
			currentResponseAsNonInitial.getGasConsumedForRAM(), currentResponseAsNonInitial.getGasConsumedForStorage(),
			BigInteger.ZERO);

		reverified.put(transaction, replacement);

		return replacement;
	}

	private GenericJarStoreTransactionResponse updateVersion(TransactionResponseWithInstrumentedJar response, TransactionReference transaction) {
		GenericJarStoreTransactionResponse replacement;

		if (response instanceof JarStoreInitialTransactionResponse)
			replacement = TransactionResponses.jarStoreInitial(response.getInstrumentedJar(), response.getDependencies(), consensus.getVerificationVersion());
		else {
			// there remains only this possibility
			var currentResponseAsNonInitial = (JarStoreTransactionSuccessfulResponse) response;

			replacement = TransactionResponses.jarStoreSuccessful(
				response.getInstrumentedJar(), response.getDependencies(),
				consensus.getVerificationVersion(), currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.getGasConsumedForCPU(),
				currentResponseAsNonInitial.getGasConsumedForRAM(), currentResponseAsNonInitial.getGasConsumedForStorage());
		}

		reverified.put(transaction, (TransactionResponse) replacement);

		return replacement;
	}

	/**
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws NoSuchElementException if the transaction does not exist in the store, or did not generate a response with instrumented jar
	 */
	private TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference) throws NoSuchElementException {
		TransactionResponse response = node.getCaches().getResponseUncommitted(reference)
			.orElseThrow(() -> new RuntimeException("Unknown transaction reference " + reference));
		
		if (response instanceof TransactionResponseWithInstrumentedJar trwij)
			return trwij;
		else
			throw new NoSuchElementException("The transaction " + reference + " did not install a jar in store");
	}
}