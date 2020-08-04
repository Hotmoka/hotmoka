package io.hotmoka.network.internal;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.*;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.services.RestClientService;
import io.hotmoka.network.models.requests.*;
import io.hotmoka.network.models.responses.TransactionResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * The implementation of a node that forwards all its calls to a remote service.
 */
public class RemoteNodeImpl implements RemoteNode {

	/**
	 * The configuration of the node.
	 */
	private final RemoteNodeConfig config;

	public RemoteNodeImpl(RemoteNodeConfig config) {
		this.config = config;
	}

	@Override
	public void close() {}

	@Override
	public TransactionReference getTakamakaCode() throws NoSuchElementException {
		return wrapInCaseOfNoSuchElementException(() -> RestClientService.get(config.url + "/get/takamakaCode", TransactionReferenceModel.class).toBean());
	}

	@Override
	public StorageReference getManifest() throws NoSuchElementException {
		return wrapInCaseOfNoSuchElementException(() -> RestClientService.get(config.url + "/get/manifest", StorageReferenceModel.class).toBean());
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
		return wrapInCaseOfNoSuchElementException(() -> RestClientService.post(config.url + "/get/classTag", new StorageReferenceModel(reference), ClassTagModel.class).toBean(reference));
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
		return wrapInCaseOfNoSuchElementException(() -> RestClientService.post(config.url + "/get/state", new StorageReferenceModel(reference), StateModel.class).toBean(reference));
	}

	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		// TODO
		return null;
	}

	@Override
	public TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException {
		return wrapInCaseOfNoSuchElementException(() -> TransactionRequestModel.toBeanFrom(RestClientService.post(config.url + "/get/requestAt", new TransactionReferenceModel(reference), TransactionRequestModel.class)));
	}

	@Override
	public TransactionResponse getResponseAt(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return wrapInCaseOfResponseAtException(() -> TransactionResponseModel.toBeanFrom(RestClientService.post(config.url + "/get/responseAt", new TransactionReferenceModel(reference), TransactionResponseModel.class)));
	}

	@Override
	public TransactionResponse getPolledResponseAt(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		return wrapInCaseOfPolledResponseException(() -> TransactionResponseModel.toBeanFrom(RestClientService.post(config.url + "/get/polledResponseAt", new TransactionReferenceModel(reference), TransactionResponseModel.class)));
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> RestClientService.post(config.url + "/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> RestClientService.post(config.url + "/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> RestClientService.post(config.url + "/add/redGreenGameteCreationTransaction", new RedGreenGameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapInCaseOfExceptionSimple(() -> RestClientService.post(config.url + "/add/initializationTransaction", new InitializationTransactionRequestModel(request), Void.class));
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return wrapInCaseOfExceptionMedium(() -> RestClientService.post(config.url + "/add/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> RestClientService.post(config.url + "/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> RestClientService.post(config.url + "/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> RestClientService.post(config.url + "/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> RestClientService.post(config.url + "/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class).toBean());
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> RestClientService.post(config.url + "/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class).toBean());
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> jarSupplierOf(RestClientService.post(config.url + "/post/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean()));
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> codeSupplierOf(RestClientService.post(config.url + "/post/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean()));
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> codeSupplierOf(RestClientService.post(config.url + "/post/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean()));
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> codeSupplierOf(RestClientService.post(config.url + "/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean()));
	}

	/**
	 * Yields a {@link io.hotmoka.nodes.Node.CodeSupplier} for the give transaction request reference.
	 * @param transactionReference the transaction reference
	 * @return the code supplier
	 */
	private static <T extends StorageValue> CodeSupplier<T> codeSupplierOf(TransactionReference transactionReference) {
		return new CodeSupplier<>() {
			@Override
			public TransactionReference getReferenceOfRequest() {
				return transactionReference;
			}

			@Override
			public T get() throws TransactionRejectedException, TransactionException, CodeExecutionException {
				return null; // TODO: not sure of this
			}
		};
	}

	/**
	 * Yields a {@link io.hotmoka.nodes.Node.JarSupplier} for the give transaction request reference.
	 * @param transactionReference the transaction reference
	 * @return the jar supplier
	 */
	private static JarSupplier jarSupplierOf(TransactionReference transactionReference) {
		return new JarSupplier() {
			@Override
			public TransactionReference getReferenceOfRequest() {
				return transactionReference;
			}

			@Override
			public TransactionReference get() throws TransactionRejectedException, TransactionException {
				return null; // TODO: not sure of this
			}
		};
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match any of the methods signature type then
	 * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TransactionException the wrapped exception
	 * @throws CodeExecutionException the wrapped exception
	 */
	private static <T> T wrapInCaseOfExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());
			else if (exceptionResponse.getExceptionClassName().equals(TransactionException.class.getName()))
				throw new TransactionException(exceptionResponse.getMessage());
			else if (exceptionResponse.getExceptionClassName().equals(CodeExecutionException.class.getName()))
				throw new CodeExecutionException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			throw new InternalFailureException(e.getMessage());
		}
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match any of the methods signature type then
	 * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapInCaseOfExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());
			else if (exceptionResponse.getExceptionClassName().equals(TransactionException.class.getName()))
				throw new TransactionException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			throw new InternalFailureException(e.getMessage());
		}
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match {@link io.hotmoka.beans.TransactionRejectedException} then it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 */
	private static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());

			throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			throw new InternalFailureException(e.getMessage());
		}
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match {@link java.util.NoSuchElementException} then it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws NoSuchElementException the wrapped exception
	 */
	private static <T> T wrapInCaseOfNoSuchElementException(Callable<T> what) throws NoSuchElementException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(NoSuchElementException.class.getName()))
				throw new NoSuchElementException(exceptionResponse.getMessage());

			throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			throw new InternalFailureException(e.getMessage());
		}
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match any of the methods signature type then
	 * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TimeoutException the wrapped exception
	 * @throws InterruptedException the wrapped exception
	 */
	private static <T> T wrapInCaseOfPolledResponseException(Callable<T> what) throws TransactionRejectedException, TimeoutException, InterruptedException  {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {

			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());

			if (exceptionResponse.getExceptionClassName().equals(TimeoutException.class.getName()))
				throw new TimeoutException(exceptionResponse.getMessage());

			if (exceptionResponse.getExceptionClassName().equals(InterruptedException.class.getName()))
				throw new InterruptedException(exceptionResponse.getMessage());

			throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			throw new InternalFailureException(e.getMessage());
		}
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match any of the methods signature type then
	 * it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws NoSuchElementException the wrapped exception
	 */
	private static <T> T wrapInCaseOfResponseAtException(Callable<T> what) throws TransactionRejectedException, NoSuchElementException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {

			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());

			if (exceptionResponse.getExceptionClassName().equals(NoSuchElementException.class.getName()))
				throw new NoSuchElementException(exceptionResponse.getMessage());

			throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			throw new InternalFailureException(e.getMessage());
		}
	}
}