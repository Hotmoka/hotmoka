package io.takamaka.code.engine;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.AbstractNodeProxyForEngine;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
public abstract class AbstractNode<C extends Config> extends AbstractNodeProxyForEngine implements Node {
	private final static Logger logger = LoggerFactory.getLogger(AbstractNode.class);

	/**
	 * The configuration of the node.
	 */
	public final C config;

	/**
	 * A map that provides a semaphore for each currently executing transaction.
	 * It is used to block threads waiting for the outcome of transactions.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores = new ConcurrentHashMap<>();

	/**
	 * An executor for short background tasks.
	 */
	private final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * The time spent for checking requests.
	 */
	private final AtomicLong checkTime = new AtomicLong();

	/**
	 * The time spent for delivering transactions.
	 */
	private final AtomicLong deliverTime = new AtomicLong();

	/**
	 * The hashing algorithm for transaction requests.
	 */
	private final HashingAlgorithm<? super TransactionRequest<?>> hashingForRequests;

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	/**
	 * The array of hexadecimal digits.
	 */
	private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes();

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractNode(C config) {
		try {
			this.config = config;
			this.hashingForRequests = hashingForRequests();
		}
		catch (Exception e) {
			logger.error("failed to create the node", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the default gas cost model. Subclasses may redefine
	 */
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}

	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> signatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		// we do not take into account the signature itself
		return SignatureAlgorithm.sha256dsa(NonInitialTransactionRequest::toByteArrayWithoutSignature);
	}

	@Override
	public final TransactionReference getTakamakaCode() throws NoSuchElementException {
		return getClassTag(getManifest()).jar;
	}

	@Override
	public void close() throws Exception {
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		logger.info("Time spent checking requests: " + checkTime + "ms");
		logger.info("Time spent delivering requests: " + deliverTime + "ms");
	}

	/**
	 * Expands the store of this node with a transaction.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	protected abstract void expandStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Expands the store of this node with a transaction that could not be delivered since an error occurred.
	 * 
	 * @param reference the reference of the request
	 * @param request the request
	 * @param errorMessage an description of why delivering failed
	 */
	protected abstract void expandStore(TransactionReference reference, TransactionRequest<?> request, String errorMessage);

	/**
	 * Post the given request to this node. It will be scheduled, eventually, checked and delivered.
	 * 
	 * @param request the request to post
	 */
	protected abstract void postTransaction(TransactionRequest<?> request);

	/**
	 * Yields the most recent update for the given non-{@code final} field,
	 * of lazy type, of the object with the given storage reference.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the update
	 */
	public abstract UpdateOfField getLastLazyUpdateToNonFinalField(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU);

	/**
	 * Yields the most recent update for the given {@code final} field,
	 * of lazy type, of the object with the given storage reference.
	 * Its implementation can be identical to
	 * that of {@link #getLastLazyUpdateToNonFinalField(StorageReference, FieldSignature, Consumer<BigInteger>)},
	 * or instead exploit the fact that the field is {@code final}, for an optimized look-up.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the update
	 */
	public abstract UpdateOfField getLastLazyUpdateToFinalField(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU);

	/**
	 * Posts the given request.
	 * 
	 * @param request the request
	 * @return the reference of the request
	 */
	protected TransactionReference postRequest(TransactionRequest<?> request) {
		TransactionReference reference = referenceOf(request);
		logger.info(reference + ": posting (" + request.getClass().getSimpleName() + ')');
		createSemaphore(reference);
		postTransaction(request);
	
		return reference;
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param task the task
	 * @return the return value computed by the task
	 */
	public final <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param task the task
	 * @return the return value computed by the task
	 */
	public final void submit(Runnable task) {
		executor.submit(task);
	}

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 */
	public final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		TransactionReference reference = referenceOf(request);

		try {
			logger.info(reference + ": checking start");
			request.check();
			logger.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			expandStore(reference, request, e.getMessage());
			logger.info(reference + ": checking failed", e);
			throw e;
		}
		catch (Exception e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			expandStore(reference, request, e.getMessage());
			logger.error(reference + ": checking failed with unexpected exception", e);
			throw InternalFailureException.of(e);
		}
		finally {
			checkTime.addAndGet(System.currentTimeMillis() - start);
		}
	}

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	public final void deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		TransactionReference reference = referenceOf(request);

		try {
			logger.info(reference + ": delivering start");
			TransactionResponse response = ResponseBuilder.of(reference, request, this).build();
			expandStore(reference, request, response);
			logger.info(reference + ": delivering success");
		}
		catch (TransactionRejectedException e) {
			expandStore(reference, request, e.getMessage());
			logger.info(reference + ": delivering failed", e);
			throw e;
		}
		catch (Exception e) {
			expandStore(reference, request, e.getMessage());
			logger.error(reference + ": delivering failed with unexpected exception", e);
			throw InternalFailureException.of(e);
		}
		finally {
			signalSemaphore(reference);
			deliverTime.addAndGet(System.currentTimeMillis() - start);
		}
	}

	/**
	 * Yields the reference to the translation that would be originated for the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	protected LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(bytesToHex(hashingForRequests.hash(request)));
	}

	/**
	 * Yields the hashing algorithm that must be used for hashing
	 * transaction requests into their hash.
	 * 
	 * @return the SHA256 hash of the request; subclasses may redefine
	 * @throws NoSuchAlgorithmException if the required hashing algorithm is not available in the Java installation
	 */
	protected HashingAlgorithm<? super TransactionRequest<?>> hashingForRequests() throws NoSuchAlgorithmException {
		return HashingAlgorithm.sha256(Marshallable::toByteArray);
	}

	/**
	 * Waits until a transaction has been committed, or until its delivering fails.
	 * If this method succeeds and this node has some form of commit, then the
	 * transaction has been definitely committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response computed for {@code request}
	 * @throws TransactionRejectedException if the request failed to be committed, because of this exception
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	protected TransactionResponse waitForResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		try {
			Semaphore semaphore = semaphores.get(reference);
			if (semaphore != null)
				semaphore.acquire();

			for (int attempt = 1, delay = config.pollingDelay; attempt <= Math.max(1, config.maxPollingAttempts); attempt++, delay = delay * 110 / 100)
				try {
					return pollResponseComputedFor(reference);
				}
				catch (NoSuchElementException e) {
					Thread.sleep(delay);
				}

			throw new TimeoutException("cannot find response for transaction reference " + reference + ": tried " + config.maxPollingAttempts + " times");
		}
		catch (TransactionRejectedException | TimeoutException | InterruptedException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	protected abstract TransactionResponse pollResponseComputedFor(TransactionReference reference) throws TransactionRejectedException;

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * Creates a semaphore for those who will wait for the result of the given request.
	 * 
	 * @param reference the reference of the transaction for the request
	 */
	private void createSemaphore(TransactionReference reference) {
		if (semaphores.putIfAbsent(reference, new Semaphore(0)) != null)
			throw new InternalFailureException("repeated request");
	}

	/**
	 * Wakes up who was waiting for the outcome of the given transaction.
	 * 
	 * @param reference the reference of the transaction
	 */
	private void signalSemaphore(TransactionReference reference) {
		Semaphore semaphore = semaphores.remove(reference);
		if (semaphore != null)
			semaphore.release();
	}
}