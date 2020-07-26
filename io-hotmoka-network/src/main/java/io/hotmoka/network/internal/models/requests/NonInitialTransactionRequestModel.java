package io.hotmoka.network.internal.models.requests;

import java.math.BigInteger;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.TransactionReferenceModel;

public abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
    private String signature;
    private StorageReferenceModel caller;
    private BigInteger nonce;
    private TransactionReferenceModel classpath;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;

    protected final TransactionReferenceModel getClasspath() {
        return classpath;
    }

    public void setClasspath(TransactionReferenceModel classpath) {
    	System.out.println("setting classpath to " + classpath);
        this.classpath = classpath;
    }

    protected final BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
    	System.out.println("setting nonce to " + nonce);
        this.nonce = nonce;
    }

    protected final StorageReferenceModel getCaller() {
        return caller;
    }

    public void setCaller(StorageReferenceModel caller) {
    	System.out.println("setting caller to " + caller);
        this.caller = caller;
    }

    protected final String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
    	System.out.println("setting signature to " + signature);
        this.signature = signature;
    }

    protected final String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
    	System.out.println("setting chain id to " + chainId);
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
