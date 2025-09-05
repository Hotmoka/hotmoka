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
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.GenericJarStoreTransactionRequest;
import io.hotmoka.node.api.requests.InitialTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.GenericJarStoreTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionResponseWithInstrumentedJar;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractStoreTransformation;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.api.ClassLoaderCreationException;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationException;
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
	private final ConcurrentMap<TransactionReference, GenericJarStoreTransactionResponse> reverified = new ConcurrentHashMap<>();

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
	 * They should be transactions that installed jars in the store of the node.
	 * 
	 * @param transactions the transactions
	 * @param environment the execution environment where the reverification is performed
	 * @param consensus the consensus parameters to use for reverification
	 * @throws ClassLoaderCreationException if some of the transactions do not exist or did not install a jar in store
	 */
	public Reverification(Stream<TransactionReference> transactions, ExecutionEnvironment environment, ConsensusConfig<?,?> consensus) throws ClassLoaderCreationException {
		this.environment = environment;
		this.consensus = consensus;

		var counter = new AtomicInteger();
		for (var dependency: transactions.toArray(TransactionReference[]::new))
			reverify(dependency, counter, false);
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
		// reverification might be called for a store or for a store transformation;
		// in the first case, it is just needed to guarantee that all jars needed
		// for the execution from the store are still verifiable; in the second,
		// it also modifies the store transformation so that, at its end, the
		// jars are modified with their new verified version
		if (environment instanceof AbstractStoreTransformation<?,?,?,?> ast) {
			for (var entry: reverified.entrySet()) {
				var reference = entry.getKey();

				try {
					environment.getRequest(reference);
					ast.replaceResponse(reference, entry.getValue());
				}
				catch (UnknownReferenceException e) {
					throw new LocalNodeException(e); // the response for this transaction has been reverified but it disappeared! The store looks corrupted
				}

				LOGGER.info(reference + ": updated after reverification");
			}
		}

		// we clean the set of reverified responses, to avoid repeated replacements in the future, if
		// the class loader is recycled for other transactions: this is just an optimization
		reverified.clear();
	}

	/**
	 * Reverifies the jars installed by the given transaction and by its dependencies,
	 * if they have a verification version different from that of the consensus of the node.
	 *
	 * @param transaction the transaction
	 * @param counter the progressive counter of how many transactions have been reverified so far with this object
	 * @param wasDependencyInStore true if and only if this has been called on a dependency of a transaction
	 *                             passed at the constructor of this object
	 * @return the responses of the requests that have tried to install classpath and all its dependencies;
	 *         this can either be made of successful responses only or it can contain a single failed response only
	 * @throws ClassLoaderCreationException if the transaction does not exist or did not install a jar in store
	 */
	private List<? extends GenericJarStoreTransactionResponse> reverify(TransactionReference transaction, AtomicInteger counter, boolean wasDependencyInStore) throws ClassLoaderCreationException {
		if (counter.incrementAndGet() > consensus.getMaxDependencies())
			throw new ClassLoaderCreationException("Too many dependencies in classpath: the maximum is " + consensus.getMaxDependencies());

		JarStoreTransactionResponseWithInstrumentedJar response = getResponse(transaction, wasDependencyInStore);
		var reverifiedDependencies = new ArrayList<JarStoreTransactionResponseWithInstrumentedJar>();

		for (var reverifiedDependency: reverifiedDependenciesOf(response, counter))
			if (reverifiedDependency instanceof JarStoreTransactionResponseWithInstrumentedJar jstrwij)
				reverifiedDependencies.add(jstrwij);
			else
				return List.of(transformIntoFailed(response, transaction, "the reverification of a dependency failed"));

		if (response.getVerificationVersion() == consensus.getVerificationVersion()) {
			reverifiedDependencies.add(response);
			return reverifiedDependencies;
		}

		// the dependencies have passed reverification successfully, but the transaction needs reverification
		GenericJarStoreTransactionResponse reverifiedResponse = newResponseAfterReverificationOfJar(transaction, response, reverifiedDependencies);

		if (reverifiedResponse instanceof JarStoreTransactionResponseWithInstrumentedJar jstrwij) {
			reverifiedDependencies.add(jstrwij);
			return reverifiedDependencies;
		}
		else
			return List.of(reverifiedResponse); // it's a failed response
	}
	
	private List<GenericJarStoreTransactionResponse> reverifiedDependenciesOf(JarStoreTransactionResponseWithInstrumentedJar responseInStore, AtomicInteger counter) throws ClassLoaderCreationException {
		var reverifiedDependencies = new ArrayList<GenericJarStoreTransactionResponse>();
		for (var dependencyInStore: responseInStore.getDependencies().toArray(TransactionReference[]::new))
			reverifiedDependencies.addAll(reverify(dependencyInStore, counter, true));
	
		return reverifiedDependencies;
	}

	/**
	 * Updates the response of the given transaction, after reverifying the jar that that transaction has installed.
	 * 
	 * @param transaction the reference to the transaction that installed a jar that must be reverified;
	 *                    it is guaranteed that this existed in store and was a transaction install transaction
	 * @param response the updated response; this might be a successful jar store install response or a failed one, if the
	 *                 reverification failed
	 * @param reverifiedDependencies the already reverified dependencies of the jar installed by {@code transaction}
	 * @return the jar installed by {@code transaction}, reverified with the current verification version of the node
	 * @throws ClassLoaderCreationException if the creation of the new response failed
	 */
	private GenericJarStoreTransactionResponse newResponseAfterReverificationOfJar
		(TransactionReference transaction, JarStoreTransactionResponseWithInstrumentedJar response, List<JarStoreTransactionResponseWithInstrumentedJar> reverifiedDependencies)
				throws ClassLoaderCreationException {

		// if the result was already computed, we avoid its recomputation
		GenericJarStoreTransactionResponse cachedResult = reverified.get(transaction);
		if (cachedResult != null)
			return cachedResult;

		TransactionRequest<?> request;

		try {
			request = environment.getRequest(transaction);
		}
		catch (UnknownReferenceException e) {
			throw new LocalNodeException("Transaction " + transaction + " under reverification has a response in store but its request cannot be found in store");
		}

		// this check should always succeed if the implementation of the node is correct, since we checked already that the response installed a jar
		if (request instanceof GenericJarStoreTransactionRequest<?> gjstr) {
			// we build the classpath for the classloader: it includes the jar...
			var jars = new ArrayList<byte[]>(1 + reverifiedDependencies.size());
			byte[] jar = gjstr.getJar();
			jars.add(jar);

			// ... and the instrumented jars of its dependencies
			for (var dependency: reverifiedDependencies)
				jars.add(dependency.getInstrumentedJar());

			if (jars.stream().mapToLong(bytes -> bytes.length).sum() > consensus.getMaxCumulativeSizeOfDependencies())
				// the transaction was already in store and the total size was fine at the time of its installation;
				// it is theoretically possible that the consensus changed since its installation
				throw new ClassLoaderCreationException("Too large cumulative size of dependencies in classpath: the maximum is " + consensus.getMaxCumulativeSizeOfDependencies() + " bytes");

			try {
				var tcl = TakamakaClassLoaders.of(jars.stream(), consensus.getVerificationVersion());
				VerifiedJars.of(jar, tcl, gjstr instanceof InitialTransactionRequest, _error -> {}, consensus.skipsVerification());
			}
			catch (UnsupportedVerificationVersionException e) {
				throw new LocalNodeException(e);
			}
			catch (VerificationException | IllegalJarException | UnknownTypeException e) {
				return transformIntoFailed(response, transaction, e.getMessage());
			}

			return updateVersion(response, transaction);
		}
		else
			throw new LocalNodeException("Transaction " + transaction + " under reverification has a response in store that installed a jar but its request is not for a jar installation");
	}

	private JarStoreTransactionFailedResponse transformIntoFailed(JarStoreTransactionResponseWithInstrumentedJar response, TransactionReference transaction, String error) {
		if (response instanceof JarStoreInitialTransactionResponse)
			throw new LocalNodeException("The reverification of the initial jar store transaction " + transaction + " failed: its jar cannot be used and the node is broken");
		else if (response instanceof JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial) {
			var replacement = TransactionResponses.jarStoreFailed(
					currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.getGasConsumedForCPU(),
					currentResponseAsNonInitial.getGasConsumedForRAM(), currentResponseAsNonInitial.getGasConsumedForStorage(),
					BigInteger.ZERO, VerificationException.class.getName(), error);

			reverified.put(transaction, replacement);

			return replacement;
		}
		else
			throw new RuntimeException("Unexpected jar-carrying response of class " + response.getClass().getName());
	}

	private JarStoreTransactionResponseWithInstrumentedJar updateVersion(JarStoreTransactionResponseWithInstrumentedJar response, TransactionReference transaction) {
		JarStoreTransactionResponseWithInstrumentedJar replacement;

		if (response instanceof JarStoreInitialTransactionResponse)
			replacement = TransactionResponses.jarStoreInitial(response.getInstrumentedJar(), response.getDependencies(), consensus.getVerificationVersion());
		else if (response instanceof JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial) 
			replacement = TransactionResponses.jarStoreSuccessful(
				response.getInstrumentedJar(), response.getDependencies(),
				consensus.getVerificationVersion(), currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.getGasConsumedForCPU(),
				currentResponseAsNonInitial.getGasConsumedForRAM(), currentResponseAsNonInitial.getGasConsumedForStorage());
		else
			throw new LocalNodeException("Unexpected jar-carrying response of class " + response.getClass().getName());

		reverified.put(transaction, replacement);

		return replacement;
	}

	/**
	 * Yields the response generated by the transaction with the given reference. The transaction must be
	 * for a request to install a jar in the store of the node and that installation must have succeeded.
	 * 
	 * @param transaction the reference of the transaction
	 * @return the response
	 * @throws ClassLoaderCreationException if the transaction does not exist in the store, or did not generate a response with instrumented jar
	 */
	private JarStoreTransactionResponseWithInstrumentedJar getResponse(TransactionReference transaction, boolean wasDependencyInStore) throws ClassLoaderCreationException {
		try {
			TransactionResponse response = environment.getResponse(transaction);

			if (response instanceof GenericJarStoreTransactionResponse gjstr) {
				if (gjstr instanceof JarStoreTransactionResponseWithInstrumentedJar trwij)
					return trwij;
				else if (wasDependencyInStore)
					throw new LocalNodeException("The transaction " + transaction + " under reverification was a dependency of a transaction in store but it did not install a jar in store");
				else
					throw new ClassLoaderCreationException("The transaction " + transaction + " under reverification did not install a jar in store");
			}
			else
				if (wasDependencyInStore)
					throw new LocalNodeException("Transaction " + transaction + " under reverification was a dependency of a transaction in store, hence it was expected to be a jar store transaction, but it is a " + response.getClass().getSimpleName());
				else
					throw new ClassLoaderCreationException("The transaction " + transaction + " under reverification is not a jar store transaction");
		}
		catch (UnknownReferenceException e) {
			if (wasDependencyInStore)
				throw new LocalNodeException("Transaction " + transaction + " under reverification was a dependency of a transaction in store, hence it was expected to exist, but it cannot be found in store");
			else
				throw new ClassLoaderCreationException("Unknown transaction reference " + transaction + " under reverification");
		}
	}
}