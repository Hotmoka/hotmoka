package io.hotmoka.network.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.*;
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
import io.hotmoka.network.internal.websocket.WebsocketClient;
import io.hotmoka.network.models.requests.*;
import io.hotmoka.network.models.responses.*;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import io.hotmoka.nodes.AbstractNodeWithSuppliers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class WsRemoteNodeImpl extends AbstractNodeWithSuppliers implements RemoteNode {

    /**
     * The websocket client for the remote node
     */
    private final ThreadLocal<WebsocketClient> websocketClient;

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     */
    public WsRemoteNodeImpl(RemoteNodeConfig config) {
        this.websocketClient = ThreadLocal.withInitial(() -> {
            try {
                return new WebsocketClient(config.url +  "/node");
            }
            catch (ExecutionException | InterruptedException e) {
               throw new InternalFailureException("Error creating webcosket client");
            }
        });
    }

    @Override
    public TransactionReference getTakamakaCode() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/takamakaCode", TransactionReferenceModel.class);
            this.websocketClient.get().send("/get/takamakaCode");

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference getManifest() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/manifest", StorageReferenceModel.class);
            this.websocketClient.get().send("/get/manifest");

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/classTag", ClassTagModel.class);
            this.websocketClient.get().send("/get/classTag", new StorageReferenceModel(reference));

            return ((ClassTagModel) subscription.get()).toBean(reference);
        });
    }

    @Override
    public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/state", StateModel.class);
            this.websocketClient.get().send("/get/state", new StorageReferenceModel(reference));

            return ((StateModel) subscription.get()).toBean();
        });
    }

    @SuppressWarnings("unchecked")
	@Override
    public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
        SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionForNoSuchAlgorithmException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/signatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class);
            this.websocketClient.get().send("/get/signatureAlgorithmForRequests");

            return ((SignatureAlgorithmResponseModel) subscription.get());
        });

        // we check if the name of the algorithm is among those that we know
        try {
            Method method = SignatureAlgorithm.class.getMethod(algoModel.algorithm, BytesSupplier.class);
            BytesSupplier<NonInitialTransactionRequest<?>> bytesSupplier = NonInitialTransactionRequest::toByteArrayWithoutSignature;
            return (SignatureAlgorithm<NonInitialTransactionRequest<?>>) method.invoke(null, bytesSupplier);
        }
        catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            throw new InternalFailureException("unknown remote signature algorithm named " + algoModel.algorithm);
        }
    }

    @Override
    public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/request", TransactionRestRequestModel.class);
            this.websocketClient.get().send("/get/request", new TransactionReferenceModel(reference));

            return requestFromModel((TransactionRestRequestModel<?>) subscription.get());
        });
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
        return wrapNetworkExceptionForResponseAtException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/response", TransactionRestResponseModel.class);
            this.websocketClient.get().send("/get/response", new TransactionReferenceModel(reference));

            return responseFromModel((TransactionRestResponseModel<?>) subscription.get());
        });
    }

    @Override
    public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
        return wrapNetworkExceptionForPolledResponseException(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/get/polledResponse", TransactionRestResponseModel.class);
            this.websocketClient.get().send("/get/polledResponse", new TransactionReferenceModel(reference));

            return responseFromModel((TransactionRestResponseModel<?>) subscription.get());
        });
    }

    @Override
    public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/jarStoreInitialTransaction", TransactionReferenceModel.class);
            this.websocketClient.get().send("/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/gameteCreationTransaction", StorageReferenceModel.class);
            this.websocketClient.get().send("/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request));

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/redGreenGameteCreationTransaction", StorageReferenceModel.class);
            this.websocketClient.get().send("/add/redGreenGameteCreationTransaction", new RedGreenGameteCreationTransactionRequestModel(request));

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/initializationTransaction", Void.class);
            this.websocketClient.get().send("/add/initializationTransaction", new InitializationTransactionRequestModel(request));

            return subscription.get();
        });
    }

    @Override
    public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
        return wrapNetworkExceptionMedium(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/jarStoreTransaction", TransactionReferenceModel.class);
            this.websocketClient.get().send("/add/jarStoreTransaction", new JarStoreTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/constructorCallTransaction", StorageReferenceModel.class);
            this.websocketClient.get().send("/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/instanceMethodCallTransaction", StorageValueModel.class);
            this.websocketClient.get().send("/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/add/staticMethodCallTransaction", StorageValueModel.class);
            this.websocketClient.get().send("/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/run/instanceMethodCallTransaction", StorageValueModel.class);
            this.websocketClient.get().send("/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/run/staticMethodCallTransaction", StorageValueModel.class);
            this.websocketClient.get().send("/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/post/jarStoreTransaction", TransactionReferenceModel.class);
            this.websocketClient.get().send("/post/jarStoreTransaction", new JarStoreTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/post/constructorCallTransaction", TransactionReferenceModel.class);
            this.websocketClient.get().send("/post/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/post/instanceMethodCallTransaction", TransactionReferenceModel.class);
            this.websocketClient.get().send("/post/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {

            WebsocketClient.Subscription subscription = this.websocketClient.get().subscribe("/post/staticMethodCallTransaction", TransactionReferenceModel.class);
            this.websocketClient.get().send("/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public void close() throws Exception {
        this.websocketClient.get().disconnect();
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
     * Build the transaction request from the given model.
     *
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

        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
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
        else
            throw new InternalFailureException("unexpected transaction request model of class " + restRequestModel.type);
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

        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
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
}
