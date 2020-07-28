package io.hotmoka.network.models.requests;

import java.math.BigInteger;
import java.util.Base64;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@Immutable
public abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
    public final String signature;
    public final StorageReferenceModel caller;
    public final BigInteger nonce;
    public final TransactionReferenceModel classpath;
    public final String chainId;
    public final BigInteger gasLimit;
    public final BigInteger gasPrice;

    protected NonInitialTransactionRequestModel(NonInitialTransactionRequest<?> request) {
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    	this.caller = new StorageReferenceModel(request.caller);
    	this.nonce = request.nonce;
    	this.classpath = new TransactionReferenceModel(request.classpath);
    	this.chainId = request.chainId;
    	this.gasLimit = request.gasLimit;
    	this.gasPrice = request.gasPrice;
    }
}