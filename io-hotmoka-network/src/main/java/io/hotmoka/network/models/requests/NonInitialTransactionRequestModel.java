package io.hotmoka.network.models.requests;

import java.math.BigInteger;
import java.util.Base64;

import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
    private String signature;
    private StorageReferenceModel caller;
    private BigInteger nonce;
    private TransactionReferenceModel classpath;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;

    /**
     * For Spring.
     */
    protected NonInitialTransactionRequestModel() {}

    protected NonInitialTransactionRequestModel(NonInitialTransactionRequest<?> request) {
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    	this.caller = new StorageReferenceModel(request.caller);
    	this.nonce = request.nonce;
    	this.classpath = new TransactionReferenceModel(request.classpath);
    	this.chainId = request.chainId;
    	this.gasLimit = request.gasLimit;
    	this.gasPrice = request.gasPrice;
    }

    protected final TransactionReferenceModel getClasspath() {
        return classpath;
    }

    public void setClasspath(TransactionReferenceModel classpath) {
        this.classpath = classpath;
    }

    protected final BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    protected final StorageReferenceModel getCaller() {
        return caller;
    }

    public void setCaller(StorageReferenceModel caller) {
        this.caller = caller;
    }

    protected final String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    protected final String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    protected final BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }

    protected final BigInteger getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }
}
