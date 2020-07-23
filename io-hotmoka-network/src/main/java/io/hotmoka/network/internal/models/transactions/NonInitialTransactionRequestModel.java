package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

import java.math.BigInteger;

public abstract class NonInitialTransactionRequestModel extends TransactionModel {
    private String signature;
    private StorageReferenceModel caller;
    private BigInteger nonce;
    private String classpath;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public StorageReferenceModel getCaller() {
        return caller;
    }

    public void setCaller(StorageReferenceModel caller) {
        this.caller = caller;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }
}
