package io.hotmoka.network.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
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
import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.services.RestClientService;
import io.hotmoka.network.models.requests.*;
import io.hotmoka.network.models.responses.TransactionResponseModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import io.hotmoka.nodes.AbstractNodeWithSuppliers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * The implementation of a node that forwards all its calls to a remote service.
 */
public class RemoteNodeImpl extends AbstractNodeWithSuppliers implements RemoteNode {

	/**
	 * The configuration of the node.
	 */
	private final RemoteNodeConfig config;

	/**
	 * A parser to and from JSON.
	 */
	private final Gson gson;

	/**
	 * Builds the remote node.
	 * 
	 * @param config the configuration of the node
	 */
	public RemoteNodeImpl(RemoteNodeConfig config) {
		this.config = config;
		this.gson = new GsonBuilder()
				.registerTypeAdapter(BigInteger.class, (JsonDeserializer<BigInteger>) (jsonElement, type, jsonDeserializationContext) -> BigInteger.valueOf(jsonElement.getAsLong()))
				.create();
	}

	@Override
	public void close() {}

	@Override
	public TransactionReference getTakamakaCode() throws NoSuchElementException {
		return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.get(config.url + "/get/takamakaCode", TransactionReferenceModel.class).toBean());
	}

	@Override
	public StorageReference getManifest() throws NoSuchElementException {
		return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.get(config.url + "/get/manifest", StorageReferenceModel.class).toBean());
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
		return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.post(config.url + "/get/classTag", new StorageReferenceModel(reference), ClassTagModel.class).toBean(reference));
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
		return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.post(config.url + "/get/state", new StorageReferenceModel(reference), StateModel.class).toBean());
	}

	@SuppressWarnings("unchecked")
	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		String algo = wrapNetworkExceptionForNoSuchAlgorithmException(() -> RestClientService.get(config.url + "/get/signatureAlgorithmForRequests", String.class));

		// we check if the name of the algorithm is among those that we know
		try {
			Method method = SignatureAlgorithm.class.getMethod(algo, BytesSupplier.class);
			BytesSupplier<NonInitialTransactionRequest<?>> bytesSupplier = NonInitialTransactionRequest::toByteArrayWithoutSignature;
			return (SignatureAlgorithm<NonInitialTransactionRequest<?>>) method.invoke(null, bytesSupplier);
		}
		catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			throw new InternalFailureException("unknown remote signature algorithm named " + algo);
		}
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
		return wrapNetworkExceptionForNoSuchElementException(() -> TransactionRequestModel.toBeanFrom(gson, RestClientService.post(config.url + "/get/request", new TransactionReferenceModel(reference), TransactionRestRequestModel.class)));
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return wrapNetworkExceptionForResponseAtException(() -> TransactionResponseModel.toBeanFrom(gson, RestClientService.post(config.url + "/get/response", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		return wrapNetworkExceptionForPolledResponseException(() -> TransactionResponseModel.toBeanFrom(gson, RestClientService.post(config.url + "/get/polledResponse", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapNetworkExceptionSimple(() -> RestClientService.post(config.url + "/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapNetworkExceptionSimple(() -> RestClientService.post(config.url + "/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapNetworkExceptionSimple(() -> RestClientService.post(config.url + "/add/redGreenGameteCreationTransaction", new RedGreenGameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapNetworkExceptionSimple(() -> RestClientService.post(config.url + "/add/initializationTransaction", new InitializationTransactionRequestModel(request), Void.class));
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return wrapNetworkExceptionMedium(() -> RestClientService.post(config.url + "/add/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> RestClientService.post(config.url + "/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> RestClientService.post(config.url + "/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> RestClientService.post(config.url + "/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> RestClientService.post(config.url + "/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class).toBean());
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> RestClientService.post(config.url + "/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class).toBean());
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		TransactionReference reference = wrapNetworkExceptionSimple(() -> RestClientService.post(config.url + "/post/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
		return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		TransactionReference reference = wrapNetworkExceptionSimple(() -> RestClientService.post(config.url + "/post/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
		return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		TransactionReference reference = RestClientService.post(config.url + "/post/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
		return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		TransactionReference reference = RestClientService.post(config.url + "/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
		return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
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
	private static <T> T wrapNetworkExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
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
			logger.error("unexpected error", e);
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
	private static <T> T wrapNetworkExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
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
			logger.error("unexpected error", e);
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
	private static <T> T wrapNetworkExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			logger.error("unexpected error", e);
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
	private static <T> T wrapNetworkExceptionForNoSuchElementException(Callable<T> what) throws NoSuchElementException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(NoSuchElementException.class.getName()))
				throw new NoSuchElementException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			logger.error("unexpected error", e);
			throw new InternalFailureException(e.getMessage());
		}
	}

	/**
	 * Runs a callable and wraps the exception by its type.
	 * If the type doesn't match {@link java.security.NoSuchAlgorithmException}
	 * then it will be wrapped into a {@link io.hotmoka.beans.InternalFailureException}.
	 *
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws NoSuchAlgorithmException the wrapped exception
	 */
	private static <T> T wrapNetworkExceptionForNoSuchAlgorithmException(Callable<T> what) throws NoSuchAlgorithmException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(NoSuchAlgorithmException.class.getName()))
				throw new NoSuchAlgorithmException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			logger.error("unexpected error", e);
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
	private static <T> T wrapNetworkExceptionForPolledResponseException(Callable<T> what) throws TransactionRejectedException, TimeoutException, InterruptedException  {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());
			else if (exceptionResponse.getExceptionClassName().equals(TimeoutException.class.getName()))
				throw new TimeoutException(exceptionResponse.getMessage());
			else if (exceptionResponse.getExceptionClassName().equals(InterruptedException.class.getName()))
				throw new InterruptedException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			logger.error("unexpected error", e);
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
	private static <T> T wrapNetworkExceptionForResponseAtException(Callable<T> what) throws TransactionRejectedException, NoSuchElementException {
		try {
			return what.call();
		}
		catch (NetworkExceptionResponse exceptionResponse) {
			if (exceptionResponse.getExceptionClassName().equals(TransactionRejectedException.class.getName()))
				throw new TransactionRejectedException(exceptionResponse.getMessage());
			else if (exceptionResponse.getExceptionClassName().equals(NoSuchElementException.class.getName()))
				throw new NoSuchElementException(exceptionResponse.getMessage());
			else
				throw new InternalFailureException(exceptionResponse.getMessage());
		}
		catch (Exception e) {
			logger.error("unexpected error", e);
			throw new InternalFailureException(e.getMessage());
		}
	}
}