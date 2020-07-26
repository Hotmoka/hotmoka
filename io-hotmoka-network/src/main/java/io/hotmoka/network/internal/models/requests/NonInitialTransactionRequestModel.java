package io.hotmoka.network.internal.models.requests;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

import java.math.BigInteger;

public abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
    private String signature;
    private StorageReferenceModel caller;
    private BigInteger nonce;
    private String classpath;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;

    protected final String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
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
