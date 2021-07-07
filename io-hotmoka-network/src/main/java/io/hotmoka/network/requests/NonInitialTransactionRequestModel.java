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

import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;

public abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
    public StorageReferenceModel caller;
    public String nonce;
    public TransactionReferenceModel classpath;
    public String gasLimit;
    public String gasPrice;

    protected NonInitialTransactionRequestModel(NonInitialTransactionRequest<?> request) {
    	this.caller = new StorageReferenceModel(request.caller);
    	this.nonce = request.nonce.toString();
    	this.classpath = new TransactionReferenceModel(request.classpath);
    	this.gasLimit = request.gasLimit.toString();
    	this.gasPrice = request.gasPrice.toString();
    }

    protected NonInitialTransactionRequestModel() {}
}