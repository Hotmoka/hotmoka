package io.hotmoka.network.models.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

import java.util.Base64;

@Immutable
public abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
    public final String signature;
    public final StorageReferenceModel caller;
    public final String nonce;
    public final TransactionReferenceModel classpath;
    public final String chainId;
    public final String gasLimit;
    public final String gasPrice;

    protected NonInitialTransactionRequestModel(NonInitialTransactionRequest<?> request) {
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    	this.caller = new StorageReferenceModel(request.caller);
    	this.nonce = request.nonce.toString();
    	this.classpath = new TransactionReferenceModel(request.classpath);
    	this.chainId = request.chainId;
    	this.gasLimit = request.gasLimit.toString();
    	this.gasPrice = request.gasPrice.toString();
    }
}