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

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.network.values.TransactionReferenceModel;

/**
 * The model of an initial jar store transaction request.
 */
public class JarStoreInitialTransactionRequestModel extends InitialTransactionRequestModel {
	public String jar;
    public List<TransactionReferenceModel> dependencies;

    public JarStoreInitialTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public JarStoreInitialTransactionRequestModel(JarStoreInitialTransactionRequest request) {
    	this.jar = Base64.getEncoder().encodeToString(request.getJar());
    	this.dependencies = request.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
    }

    /**
     * Yields the request having this model.
     * 
     * @return the request
     */
    public JarStoreInitialTransactionRequest toBean() {
    	if (jar == null)
    		throw new RuntimeException("unexpected null jar");

    	return new JarStoreInitialTransactionRequest(decodeBase64(jar),
    		dependencies.stream().map(TransactionReferenceModel::toBean).toArray(TransactionReference[]::new));
    }
}