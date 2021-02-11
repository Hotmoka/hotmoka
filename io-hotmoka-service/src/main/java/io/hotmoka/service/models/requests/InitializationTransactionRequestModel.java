package io.hotmoka.service.models.requests;

import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

public class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    public StorageReferenceModel manifest;
    public TransactionReferenceModel classpath;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InitializationTransactionRequestModel(InitializationTransactionRequest request) {
    	this.manifest = new StorageReferenceModel(request.manifest);
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public InitializationTransactionRequestModel() {}

    public InitializationTransactionRequest toBean() {
    	return new InitializationTransactionRequest(classpath.toBean(), manifest.toBean());
    }
}