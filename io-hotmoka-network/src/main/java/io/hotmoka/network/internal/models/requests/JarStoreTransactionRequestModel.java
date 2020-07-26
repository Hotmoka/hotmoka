package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

public class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String jar;
    private List<StorageReferenceModel> dependencies;

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public void setDependencies(List<StorageReferenceModel> dependencies) {
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
            JSONTransactionReference.fromJSON(getClasspath()),
            decodeBase64(jar),
            StorageResolver.resolveJarDependencies(dependencies));
    }
}