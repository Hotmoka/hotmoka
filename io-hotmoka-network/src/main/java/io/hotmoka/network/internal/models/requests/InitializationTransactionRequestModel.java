package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.network.internal.models.values.StorageReferenceModel;
import io.hotmoka.network.internal.models.values.TransactionReferenceModel;

public class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    private StorageReferenceModel manifest;
    private TransactionReferenceModel classpath;

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