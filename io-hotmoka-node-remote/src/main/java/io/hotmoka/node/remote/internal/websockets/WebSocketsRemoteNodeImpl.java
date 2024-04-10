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

package io.hotmoka.node.remote.internal.websockets;

import java.io.IOException;

import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.network.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.remote.internal.AbstractRemoteNode;
import jakarta.websocket.DeploymentException;

/**
 * The implementation of a node that forwards all its calls to a remote service,
 * by using websockets.
 */
public class WebSocketsRemoteNodeImpl extends AbstractRemoteNode {

    /**
     * Builds the remote node.
     *
     * @param config the configuration of the node
     * @throws IOException 
     * @throws DeploymentException 
     */
    public WebSocketsRemoteNodeImpl(RemoteNodeConfig config) throws IOException, DeploymentException {
        super(config);
    }

    @Override
    public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
        return wrapNetworkExceptionSimple
                (() -> send("/add/gameteCreationTransaction", StorageReferenceModel.class, new GameteCreationTransactionRequestModel(request)).toBean());
    }

    @Override
    public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
        wrapNetworkExceptionSimple
                (() -> send("/add/initializationTransaction", Void.class, new InitializationTransactionRequestModel(request)));
    }

    /**
     * Sends a request for the given topic and yields the result.
     *
     * @param <T> the type of the expected result
     * @param <P> the type of the payload
     * @param topic the topic
     * @param model the class of the expected result
     * @param payload the payload of the request
     * @return the result
     * @throws InterruptedException if the websockets subscription throws that
     */
    private <T, P> T send(String topic, Class<T> model, P payload) throws InterruptedException {
        return webSocketClient.subscribeAndSend(topic, model, payload);
    }
}