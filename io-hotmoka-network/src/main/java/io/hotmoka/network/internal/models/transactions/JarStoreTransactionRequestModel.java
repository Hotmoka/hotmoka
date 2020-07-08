package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;
import java.util.List;

import io.hotmoka.network.internal.models.storage.StorageModel;

public class JarStoreTransactionRequestModel extends JarStoreInitialTransactionRequestModel {
    private String signature;
    private String classpath;
    private StorageModel caller;
    private BigInteger nonce;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;


    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
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

    public StorageModel getCaller() {
        return caller;
    }

    public void setCaller(StorageModel caller) {
        this.caller = caller;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
