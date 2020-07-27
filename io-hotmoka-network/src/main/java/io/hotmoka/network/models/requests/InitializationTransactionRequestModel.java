package io.hotmoka.network.models.requests;

import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    private StorageReferenceModel manifest;
    private TransactionReferenceModel classpath;

    /**
     * For Spring.
     */
    public InitializationTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InitializationTransactionRequestModel(InitializationTransactionRequest request) {
    	this.manifest = new StorageReferenceModel(request.manifest);
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public void setClasspath(TransactionReferenceModel classpath) {
        this.classpath = classpath;
    }

    public void setManifest(StorageReferenceModel manifest) {
        this.manifest = manifest;
    }

    public InitializationTransactionRequest toBean() {
    	return new InitializationTransactionRequest(classpath.toBean(), manifest.toBean());
    }
}