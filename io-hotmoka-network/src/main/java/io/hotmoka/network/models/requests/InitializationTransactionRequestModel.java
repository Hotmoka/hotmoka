package io.hotmoka.network.models.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@Immutable
public class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    public final StorageReferenceModel manifest;
    public final TransactionReferenceModel classpath;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InitializationTransactionRequestModel(InitializationTransactionRequest request) {
    	this.manifest = new StorageReferenceModel(request.manifest);
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public InitializationTransactionRequest toBean() {
    	return new InitializationTransactionRequest(classpath.toBean(), manifest.toBean());
    }
}