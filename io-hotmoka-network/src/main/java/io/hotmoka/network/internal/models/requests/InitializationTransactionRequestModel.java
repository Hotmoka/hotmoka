package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.json.JSONTransactionReference;

public class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    private StorageReferenceModel manifest;
    private String classpath;

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public StorageReferenceModel getManifest() {
        return manifest;
    }

    public void setManifest(StorageReferenceModel manifest) {
        this.manifest = manifest;
    }

    public InitializationTransactionRequest toBean() {
    	return new InitializationTransactionRequest(JSONTransactionReference.fromJSON(classpath), manifest.toBean());
    }
}