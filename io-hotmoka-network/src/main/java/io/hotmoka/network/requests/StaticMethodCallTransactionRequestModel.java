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

import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.network.values.StorageValueModel;

public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
    public String chainId;
	public String signature;

	/**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public StaticMethodCallTransactionRequestModel(StaticMethodCallTransactionRequest request) {
    	super(request);

    	this.chainId = request.getChainId();
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    }

    public StaticMethodCallTransactionRequestModel() {}

    public StaticMethodCallTransactionRequest toBean() {
		return TransactionRequests.staticMethodCall(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            method.toBean(),
            getActuals().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
	}
}