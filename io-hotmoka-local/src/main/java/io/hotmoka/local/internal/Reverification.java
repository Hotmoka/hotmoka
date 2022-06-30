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

package io.hotmoka.local.internal;

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

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.verification.TakamakaClassLoader;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.VerifiedJar;

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
	private final ConsensusParams consensus;
	
	/**
	 * Reverifies the responses of the given transactions and of their dependencies.
	 * They must be transactions that installed jars in the store of the node.
	 * 
	 * @param transactions the transactions
	 * @param node the node
	 * @param consensus the consensus parameters to use for reverification
	 */
	public Reverification(Stream<TransactionReference> transactions, NodeInternal node, ConsensusParams consensus) {
		this.node = node;
		this.consensus = consensus;

		AtomicInteger counter = new AtomicInteger();
		transactions.forEachOrdered(dependency -> reverify(dependency, counter));
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
	 */
	public void replace() {
		reverified.forEach((reference, response) -> {
			node.getStore().replace(reference, node.getRequest(reference), response);
			logger.info(reference + ": updated after reverification");
		});

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
	 */
	private List<JarStoreTransactionResponse> reverify(TransactionReference transaction, AtomicInteger counter) {
		if (consensus != null && counter.incrementAndGet() > consensus.maxDependencies)
			throw new IllegalArgumentException("too many dependencies in classpath: max is " + consensus.maxDependencies);

		TransactionResponseWithInstrumentedJar response = getResponseWithInstrumentedJarAtUncommitted(transaction);
		List<JarStoreTransactionResponse> reverifiedDependencies = reverifiedDependenciesOf(response, counter);

		if (anyFailed(reverifiedDependencies))
			return List.of(transformIntoFailed(response, transaction, "the reverification of a dependency failed"));

		if (!needsReverification(response))
			return union(reverifiedDependencies, (JarStoreTransactionResponse) response); // by type hierarchy, this cast will always succeed

		// the dependencies have passed reverification successfully, but the transaction needs reverification
		VerifiedJar vj = recomputeVerifiedJarFor(transaction, reverifiedDependencies);
		if (vj.hasErrors())
			return List.of(transformIntoFailed(response, transaction, vj.getFirstError().get().message));
		else
			return union(reverifiedDependencies, updateVersion(response, transaction));
	}
	
	private VerifiedJar recomputeVerifiedJarFor(TransactionReference transaction, List<JarStoreTransactionResponse> reverifiedDependencies) {
		// we get the original jar that classpath had requested to install; this cast will always
		// succeed if the implementation of the node is correct, since we checked already that the response installed a jar
		AbstractJarStoreTransactionRequest jarStoreRequestOfTransaction = (AbstractJarStoreTransactionRequest) node.getRequest(transaction);

		// we build the classpath for the classloader: it includes the jar...
		byte[] jar = jarStoreRequestOfTransaction.getJar();
		List<byte[]> jars = new ArrayList<>();
		jars.add(jar);

		// ... and the instrumented jars of its dependencies: since we have already considered the case
		// when a dependency is failed, we can conclude that they must all have an instrumented jar
		reverifiedDependencies.stream()
			.map(dependency -> (TransactionResponseWithInstrumentedJar) dependency)
			.map(TransactionResponseWithInstrumentedJar::getInstrumentedJar)
			.forEachOrdered(jars::add);

		// consensus might be null if the node is restarting, during the recomputation of its consensus itself
		if (consensus != null && jars.stream().mapToLong(bytes -> bytes.length).sum() > consensus.maxCumulativeSizeOfDependencies)
			throw new IllegalArgumentException("too large cumulative size of dependencies in classpath: max is " + consensus.maxCumulativeSizeOfDependencies + " bytes");

		TakamakaClassLoader tcl = TakamakaClassLoader.of(jars.stream(), consensus != null ? consensus.verificationVersion : 0);

		try {
			return VerifiedJar.of(jar, tcl, jarStoreRequestOfTransaction instanceof InitialTransactionRequest,
				consensus != null && consensus.allowsSelfCharged, consensus != null && consensus.skipsVerification);
		}
		catch (IOException e) {
			throw InternalFailureException.of(e);
		}
	}

	private boolean needsReverification(TransactionResponseWithInstrumentedJar response) {
		return response.getVerificationVersion() != consensus.verificationVersion;
	}

	private List<JarStoreTransactionResponse> reverifiedDependenciesOf(TransactionResponseWithInstrumentedJar response, AtomicInteger counter) {
		List<JarStoreTransactionResponse> reverifiedDependencies = new ArrayList<>();
		response.getDependencies().map(dependency -> reverify(dependency, counter)).forEachOrdered(reverifiedDependencies::addAll);
		return reverifiedDependencies;
	}

	private boolean anyFailed(List<JarStoreTransactionResponse> responses) {
		return responses.stream().anyMatch(response -> response instanceof JarStoreTransactionFailedResponse);
	}

	private List<JarStoreTransactionResponse> union(List<JarStoreTransactionResponse> responses, JarStoreTransactionResponse added) {
		responses.add(added);
		return responses;
	}

	private JarStoreTransactionFailedResponse transformIntoFailed(TransactionResponseWithInstrumentedJar response, TransactionReference transaction, String error) {
		if (response instanceof JarStoreInitialTransactionResponse)
			throw new InternalFailureException("the reverification of the initial jar store transaction " + transaction + " failed: its jar cannot be used");

		// there remains only this possibility:
		JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial = (JarStoreTransactionSuccessfulResponse) response;

		JarStoreTransactionFailedResponse replacement = new JarStoreTransactionFailedResponse(
			VerificationException.class.getName(), error,
			currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.gasConsumedForCPU,
			currentResponseAsNonInitial.gasConsumedForRAM, currentResponseAsNonInitial.gasConsumedForStorage,
			BigInteger.ZERO);

		reverified.put(transaction, replacement);

		return replacement;
	}

	private JarStoreTransactionResponse updateVersion(TransactionResponseWithInstrumentedJar response, TransactionReference transaction) {
		JarStoreTransactionResponse replacement;

		if (response instanceof JarStoreInitialTransactionResponse)
			replacement = new JarStoreInitialTransactionResponse(response.getInstrumentedJar(), response.getDependencies(), consensus.verificationVersion);
		else {
			// there remains only this possibility
			JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial = (JarStoreTransactionSuccessfulResponse) response;

			replacement = new JarStoreTransactionSuccessfulResponse(
				response.getInstrumentedJar(), response.getDependencies(),
				consensus.verificationVersion, currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.gasConsumedForCPU,
				currentResponseAsNonInitial.gasConsumedForRAM, currentResponseAsNonInitial.gasConsumedForStorage);
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
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + reference));
		
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new NoSuchElementException("the transaction " + reference + " did not install a jar in store");
	
		return (TransactionResponseWithInstrumentedJar) response;
	}
}