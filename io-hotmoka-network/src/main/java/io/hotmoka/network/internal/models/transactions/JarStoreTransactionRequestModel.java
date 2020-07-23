package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;
import java.util.List;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String signature;
    private String classpath;
    private StorageReferenceModel caller;
    private BigInteger nonce;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;
    private String jar;
    private List<StorageReferenceModel> dependencies;

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public List<StorageReferenceModel> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<StorageReferenceModel> dependencies) {
        this.dependencies = dependencies;
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

    public StorageReferenceModel getCaller() {
        return caller;
    }

    public void setCaller(StorageReferenceModel caller) {
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
