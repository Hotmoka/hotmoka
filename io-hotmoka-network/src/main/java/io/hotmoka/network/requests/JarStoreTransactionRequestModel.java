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

package io.hotmoka.network.requests;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.network.values.TransactionReferenceModel;

/**
 * The model of a jar store transaction request.
 */
public class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    public String jar;
    private List<TransactionReferenceModel> dependencies;
    public String chainId;
    public String signature;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public JarStoreTransactionRequestModel(JarStoreTransactionRequest request) {
    	super(request);

    	this.chainId = request.chainId;
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    	this.jar = Base64.getEncoder().encodeToString(request.getJar());
    	this.dependencies = request.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
    }

    public JarStoreTransactionRequestModel() {}

    public final Stream<TransactionReferenceModel> getDependencies() {
    	return dependencies.stream();
    }

    public JarStoreTransactionRequest toBean() {
    	return new JarStoreTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            decodeBase64(jar),
            dependencies.stream().map(TransactionReferenceModel::toBean).toArray(TransactionReference[]::new));
    }
}