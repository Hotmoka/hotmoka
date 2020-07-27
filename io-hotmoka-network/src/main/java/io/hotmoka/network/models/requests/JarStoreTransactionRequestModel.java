package io.hotmoka.network.models.requests;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * The model of a jar store transaction request.
 */
public class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String jar;
    private List<TransactionReferenceModel> dependencies;

    /**
     * For Spring.
     */
    public JarStoreTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public JarStoreTransactionRequestModel(JarStoreTransactionRequest request) {
    	this.jar = Base64.getEncoder().encodeToString(request.getJar());
    	this.dependencies = request.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
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
            dependencies.stream().map(TransactionReferenceModel::toBean).toArray(TransactionReference[]::new));
    }
}