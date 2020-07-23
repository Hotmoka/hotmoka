package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

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
}