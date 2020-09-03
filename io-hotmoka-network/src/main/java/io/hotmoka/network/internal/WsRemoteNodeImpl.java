package io.hotmoka.network.internal;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

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
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.internal.websocket.WebsocketClient;
import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.RedGreenGameteCreationTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.TransactionRestRequestModel;
import io.hotmoka.network.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * An implementation of the remote node that connects through websockets.
 */
public class WsRemoteNodeImpl extends AbstractRemoteNode {

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
    	super(config);

    	this.websocketClient = ThreadLocal.withInitial(() -> {
            try {
                return new WebsocketClient(config.url +  "/node");
            }
            catch (ExecutionException | InterruptedException e) {
               throw InternalFailureException.of(e);
            }
        });
    }

    @Override
    public TransactionReference getTakamakaCode() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {
           WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/takamakaCode", TransactionReferenceModel.class);
           websocketClient.get().send("/get/takamakaCode");

           return ((TransactionReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference getManifest() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/manifest", StorageReferenceModel.class);
            websocketClient.get().send("/get/manifest");

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/classTag", ClassTagModel.class);
            websocketClient.get().send("/get/classTag", new StorageReferenceModel(reference));

            return ((ClassTagModel) subscription.get()).toBean(reference);
        });
    }

    @Override
    public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/state", StateModel.class);
            websocketClient.get().send("/get/state", new StorageReferenceModel(reference));

            return ((StateModel) subscription.get()).toBean();
        });
    }

	@Override
    public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
        SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionForNoSuchAlgorithmException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/signatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class);
            websocketClient.get().send("/get/signatureAlgorithmForRequests");

            return ((SignatureAlgorithmResponseModel) subscription.get());
        });

        return signatureAlgorithmFromModel(algoModel);
    }

    @Override
    public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/request", TransactionRestRequestModel.class);
            websocketClient.get().send("/get/request", new TransactionReferenceModel(reference));

            return requestFromModel((TransactionRestRequestModel<?>) subscription.get());
        });
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
        return wrapNetworkExceptionForResponseAtException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/response", TransactionRestResponseModel.class);
            websocketClient.get().send("/get/response", new TransactionReferenceModel(reference));

            return responseFromModel((TransactionRestResponseModel<?>) subscription.get());
        });
    }

    @Override
    public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
        return wrapNetworkExceptionForPolledResponseException(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/get/polledResponse", TransactionRestResponseModel.class);
            websocketClient.get().send("/get/polledResponse", new TransactionReferenceModel(reference));

            return responseFromModel((TransactionRestResponseModel<?>) subscription.get());
        });
    }

    @Override
    public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/jarStoreInitialTransaction", TransactionReferenceModel.class);
            websocketClient.get().send("/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/gameteCreationTransaction", StorageReferenceModel.class);
            websocketClient.get().send("/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request));

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/redGreenGameteCreationTransaction", StorageReferenceModel.class);
            websocketClient.get().send("/add/redGreenGameteCreationTransaction", new RedGreenGameteCreationTransactionRequestModel(request));

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/initializationTransaction", Void.class);
            websocketClient.get().send("/add/initializationTransaction", new InitializationTransactionRequestModel(request));

            return subscription.get();
        });
    }

    @Override
    public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
        return wrapNetworkExceptionMedium(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/jarStoreTransaction", TransactionReferenceModel.class);
            websocketClient.get().send("/add/jarStoreTransaction", new JarStoreTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/constructorCallTransaction", StorageReferenceModel.class);
            websocketClient.get().send("/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            return ((StorageReferenceModel) subscription.get()).toBean();
        });
    }

    @Override
    public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/instanceMethodCallTransaction", StorageValueModel.class);
            websocketClient.get().send("/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/add/staticMethodCallTransaction", StorageValueModel.class);
            websocketClient.get().send("/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/run/instanceMethodCallTransaction", StorageValueModel.class);
            websocketClient.get().send("/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/run/staticMethodCallTransaction", StorageValueModel.class);
            websocketClient.get().send("/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request));

            return dealWithReturnVoid(request, (StorageValueModel) subscription.get());
        });
    }

    @Override
    public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/post/jarStoreTransaction", TransactionReferenceModel.class);
            websocketClient.get().send("/post/jarStoreTransaction", new JarStoreTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/post/constructorCallTransaction", TransactionReferenceModel.class);
            websocketClient.get().send("/post/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/post/instanceMethodCallTransaction", TransactionReferenceModel.class);
            websocketClient.get().send("/post/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> {
            WebsocketClient.Subscription<?> subscription = websocketClient.get().subscribe("/post/staticMethodCallTransaction", TransactionReferenceModel.class);
            websocketClient.get().send("/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request));

            return ((TransactionReferenceModel) subscription.get()).toBean();
        });

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public void close() throws Exception {
        websocketClient.get().disconnect();
    }
}