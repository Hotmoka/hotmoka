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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.GenericJarStoreTransactionRequest;
import io.hotmoka.node.api.requests.InitialTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.GenericJarStoreTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionResponseWithInstrumentedJar;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractStoreTransformation;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * A class used to perform a re-verification of jars already stored in the node.
 */
public class Reverification {
	private final static Logger LOGGER = Logger.getLogger(Reverification.class.getName());

	/**
	 * Responses that have been found to have a distinct verification version
	 * than that of the node and have been consequently reverified.
	 */
	private final ConcurrentMap<TransactionReference, TransactionResponse> reverified = new ConcurrentHashMap<>();

	/**
	 * The execution environment where the reverification is performed.
	 */
	private final ExecutionEnvironment environment;

	/**
	 * The consensus parameters to use for reverification.
	 */
	private final ConsensusConfig<?,?> consensus;
	
	/**
	 * Reverifies the responses of the given transactions and of their dependencies.
	 * They must be transactions that installed jars in the store of the node.
	 * 
	 * @param transactions the transactions
	 * @param environment the execution environment where the reverification is performed
	 * @param consensus the consensus parameters to use for reverification
	 * @throws StoreException if the operation cannot be completed correctly
	 * @throws TransactionRejectedException if the transactions does not exist or did not install a jar in store
	 */
	public Reverification(Stream<TransactionReference> transactions, ExecutionEnvironment environment, ConsensusConfig<?,?> consensus) throws StoreException, TransactionRejectedException {
		this.environment = environment;
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
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public void replace() throws StoreException {
		for (var entry: reverified.entrySet()) {
			var reference = entry.getKey();

			try {
				environment.getRequest(reference);

				if (environment instanceof AbstractStoreTransformation<?,?,?,?> ast) // TODO: can we remove this?
					ast.setResponse(reference, entry.getValue());
			}
			catch (UnknownReferenceException e) {
				throw new StoreException(e); // the response for this transaction has been reverified but it disappeared! The store looks corrupted
			}

			LOGGER.info(reference + ": updated after reverification");
		}

		// we clean the set of reverified responses, to avoid repeated replacements in the future, if
		// the class loader is recycled for other transactions
		reverified.clear();
	}

	/**
	 * Reverifies the jars installed by the given transaction and by its dependencies,
	 * if they have a verification version different from that of the consensus of the node.
	 *
	 * @param transaction the transaction
	 * @return the responses of the requests that have tried to install classpath and all its dependencies;
	 *         this can either be made of successful responses only or it can contain a single failed response only
	 * @throws StoreException if the store of the node is misbehaving
	 * @throws TransactionRejectedException if the transactions does not exist or did not install a jar in store
	 */
	private List<GenericJarStoreTransactionResponse> reverify(TransactionReference transaction, AtomicInteger counter) throws StoreException, TransactionRejectedException {
		if (consensus != null && counter.incrementAndGet() > consensus.getMaxDependencies())
			throw new StoreException("Too many dependencies in classpath: max is " + consensus.getMaxDependencies());

		JarStoreTransactionResponseWithInstrumentedJar response = getResponseWithInstrumentedJarAtUncommitted(transaction);
		List<GenericJarStoreTransactionResponse> reverifiedDependencies = reverifiedDependenciesOf(response, counter);

		if (anyFailed(reverifiedDependencies))
			return List.of(transformIntoFailed(response, transaction, "the reverification of a dependency failed"));

		if (!needsReverification(response))
			return union(reverifiedDependencies, response);

		// the dependencies have passed reverification successfully, but the transaction needs reverification
		VerifiedJar vj = recomputeVerifiedJarFor(transaction, reverifiedDependencies);
		var firstError = vj.getErrors().findFirst();
		if (firstError.isPresent())
			return List.of(transformIntoFailed(response, transaction, firstError.get().getMessage()));
		else
			return union(reverifiedDependencies, updateVersion(response, transaction));
	}
	
	private VerifiedJar recomputeVerifiedJarFor(TransactionReference transaction, List<GenericJarStoreTransactionResponse> reverifiedDependencies) throws StoreException, TransactionRejectedException {
		// we get the original jar that classpath had requested to install
		TransactionRequest<?> request;

		try {
			request = environment.getRequest(transaction);
		}
		catch (UnknownReferenceException e) {
			throw new TransactionRejectedException("Transaction " + transaction + " under reverification cannot be found in store");
		}

		// this check should always succeed if the implementation of the node is correct, since we checked already that the response installed a jar
		if (request instanceof GenericJarStoreTransactionRequest<?> gjstr) {
			// we build the classpath for the classloader: it includes the jar...
			byte[] jar = gjstr.getJar();
			var jars = new ArrayList<byte[]>(1 + reverifiedDependencies.size());
			jars.add(jar);

			// ... and the instrumented jars of its dependencies: since we have already considered the case
			// when a dependency is failed, we can conclude that they must all have an instrumented jar
			for (var dependency: reverifiedDependencies)
				jars.add(((JarStoreTransactionResponseWithInstrumentedJar) dependency).getInstrumentedJar());

			// consensus might be null if the node is restarting, during the recomputation of its consensus itself
			if (consensus != null && jars.stream().mapToLong(bytes -> bytes.length).sum() > consensus.getMaxCumulativeSizeOfDependencies())
				throw new StoreException("Too large cumulative size of dependencies in classpath: max is " + consensus.getMaxCumulativeSizeOfDependencies() + " bytes");

			try {
				var tcl = TakamakaClassLoaders.of(jars.stream(), consensus != null ? consensus.getVerificationVersion() : 0);
				return VerifiedJars.of(jar, tcl, gjstr instanceof InitialTransactionRequest, consensus != null && consensus.skipsVerification());
			}
			catch (IllegalJarException | UnsupportedVerificationVersionException e) {
				throw new StoreException(e);
			}
		}
		else
			throw new StoreException("The transaction that installed the jar under reverification did not really install a jar");
	}

	private boolean needsReverification(JarStoreTransactionResponseWithInstrumentedJar response) {
		return response.getVerificationVersion() != consensus.getVerificationVersion();
	}

	private List<GenericJarStoreTransactionResponse> reverifiedDependenciesOf(JarStoreTransactionResponseWithInstrumentedJar response, AtomicInteger counter) throws StoreException, TransactionRejectedException {
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

	private JarStoreTransactionFailedResponse transformIntoFailed(JarStoreTransactionResponseWithInstrumentedJar response, TransactionReference transaction, String error) throws StoreException {
		if (response instanceof JarStoreInitialTransactionResponse)
			throw new StoreException("The reverification of the initial jar store transaction " + transaction + " failed: its jar cannot be used");
		else if (response instanceof JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial) {
			var replacement = TransactionResponses.jarStoreFailed(
					currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.getGasConsumedForCPU(),
					currentResponseAsNonInitial.getGasConsumedForRAM(), currentResponseAsNonInitial.getGasConsumedForStorage(),
					BigInteger.ZERO, VerificationException.class.getName(), error);

			reverified.put(transaction, replacement);

			return replacement;
		}
		else
			throw new StoreException("Unexpected jar-carrying reponse of class " + response.getClass().getName());
	}

	private GenericJarStoreTransactionResponse updateVersion(JarStoreTransactionResponseWithInstrumentedJar response, TransactionReference transaction) throws StoreException {
		GenericJarStoreTransactionResponse replacement;

		if (response instanceof JarStoreInitialTransactionResponse)
			replacement = TransactionResponses.jarStoreInitial(response.getInstrumentedJar(), response.getDependencies(), consensus.getVerificationVersion());
		else if (response instanceof JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial) 
			replacement = TransactionResponses.jarStoreSuccessful(
				response.getInstrumentedJar(), response.getDependencies(),
				consensus.getVerificationVersion(), currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.getGasConsumedForCPU(),
				currentResponseAsNonInitial.getGasConsumedForRAM(), currentResponseAsNonInitial.getGasConsumedForStorage());
		else
			throw new StoreException("Unexpected jar-carrying reponse of class " + response.getClass().getName());

		reverified.put(transaction, replacement);

		return replacement;
	}

	/**
	 * Yields the response generated by the transaction with the given reference.
	 * The transaction must be a transaction that installed a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws StoreException if the store is misbehaving
	 * @throws TransactionRejectedException if the transaction does not exist in the store, or did not generate a response with instrumented jar
	 */
	private JarStoreTransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference) throws StoreException, TransactionRejectedException {
		try {
			if (environment.getResponse(reference) instanceof JarStoreTransactionResponseWithInstrumentedJar trwij)
				return trwij;
			else
				throw new TransactionRejectedException("The transaction " + reference + " under reverification did not install a jar in store");
		}
		catch (UnknownReferenceException e) {
			throw new TransactionRejectedException("Unknown transaction reference " + reference + " under reverification");
		}
	}
}