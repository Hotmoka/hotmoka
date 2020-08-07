package io.hotmoka.network.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.google.gson.Gson;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.requests.TransferTransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.VoidMethodSignature;
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
import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.RedGreenGameteCreationTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.TransactionRestRequestModel;
import io.hotmoka.network.models.requests.TransferTransactionRequestModel;
import io.hotmoka.network.models.responses.ConstructorCallTransactionExceptionResponseModel;
import io.hotmoka.network.models.responses.ConstructorCallTransactionFailedResponseModel;
import io.hotmoka.network.models.responses.ConstructorCallTransactionSuccessfulResponseModel;
import io.hotmoka.network.models.responses.GameteCreationTransactionResponseModel;
import io.hotmoka.network.models.responses.InitializationTransactionResponseModel;
import io.hotmoka.network.models.responses.JarStoreInitialTransactionResponseModel;
import io.hotmoka.network.models.responses.JarStoreTransactionFailedResponseModel;
import io.hotmoka.network.models.responses.JarStoreTransactionSuccessfulResponseModel;
import io.hotmoka.network.models.responses.MethodCallTransactionExceptionResponseModel;
import io.hotmoka.network.models.responses.MethodCallTransactionFailedResponseModel;
import io.hotmoka.network.models.responses.MethodCallTransactionSuccessfulResponseModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.responses.VoidMethodCallTransactionSuccessfulResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import io.hotmoka.nodes.AbstractNodeWithSuppliers;

/**
 * The implementation of a node that forwards all its calls to a remote service.
 */
public class RemoteNodeImpl extends AbstractNodeWithSuppliers implements RemoteNode {

	/**
	 * The configuration of the node.
	 */
	private final RemoteNodeConfig config;


