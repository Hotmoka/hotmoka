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

import io.hotmoka.beans.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;

public class InstanceSystemMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	public StorageReferenceModel receiver;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InstanceSystemMethodCallTransactionRequestModel(InstanceSystemMethodCallTransactionRequest request) {
    	super(request);

    	this.receiver = new StorageReferenceModel(request.receiver);
    }

    public InstanceSystemMethodCallTransactionRequestModel() {}

    public InstanceSystemMethodCallTransactionRequest toBean() {
    	return new InstanceSystemMethodCallTransactionRequest(
            caller.toBean(),
            new BigInteger(nonce),
            new BigInteger(gasLimit),
            classpath.toBean(),
            method.toBean(),
            receiver.toBean(),
            getActuals().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}