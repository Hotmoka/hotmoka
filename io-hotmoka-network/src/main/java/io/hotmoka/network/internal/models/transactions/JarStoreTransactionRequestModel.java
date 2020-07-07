package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;
import java.util.List;

import io.hotmoka.network.internal.models.storage.StorageModel;

public class JarStoreTransactionRequestModel extends JarStoreInitialTransactionRequestModel {
    private String caller;
    private BigInteger callerProgressive;
    private BigInteger nonce;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;
    private List<StorageModel> dependencies;

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public BigInteger getCallerProgressive() {
        return callerProgressive;
    }

    public void setCallerProgressive(BigInteger callerProgressive) {
        this.callerProgressive = callerProgressive;
    }

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

    public List<StorageModel> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<StorageModel> dependencies) {
        this.dependencies = dependencies;
    }
}
