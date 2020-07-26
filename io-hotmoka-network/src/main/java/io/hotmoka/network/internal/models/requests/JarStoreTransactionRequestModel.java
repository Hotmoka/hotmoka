package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.network.internal.models.storage.TransactionReferenceModel;
import io.hotmoka.network.internal.util.StorageResolver;

public class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String jar;
    private List<TransactionReferenceModel> dependencies;

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public void setDependencies(List<TransactionReferenceModel> dependencies) {
        this.dependencies = dependencies;
    }

    public JarStoreTransactionRequest toBean() {
    	return new JarStoreTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            decodeBase64(jar),
            StorageResolver.resolveJarDependencies(dependencies));
    }
}