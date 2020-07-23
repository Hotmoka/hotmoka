package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

import java.util.List;

public class JarStoreInitialTransactionRequestModel extends InitialTransactionRequestModel {
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
}