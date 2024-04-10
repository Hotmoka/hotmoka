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

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.network.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.remote.internal.AbstractRemoteNode;
import io.hotmoka.node.remote.internal.http.client.RestClientService;
import jakarta.websocket.DeploymentException;

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
    private final RestClientService service = new RestClientService();

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     * @throws IOException 
     * @throws DeploymentException 
     */
    public HTTPRemoteNodeImpl(RemoteNodeConfig config) throws IOException, DeploymentException {
        super(config);

        this.url = "http://" + config.getURL();
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple(() -> service.post(url + "/add/gameteCreationTransaction", new GameteCreationTransactionRequestModel(request), StorageReferenceModel.class).toBean());
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple(() -> service.post(url + "/add/initializationTransaction", new InitializationTransactionRequestModel(request), Void.class));
    }
}