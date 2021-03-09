package io.hotmoka.remote.internal.http;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.remote.RemoteNodeConfig;
import io.hotmoka.remote.internal.AbstractRemoteNode;
import io.hotmoka.remote.internal.http.client.RestClientService;
import io.hotmoka.service.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.service.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.service.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.TransactionRestRequestModel;
import io.hotmoka.service.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.service.models.responses.TransactionRestResponseModel;
import io.hotmoka.service.models.updates.ClassTagModel;
import io.hotmoka.service.models.updates.StateModel;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.StorageValueModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

/**
 * The implementation of a node that forwards all its calls to a remote service,
 * by using the HTTP protocol.
 */
@ThreadSafe
public class HTTPRemoteNodeImpl extends AbstractRemoteNode {

    /**
     * The URL of the remote service, including the HTTP protocol.
     */
    private final String url;

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     */
    public HTTPRemoteNodeImpl(RemoteNodeConfig config) {
        super(config);

        this.url = "http://" + config.url;
    }

    @Override
    public TransactionReference getTakamakaCode() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.get(url + "/get/takamakaCode", TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference getManifest() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.get(url + "/get/manifest", StorageReferenceModel.class).toBean());
    }

    @Override
    public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.post(url + "/get/classTag", new StorageReferenceModel(reference), ClassTagModel.class).toBean(reference));
    }

    @Override
    public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> RestClientService.post(url + "/get/state", new StorageReferenceModel(reference), StateModel.class).toBean());
    }

    @Override
    public SignatureAlgorithm<SignedTransactionRequest> getSignatureAlgorithmForRequests() {
        SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionForGetSignatureAlgorithmForRequests(() -> RestClientService.get(url + "/get/signatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class));
        return signatureAlgorithmFromModel(algoModel);
    }

    @Override
    public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> requestFromModel(RestClientService.post(url + "/get/request", new TransactionReferenceModel(reference), TransactionRestRequestModel.class)));
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
        return wrapNetworkExceptionForResponseAtException(() -> responseFromModel(RestClientService.post(url + "/get/response", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
    }

    @Override
    public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
        return wrapNetworkExceptionForPolledResponseException(() -> responseFromModel(RestClientService.post(url + "/get/polledResponse", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
    }

    @Override
    public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> RestClientService.post(url + "/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> RestClientService.post(url + "/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple(() -> RestClientService.post(url + "/add/initializationTransaction", new InitializationTransactionRequestModel(request), Void.class));
    }

    @Override
    public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
        return wrapNetworkExceptionMedium(() -> RestClientService.post(url + "/add/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> RestClientService.post(url + "/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
    }

    @Override
    public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(url + "/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(url + "/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(url + "/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(url + "/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> RestClientService.post(url + "/post/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
        return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> RestClientService.post(url + "/post/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
        return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = RestClientService.post(url + "/post/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
        return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = RestClientService.post(url + "/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
        return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
    }
}