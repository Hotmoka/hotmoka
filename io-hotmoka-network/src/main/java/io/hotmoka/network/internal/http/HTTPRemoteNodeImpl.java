package io.hotmoka.network.internal.http;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
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
import io.hotmoka.network.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * The implementation of a node that forwards all its calls to a remote service,
 * by using the HTTP protocol.
 */
@ThreadSafe
public class HTTPRemoteNodeImpl extends AbstractRemoteNode {

	/**
	 * Builds the remote node.
	 * 
	 * @param config the configuration of the node
	 */
	public HTTPRemoteNodeImpl(RemoteNodeConfig config) {
		super(config);
	}

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

	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionForNoSuchAlgorithmException(() -> RestClientService.get(config.url + "/get/signatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class));
		return signatureAlgorithmFromModel(algoModel);
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
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class)));
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class)));
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, RestClientService.post(config.url + "/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
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
}