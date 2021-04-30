/*
Copyright 2021 Fausto Spoto

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

import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.network.values.TransactionReferenceModel;

public class GameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	public String initialAmount;
    public String redInitialAmount;
	public String publicKey;
	public TransactionReferenceModel classpath;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public GameteCreationTransactionRequestModel(GameteCreationTransactionRequest request) {
    	this.initialAmount = request.initialAmount.toString();
    	this.redInitialAmount = request.redInitialAmount.toString();
    	this.publicKey = request.publicKey;
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public GameteCreationTransactionRequestModel() {}

    public GameteCreationTransactionRequest toBean() {
    	return new GameteCreationTransactionRequest(classpath.toBean(), new BigInteger(initialAmount), new BigInteger(redInitialAmount), publicKey);
    }
}