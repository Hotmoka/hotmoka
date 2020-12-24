package io.hotmoka.network.models.requests;

import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

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