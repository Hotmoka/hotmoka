package io.takamaka.code.engine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.takamaka.code.engine.internal.AbstractNodeProxyForEngine;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
public abstract class AbstractNode<C extends Config, S extends Store> extends AbstractNodeProxyForEngine<S> {
	protected final static Logger logger = LoggerFactory.getLogger(AbstractNode.class);

	/**
	 * The configuration of the node.
	 */
	public final C config;

	/**
	 * The store of the node.
	 */
	private final S store;

	/**
	 * A map that provides a semaphore for each currently executing transaction.
	 * It is used to block threads waiting for the outcome of transactions.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores = new ConcurrentHashMap<>();

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
	 * True if this blockchain has been already closed. Used to avoid double-closing in the shutdown hook.
	 */
	private final AtomicBoolean closed = new AtomicBoolean();

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

			if (config.delete) {
				deleteRecursively(config.dir);  // cleans the directory where the node's data live
				Files.createDirectories(config.dir);
			}

			this.store = mkStore();
			addShutdownHook();
		}
		catch (Exception e) {
			logger.error("failed to create the node", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Factory method for creating the store of this node.
	 * 
	 * @return the store
	 */
	protected abstract S mkStore();

	/**
	 * Determines if this node has not been closed yet.
	 * This thread-safe method can be called to avoid double-closing of a node.
	 * 
	 * @return true if and only if the node has not been closed yet
	 */
	protected final boolean isNotYetClosed() {
		return !closed.getAndSet(true);
	}

	@Override
	public final S getStore() {
		return store;
	}

	@Override
	public void close() throws Exception {
		super.close();
	
		logger.info("Time spent checking requests: " + checkTime + "ms");
		logger.info("Time spent delivering requests: " + deliverTime + "ms");
	}

	/**
	 * Yields the algorithm used to sign non-initial requests with this node.
	 * 
	 * @return the SHA256withDSA algorithm for signing non-initial requests (without their signature itself); subclasses may redefine
	 * @throws NoSuchAlgorithmException if the required signature algorithm is not available in the Java installation
	 */
	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		// we do not take into account the signature itself
		return SignatureAlgorithm.sha256dsa(NonInitialTransactionRequest::toByteArrayWithoutSignature);
	}

	@Override
	public final TransactionReference getTakamakaCode() throws NoSuchElementException {
		return getClassTag(getManifest()).jar;
	}

	@Override
	public final StorageReference getManifest() throws NoSuchElementException {
		return getStore().getManifest().orElseThrow(() -> new NoSuchElementException("no manifest set for this node"));
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return ((JarStoreInitialTransactionResponse) waitForResponse(reference)).getOutcomeAt(reference);
		});
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapInCaseOfExceptionSimple(() -> waitForResponse(postRequest(request))); // result unused
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) waitForResponse(postRequest(request))).getOutcome());
	}

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) waitForResponse(postRequest(request))).getOutcome());
	}

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return wrapInCaseOfExceptionMedium(() -> postJarStoreTransaction(request).get());
	}

	@Override
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postConstructorCallTransaction(request).get());
	}

	@Override
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postInstanceMethodCallTransaction(request).get());
	}

	@Override
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postStaticMethodCallTransaction(request).get());
	}

	@Override
	public final StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			TransactionReference reference = referenceOf(request);
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + ')');
			StorageValue result = ResponseBuilder.ofView(reference, request, this).build().getOutcome();
			logger.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			TransactionReference reference = referenceOf(request);
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + ')');
			StorageValue result = ResponseBuilder.ofView(reference, request, this).build().getOutcome();
			logger.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return jarSupplierFor(() -> ((JarStoreTransactionResponse) waitForResponse(reference)).getOutcomeAt(reference));
		});
	}

	@Override
	public final CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return codeSupplierFor(() -> ((ConstructorCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
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
			getStore().push(reference, request, trimmedMessage(e));
			logger.info(reference + ": checking failed", e);
			throw e;
		}
		catch (Exception e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			getStore().push(reference, request, trimmedMessage(e));
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
			getStore().push(reference, request, response);
			logger.info(reference + ": delivering success");
		}
		catch (TransactionRejectedException e) {
			getStore().push(reference, request, trimmedMessage(e));
			logger.info(reference + ": delivering failed", e);
			throw e;
		}
		catch (Exception e) {
			getStore().push(reference, request, trimmedMessage(e));
			logger.error(reference + ": delivering failed with unexpected exception", e);
			throw InternalFailureException.of(e);
		}
		finally {
			signalSemaphore(reference);
			deliverTime.addAndGet(System.currentTimeMillis() - start);
		}
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
	 * Post the given request to this node. It will be scheduled, eventually, checked and delivered.
	 * 
	 * @param request the request to post
	 */
	protected abstract void postTransaction(TransactionRequest<?> request);

	/**
	 * Tries to access the response computed for the given request reference.
	 * If it is not (yet) available, it throws an exception.
	 * 
	 * @param reference the reference of the request
	 * @return the response, if any
	 * @throws TransactionRejectedException if the request was rejected
	 * @throws NoSuchElementException if the response is not available yet
	 */
	protected abstract TransactionResponse pollResponseComputedFor(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException;

	/**
	 * Yields the reference to the translation that would be originated for the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	private LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(bytesToHex(hashingForRequests.hash(request)));
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
	private TransactionResponse waitForResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
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
	
			throw new TimeoutException("cannot find the response of the transaction reference " + reference + ": tried " + config.maxPollingAttempts + " times");
		}
		catch (TransactionRejectedException | TimeoutException | InterruptedException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Posts the given request.
	 * 
	 * @param request the request
	 * @return the reference of the request
	 */
	private TransactionReference postRequest(TransactionRequest<?> request) {
		TransactionReference reference = referenceOf(request);
		logger.info(reference + ": posting (" + request.getClass().getSimpleName() + ')');
		createSemaphore(reference);
		postTransaction(request);
	
		return reference;
	}

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

	/**
	 * Deletes the given directory, recursively.
	 * 
	 * @param dir the directory to delete
	 * @throws IOException if the directory or some of its subdirectories cannot be deleted
	 */
	private static void deleteRecursively(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	/**
	 * Adds a shutdown hook that shuts down the blockchain orderly if the JVM terminates.
	 */
	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				close();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	private static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null) {
				logger.error("transaction rejected", e.getCause());
				throw new TransactionRejectedException(e.getCause());
			}

			logger.error("transaction rejected", e);
			throw new TransactionRejectedException(e);
		}
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	private static <T> T wrapInCaseOfExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null) {
				logger.error("transaction rejected", e.getCause());
				throw new TransactionRejectedException(e.getCause());
			}

			logger.error("transaction rejected", e);
			throw new TransactionRejectedException(e);
		}
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	private static <T> T wrapInCaseOfExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | CodeExecutionException | TransactionException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null) {
				logger.error("transaction rejected", e.getCause());
				throw new TransactionRejectedException(e.getCause());
			}

			logger.error("transaction rejected", e);
			throw new TransactionRejectedException(e);
		}
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	/**
     * Yields the error message trimmed to a maximal length, to avoid overflow.
     *
     * @param t the throwable whose error message is processed
     * @return the resulting message
     */
	private String trimmedMessage(Throwable t) {
    	String message = t.getMessage();
		int length = message.length();
		if (length > config.maxErrorLength)
			return message.substring(0, config.maxErrorLength) + "...";
		else
			return message;
    }

    /**
	 * Yields an adaptor of a callable into a jar supplier.
	 * 
	 * @param task the callable
	 * @return the jar supplier
	 */
	private JarSupplier jarSupplierFor(Callable<TransactionReference> task) {
		return new JarSupplier() {
			private volatile TransactionReference cachedGet;

			@Override
			public TransactionReference get() throws TransactionRejectedException, TransactionException {
				return cachedGet != null ? cachedGet : (cachedGet = wrapInCaseOfExceptionMedium(task));
			}
		};
	}

	/**
	 * Yields an adaptor of a callable into a code supplier.
	 * 
	 * @param <W> the return value of the callable
	 * @param task the callable
	 * @return the code supplier
	 */
	private <W extends StorageValue> CodeSupplier<W> codeSupplierFor(Callable<W> task) {
		return new CodeSupplier<>() {
			private volatile W cachedGet;

			@Override
			public W get() throws TransactionRejectedException, TransactionException {
				return cachedGet != null ? cachedGet : (cachedGet = wrapInCaseOfExceptionMedium(task));
			}
		};
	}
}