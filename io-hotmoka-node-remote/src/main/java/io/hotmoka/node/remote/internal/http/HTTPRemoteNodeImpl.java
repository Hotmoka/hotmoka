/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.remote.internal.http;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.NodeInfo;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.nodes.NodeInfoModel;
import io.hotmoka.network.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.remote.internal.AbstractRemoteNode;
import io.hotmoka.node.remote.internal.http.client.RestClientService;

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
     * The service used for the connection to the server.
     */
    private RestClientService service = new RestClientService();

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     * @throws IOException 
     */
    public HTTPRemoteNodeImpl(RemoteNodeConfig config) throws IOException {
        super(config);

        this.url = "http://" + config.getURL();
    }

    @Override
    public NodeInfo getNodeInfo() {
    	return wrapNetworkExceptionBasic(() -> service.get(url + "/get/nodeID", NodeInfoModel.class).toBean());
    }

    @Override
    public TransactionReference getTakamakaCode() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> service.get(url + "/get/takamakaCode", TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference getManifest() throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> service.get(url + "/get/manifest", StorageReferenceModel.class).toBean());
    }

    @Override
    public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> service.post(url + "/get/classTag", new StorageReferenceModel(reference), ClassTagModel.class).toBean(reference));
    }

    @Override
    public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> service.post(url + "/get/state", new StorageReferenceModel(reference), StateModel.class).toBean());
    }

    @Override
    public String getNameOfSignatureAlgorithmForRequests() {
        SignatureAlgorithmResponseModel algoModel = wrapNetworkExceptionBasic(() -> service.get(url + "/get/nameOfSignatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class));
        return algoModel.algorithm;
    }

    @Override
    public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
        return wrapNetworkExceptionForNoSuchElementException(() -> requestFromModel(service.post(url + "/get/request", new TransactionReferenceModel(reference), TransactionRestRequestModel.class)));
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
        return wrapNetworkExceptionForResponseAtException(() -> responseFromModel(service.post(url + "/get/response", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
    }

    @Override
    public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
        return wrapNetworkExceptionForPolledResponseException(() -> responseFromModel(service.post(url + "/get/polledResponse", new TransactionReferenceModel(reference), TransactionRestResponseModel.class)));
    }

    @Override
    public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> service.post(url + "/add/jarStoreInitialTransaction", new JarStoreInitialTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> service.post(url + "/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple(() -> service.post(url + "/add/initializationTransaction", new InitializationTransactionRequestModel(request), Void.class));
    }

    @Override
    public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
        return wrapNetworkExceptionMedium(() -> service.post(url + "/add/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
    }

    @Override
    public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> service.post(url + "/add/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), StorageReferenceModel.class).toBean());
    }

    @Override
    public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, service.post(url + "/add/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, service.post(url + "/add/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, service.post(url + "/run/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
        return wrapNetworkExceptionFull(() -> dealWithReturnVoid(request, service.post(url + "/run/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), StorageValueModel.class)));
    }

    @Override
    public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> service.post(url + "/post/jarStoreTransaction", new JarStoreTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
        return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = wrapNetworkExceptionSimple(() -> service.post(url + "/post/constructorCallTransaction", new ConstructorCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean());
        return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = service.post(url + "/post/instanceMethodCallTransaction", new InstanceMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
        return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
    }

    @Override
    public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
        TransactionReference reference = service.post(url + "/post/staticMethodCallTransaction", new StaticMethodCallTransactionRequestModel(request), TransactionReferenceModel.class).toBean();
        return wrapNetworkExceptionSimple(() -> methodSupplierFor(reference));
    }
}