	/**
	 * Builds the remote node.
	 * 
	 * @param config the configuration of the node
	 */
	public RemoteNodeImpl(RemoteNodeConfig config) {
		this.config = config;
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
		return wrapNetworkExceptionForNoSuchElementException(() -> requestFromModel(RestClientService.post(config.url + "/get/request", new TransactionReferenceModel(reference), TransactionRestRequestModel.class)));
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return wrapNetworkExceptionForResponseAtException(() -> responseFromModel(RestClientService.post(config.url + "/get/response", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		return wrapNetworkExceptionForPolledResponseException(() -> responseFromModel(RestClientService.post(config.url + "/get/polledResponse", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
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
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/add/instanceMethodCallTransaction", modelFor(request), StorageValueModel.class)));
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/run/instanceMethodCallTransaction", modelFor(request), StorageValueModel.class)));
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
	}

	/**
	 * Deals with methods that return void: the API of the node
	 * requires to return null, always, when such methods are called.
	 * 
	 * @param request the request that calls the method
	 * @param model the model of the return value of the method
	 * @return the resulting value, using {@code null} if the method returned void
	 */
	private static StorageValue dealWithReturnVoid(MethodCallTransactionRequest request, StorageValueModel model) {
		return request.getStaticTarget() instanceof VoidMethodSignature ? null : model.toBean();
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
		TransactionReference reference = RestClientService.post(config.url + "/post/instanceMethodCallTransaction", modelFor(request), TransactionReferenceModel.class).toBean();
		return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
	}

	private static InstanceMethodCallTransactionRequestModel modelFor(InstanceMethodCallTransactionRequest request) {
		// we consider the special, optimized request
		if (request instanceof TransferTransactionRequest)
			return new TransferTransactionRequestModel((TransferTransactionRequest) request);
		else
			return new InstanceMethodCallTransactionRequestModel(request);
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		TransactionReference reference = RestClientService.post(config.url + "/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
		return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
	}

	/**
	 * Build the transaction request from the given model.
	 *
	 * @param gson the gson instance
	 * @param restRequestModel the request model
	 * @return the corresponding transaction request
	 */
	private static TransactionRequest<?> requestFromModel(TransactionRestRequestModel<?> restRequestModel) {
		if (restRequestModel == null)
			throw new InternalFailureException("unexpected null rest request model");

		if (restRequestModel.type == null)
			throw new InternalFailureException("unexpected null rest request type model");

		if (restRequestModel.transactionRequestModel == null)
			throw new InternalFailureException("unexpected null rest request object model");

		final Gson gson = new Gson();
		final String serialized = serialize(gson, restRequestModel);

		if (serialized == null)
			throw new InternalFailureException("unexpected null serialized object");
		if (restRequestModel.type.equals(ConstructorCallTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, ConstructorCallTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(GameteCreationTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, GameteCreationTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(InitializationTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, InitializationTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(InstanceMethodCallTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, InstanceMethodCallTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(JarStoreInitialTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, JarStoreInitialTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(JarStoreTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, JarStoreTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(RedGreenGameteCreationTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, RedGreenGameteCreationTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(StaticMethodCallTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, StaticMethodCallTransactionRequestModel.class).toBean();
		else if (restRequestModel.type.equals(TransferTransactionRequestModel.class.getName()))
			return gson.fromJson(serialized, TransferTransactionRequestModel.class).toBean();
		else
			throw new InternalFailureException("unexpected transaction request model of class " + restRequestModel.type);
	}

	/**
	 * Serializes the transaction request model of the rest model
	 * @param gson the gson instance
	 * @param restRequestModel the rest model
	 * @return the string
	 */
	private static String serialize(Gson gson, TransactionRestRequestModel<?> restRequestModel) {
		try {
			return gson.toJsonTree(restRequestModel.transactionRequestModel).toString();
		}
		catch (Exception e) {
			throw new InternalFailureException("unexpected serialization error");
		}
	}

	/**
     * Builds the transaction response for the given rest response model.
     *
     * @param restResponseModel the rest response model
     * @return the corresponding transaction response
     */
    private static TransactionResponse responseFromModel(TransactionRestResponseModel<?> restResponseModel) {
    	if (restResponseModel == null)
            throw new InternalFailureException("unexpected null rest response model");

        if (restResponseModel.type == null)
            throw new InternalFailureException("unexpected null rest response type model");

        if (restResponseModel.transactionResponseModel == null)
            throw new InternalFailureException("unexpected null rest response object model");

        final Gson gson = new Gson();
        final String serialized = serialize(gson, restResponseModel);

        if (serialized == null)
            throw new InternalFailureException("unexpected null serialized object");
        else if (restResponseModel.type.equals(JarStoreInitialTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(GameteCreationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreInitialTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(InitializationTransactionResponseModel.class.getName()))
            return gson.fromJson(serialized, InitializationTransactionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(JarStoreTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.type.equals(JarStoreTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, JarStoreTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(ConstructorCallTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.type.equals(ConstructorCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(ConstructorCallTransactionExceptionResponseModel.class.getName()))
            return gson.fromJson(serialized, ConstructorCallTransactionExceptionResponseModel.class).toBean();
        else if (restResponseModel.type.equals(MethodCallTransactionFailedResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionFailedResponseModel.class).toBean();
        else if (restResponseModel.type.equals(MethodCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(VoidMethodCallTransactionSuccessfulResponseModel.class.getName()))
            return gson.fromJson(serialized, VoidMethodCallTransactionSuccessfulResponseModel.class).toBean();
        else if (restResponseModel.type.equals(MethodCallTransactionExceptionResponseModel.class.getName()))
            return gson.fromJson(serialized, MethodCallTransactionExceptionResponseModel.class).toBean();
        else
            throw new InternalFailureException("unexpected transaction rest response model of class " + restResponseModel.type);
    }

    /**
     * Serializes the transaction response model of the rest model
     * @param gson the gson instance
     * @param restResponseModel the rest model
     * @return the string
     */
    private static String serialize(Gson gson, TransactionRestResponseModel<?> restResponseModel) {

        try {
            return gson.toJsonTree(restResponseModel.transactionResponseModel).toString();
        }
        catch (Exception e) {
            throw new InternalFailureException("unexpected serialization error");
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