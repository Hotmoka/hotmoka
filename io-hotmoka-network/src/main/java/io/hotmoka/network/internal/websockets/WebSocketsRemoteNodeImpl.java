package io.hotmoka.network.internal.websockets;

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
import io.hotmoka.network.internal.AbstractRemoteNode;
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
 * The implementation of a node that forwards all its calls to a remote service,
 * by using websockets.
 */
public class WebSocketsRemoteNodeImpl extends AbstractRemoteNode {

    /**
     * The websockets client for the remote node. There is one per thread,
     * in order to avoid race conditions.
     */
    private final ThreadLocal<WebsocketsClient> websocketClient;

    /**
     * All clients created so far.
     */
    private final Set<WebsocketsClient> allClients = new HashSet<>();

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     */
    public WebSocketsRemoteNodeImpl(RemoteNodeConfig config) {
    	super(config);

    	this.websocketClient = ThreadLocal.withInitial(() -> {
            try {
            	WebsocketsClient client = new WebsocketsClient(config.url +  "/node");

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
        	(() -> send("/get/takamakaCode", TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference getManifest() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
        	(() -> send("/get/manifest", StorageReferenceModel.class).toBean());
    }

    @Override
    public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
        	(() -> send("/get/classTag", ClassTagModel.class, new StorageReferenceModel(reference)).toBean(reference));
    }

    @Override
    public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
       		(() -> send("/get/state", StateModel.class, new StorageReferenceModel(reference)).toBean());
    }

	@Override
    public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
        SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionForNoSuchAlgorithmException
       		(() -> send("/get/signatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class));

        return signatureAlgorithmFromModel(algoModel);
    }

    @Override
    public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException
       		(() -> requestFromModel(send("/get/request", TransactionRestRequestModel.class, new TransactionReferenceModel(reference))));
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
        return wrapNetworkExceptionForResponseAtException
       		(() -> responseFromModel(send("/get/response", TransactionRestResponseModel.class, new TransactionReferenceModel(reference))));
    }

    @Override
    public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {    	
        return wrapNetworkExceptionForPolledResponseException
       		(() -> responseFromModel(send("/get/polledResponse", TransactionRestResponseModel.class, new TransactionReferenceModel(reference))));
    }

    @Override
    public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
    	return wrapNetworkExceptionSimple
   			(() -> send("/add/jarStoreInitialTransaction", TransactionReferenceModel.class, new JarStoreInitialTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple
       		(() -> send("/add/gameteCreationTransaction", StorageReferenceModel.class, new GameteCreationTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple
       		(() -> send("/add/redGreenGameteCreationTransaction", StorageReferenceModel.class, new RedGreenGameteCreationTransactionRequestModel(request)).toBean());
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple
        	(() -> send("/add/initializationTransaction", Void.class, new InitializationTransactionRequestModel(request)));
    }

    @Override
    public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
        return wrapNetworkExceptionMedium
       		(() -> send("/add/jarStoreTransaction", TransactionReferenceModel.class, new JarStoreTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> send("/add/constructorCallTransaction", StorageReferenceModel.class, new ConstructorCallTransactionRequestModel(request)).toBean());
    }

    @Override
    public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, send("/add/instanceMethodCallTransaction", StorageValueModel.class, new InstanceMethodCallTransactionRequestModel(request))));
    }

    @Override
    public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, send("/add/staticMethodCallTransaction", StorageValueModel.class, new StaticMethodCallTransactionRequestModel(request))));
    }

    @Override
    public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, send("/run/instanceMethodCallTransaction", StorageValueModel.class, new InstanceMethodCallTransactionRequestModel(request))));
    }

    @Override
    public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull
       		(() -> dealWithReturnVoid(request, send("/run/staticMethodCallTransaction", StorageValueModel.class, new StaticMethodCallTransactionRequestModel(request))));
    }

    @Override
    public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> send("/post/jarStoreTransaction", TransactionReferenceModel.class, new JarStoreTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> send("/post/constructorCallTransaction", TransactionReferenceModel.class, new ConstructorCallTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> send("/post/instanceMethodCallTransaction", TransactionReferenceModel.class, new InstanceMethodCallTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple
       		(() -> send("/post/staticMethodCallTransaction", TransactionReferenceModel.class, new StaticMethodCallTransactionRequestModel(request)).toBean());

        return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
	public void close() {
    	Set<WebsocketsClient> copyOfAllClients;

    	synchronized (allClients) {
    		copyOfAllClients = allClients;
    		allClients.clear();
    	}

    	copyOfAllClients.forEach(WebsocketsClient::close);
    }

	/**
	 * Sends a request for the given topic and yields the result.
	 * 
	 * @param <T> the type of the expected result
	 * @param topic the topic
	 * @param model the class of the expected result
	 * @return the result
	 * @throws ExecutionException if the websockets subscription throws that
	 * @throws InterruptedException if the websockets subscription throws that
	 */
	private <T> T send(String topic, Class<T> model) throws ExecutionException, InterruptedException {
		return websocketClient.get().send(topic, model, Optional.empty());
	}

	/**
	 * Sends a request for the given topic and yields the result.
	 * 
	 * @param <T> the type of the expected result
	 * @param topic the topic
	 * @param model the class of the expected result
	 * @param payload the payload of the request
	 * @return the result
	 * @throws ExecutionException if the websockets subscription throws that
	 * @throws InterruptedException if the websockets subscription throws that
	 */
	private <T> T send(String topic, Class<T> model, Object payload) throws ExecutionException, InterruptedException {
		return websocketClient.get().send(topic, model, Optional.of(payload));
	}
}