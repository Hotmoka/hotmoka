package io.takamaka.code.engine.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import io.takamaka.code.engine.AbstractLocalNode;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedJar;

/**
 * A class used to perform a re-verification of jars already stored in the blockchain
 */
public class Reverification {

	/**
	 * Responses that should be pushed into the store of the node, since they have been found to have
	 * a distinct verification version than that of the node and have been consequently reverified.
	 */
	private final Map<TransactionReference, JarStoreTransactionResponse> reverified;
	
	private final TransactionReference transaction;
	private final AbstractLocalNode<?,?> node;
	private final int verificationVersion;
	
	/**
	 * 
	 * @param transaction the transaction that has installed the jar
	 * @param node the node
	 * @param verificationVersion the version of the verification module of the node
	 */
	public Reverification(TransactionReference transaction, AbstractLocalNode<?,?> node, int verificationVersion) {
		reverified = new HashMap<>();
		this.transaction = transaction;
		this.node = node;
		this.verificationVersion = verificationVersion;
	}
	
	/**
	 * Reverifies the jars installed by the given transaction and by its dependencies,
	 * if they have a verification version different from that of the node.
	 *
	 * @return the responses of the requests that have tried to install classpath and all its dependencies;
	 *         this can either be made of successful responses only or it can contain a single failed response only
	 */
	public List<JarStoreTransactionResponse> reverify() {
		TransactionResponseWithInstrumentedJar response = getResponseWithInstrumentedJarAtUncommitted(transaction);
		List<JarStoreTransactionResponse> reverifiedDependencies = reverifiedDependenciesOf(response, node, verificationVersion);

		if (anyFailed(reverifiedDependencies))
			return List.of(transformIntoFailed(response, transaction, "the reverification of a dependency failed"));

		if (!needsReverification(response, verificationVersion))
			return union(reverifiedDependencies, (JarStoreTransactionResponse) response); // by type hierarchy, this cast will always hold

		// the dependencies have passed reverification successully, but the transaction needs reverification
		VerifiedJar vj = recomputeVerifiedJarFor(transaction, reverifiedDependencies, node);
		if (vj.hasErrors())
			return List.of(transformIntoFailed(response, transaction, vj.getFirstError().get().message));

		return union(reverifiedDependencies, updateVersion(response, transaction, verificationVersion));
	}
	
	private VerifiedJar recomputeVerifiedJarFor(TransactionReference transaction, List<JarStoreTransactionResponse> reverifiedDependencies, AbstractLocalNode<?, ?> node) {
		// we collect the instrumented jars of its dependencies: since we have already considered the case
		// when a dependency is failed, we can conclude that they must all have an instrumented jar
		Stream<byte[]> instrumentedJarsOfDependencies = reverifiedDependencies.stream()
			.map(dependency -> (TransactionResponseWithInstrumentedJar) dependency)
			.map(TransactionResponseWithInstrumentedJar::getInstrumentedJar);

		// we get the original jar that classpath had requested to install; this cast will always
		// succeed if the implementation of the node is correct, since we checked already that the response installed a jar
		AbstractJarStoreTransactionRequest jarStoreRequestOfTransaction = (AbstractJarStoreTransactionRequest) node.getRequest(transaction);
		TakamakaClassLoader tcl = TakamakaClassLoader.of(instrumentedJarsOfDependencies, (name, pos) -> {});

		try {
			return VerifiedJar.of(jarStoreRequestOfTransaction.getJar(), tcl, jarStoreRequestOfTransaction instanceof InitialTransactionRequest, node.config.allowSelfCharged);
		}
		catch (IOException e) {
			throw InternalFailureException.of(e);
		}
	}

	private boolean needsReverification(TransactionResponseWithInstrumentedJar response, int verificationVersion) {
		return response.getVerificationVersion() != verificationVersion;
	}

	private List<JarStoreTransactionResponse> reverifiedDependenciesOf(TransactionResponseWithInstrumentedJar response, AbstractLocalNode<?,?> node, int verificationVersion) {
		List<JarStoreTransactionResponse> reverifiedDependencies = new ArrayList<>();
		response.getDependencies().map(dependency -> reverify()).forEachOrdered(reverifiedDependencies::addAll);
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

	private JarStoreTransactionResponse updateVersion(TransactionResponseWithInstrumentedJar response, TransactionReference transaction, int verificationVersion) {
		JarStoreTransactionResponse replacement;

		if (response instanceof JarStoreInitialTransactionResponse)
			replacement = new JarStoreInitialTransactionResponse(response.getInstrumentedJar(), response.getDependencies(), verificationVersion);
		else {
			// there remains only this possibility
			JarStoreTransactionSuccessfulResponse currentResponseAsNonInitial = (JarStoreTransactionSuccessfulResponse) response;

			replacement = new JarStoreTransactionSuccessfulResponse(
				response.getInstrumentedJar(), response.getDependencies(),
				verificationVersion, currentResponseAsNonInitial.getUpdates(), currentResponseAsNonInitial.gasConsumedForCPU,
				currentResponseAsNonInitial.gasConsumedForRAM, currentResponseAsNonInitial.gasConsumedForStorage);
		}

		reverified.put(transaction, replacement);

		return replacement;
	}

	/**
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @param node the node for which the class loader is created
	 * @return the response
	 * @throws NoSuchElementException if the transaction does not exist in the store, or
	 *                                did not generate a response with instrumented jar
	 */
	private TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference) throws NoSuchElementException {
		TransactionResponse response = (TransactionResponse) reverified.get(reference);
		if (response == null)
			response = node.getStore().getResponseUncommitted(reference)
				.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + reference));
		
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new NoSuchElementException("the transaction " + reference + " did not install a jar in store");
	
		return (TransactionResponseWithInstrumentedJar) response;
	}

	public Map<TransactionReference, JarStoreTransactionResponse> getReverified() {
		return reverified;
	}
	
	
}
