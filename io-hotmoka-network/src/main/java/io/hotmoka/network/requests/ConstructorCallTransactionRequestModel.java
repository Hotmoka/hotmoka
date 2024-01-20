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

import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.crypto.Base64;
import io.hotmoka.network.signatures.ConstructorSignatureModel;
import io.hotmoka.network.values.StorageValueModel;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The model of a constructor call transaction.
 */
public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    public ConstructorSignatureModel constructorSignature;
    public List<StorageValueModel> actuals;
    public String chainId;
    public String signature;

    public ConstructorCallTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public ConstructorCallTransactionRequestModel(ConstructorCallTransactionRequest request) {
    	super(request);

    	this.signature = Base64.toBase64String(request.getSignature());
    	this.chainId = request.getChainId();
    	this.constructorSignature = new ConstructorSignatureModel(request.getStaticTarget());
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

    public ConstructorCallTransactionRequest toBean() {
    	return TransactionRequests.constructorCall(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            constructorSignature.toBean(),
            actuals.stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}