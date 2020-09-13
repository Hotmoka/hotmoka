package io.hotmoka.network.internal;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
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
     * The websocket client for the remote node. There is one per thread,
     * in order to avoid race conditions.
     */
    private final ThreadLocal<WebsocketClient> websocketClient;

    private final Set<WebsocketClient> allClients = new HashSet<>();

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     */
    public WsRemoteNodeImpl(RemoteNodeConfig config) {
    	super(config);

    	this.websocketClient = ThreadLocal.withInitial(() -> {
            try {
            	WebsocketClient client = new WebsocketClient(config.url +  "/node");

            	synchronized (allClients) {
            		allClients.add(client);
            	}

            	return client;
            }
            catch (ExecutionException | InterruptedException e) {
            	throw InternalFailureException.of(e);
            }
        });
    }

    @Override
    public TransactionReference getTakamakaCode() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
        	(() -> subscribeAndSend("/get/takamakaCode", TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference getManifest() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
        	(() -> subscribeAndSend("/get/manifest", StorageReferenceModel.class).toBean());
    }

    @Override
    public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
        	(() -> subscribeAndSend("/get/classTag", ClassTagModel.class, new StorageReferenceModel(reference)).toBean(reference));
    }

    @Override
    public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
       		(() -> subscribeAndSend("/get/state", StateModel.class, new StorageReferenceModel(reference)).toBean());
    }

	@Override
    public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
        SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionForNoSuchAlgorithmException
       		(() -> subscribeAndSend("/get/signatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class));

        return signatureAlgorithmFromModel(algoModel);
    }

    @Override
    public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
       		(() -> requestFromModel(subscribeAndSend("/get/request", TransactionRestRequestModel.class, new TransactionReferenceModel(reference))));
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
        return wrapNetworkExceptionForResponseAtException
       		(() -> responseFromModel(subscribeAndSend("/get/response", TransactionRestResponseModel.class, new TransactionReferenceModel(reference))));
    }

    @Override
    public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {    	
        return wrapNetworkExceptionForPolledResponseException
       		(() -> responseFromModel(subscribeAndSend("/get/polledResponse", TransactionRestResponseModel.class, new TransactionReferenceModel(reference))));
    }

    @Override
    public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
    	return wrapNetworkExceptionSimple
   			(() -> subscribeAndSend("/add/jarStoreInitialTransaction", TransactionReferenceModel.class, new JarStoreInitialTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple
       		(() -> subscribeAndSend("/add/gameteCreationTransaction", StorageReferenceModel.class, new GameteCreationTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple
       		(() -> subscribeAndSend("/add/redGreenGameteCreationTransaction", StorageReferenceModel.class, new RedGreenGameteCreationTransactionRequestModel(request)).toBean());
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple
        	(() -> subscribeAndSend("/add/initializationTransaction", Void.class, new InitializationTransactionRequestModel(request)));
    }

    @Override
    public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
        return wrapNetworkExceptionMedium
       		(() -> subscribeAndSend("/add/jarStoreTransaction", TransactionReferenceModel.class, new JarStoreTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> subscribeAndSend("/add/constructorCallTransaction", StorageReferenceModel.class, new ConstructorCallTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, subscribeAndSend("/add/instanceMethodCallTransaction", StorageValueModel.class, new InstanceMethodCallTransactionRequestModel(request))));
    }

    @Override
    public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, subscribeAndSend("/add/staticMethodCallTransaction", StorageValueModel.class, new StaticMethodCallTransactionRequestModel(request))));
    }

    @Override
    public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, subscribeAndSend("/run/instanceMethodCallTransaction", StorageValueModel.class, new InstanceMethodCallTransactionRequestModel(request))));
    }

    @Override
    public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, subscribeAndSend("/run/staticMethodCallTransaction", StorageValueModel.class, new StaticMethodCallTransactionRequestModel(request))));
    }

    @Override
    public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> subscribeAndSend("/post/jarStoreTransaction", TransactionReferenceModel.class, new JarStoreTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> subscribeAndSend("/post/constructorCallTransaction", TransactionReferenceModel.class, new ConstructorCallTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> subscribeAndSend("/post/instanceMethodCallTransaction", TransactionReferenceModel.class, new InstanceMethodCallTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> subscribeAndSend("/post/staticMethodCallTransaction", TransactionReferenceModel.class, new StaticMethodCallTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
	public void close() {
    	Set<WebsocketClient> copyOfAllClients;

    	synchronized (allClients) {
    		copyOfAllClients = allClients;
    		allClients.clear();
    	}

    	copyOfAllClients.forEach(WebsocketClient::close);
    }

	/**
	 * Subscribes to the given topic and sends it to the websocket server.
	 * 
	 * @param <T> the type of the expected result of the subscription
	 * @param topic the topic
	 * @param model the class of the expected result of the subscription
	 * @return the result of the subscription
	 * @throws ExecutionException if the subscription throws that
	 * @throws InterruptedException if the subscription throws that
	 */
	private <T> T subscribeAndSend(String topic, Class<T> model) throws ExecutionException, InterruptedException {
		return websocketClient.get().subscribeAndSend(topic, model, Optional.empty());
	}

	/**
	 * Subscribes to the given topic and sends it to the websocket server, with a payload.
	 * 
	 * @param <T> the type of the expected result of the subscription
	 * @param topic the topic
	 * @param model the class of the expected result of the subscription
	 * @param payload the payload
	 * @return the result of the subscription
	 * @throws ExecutionException if the subscription throws that
	 * @throws InterruptedException if the subscription throws that
	 */
	private <T> T subscribeAndSend(String topic, Class<T> model, Object payload) throws ExecutionException, InterruptedException {
		return websocketClient.get().subscribeAndSend(topic, model, Optional.of(payload));
	}
}