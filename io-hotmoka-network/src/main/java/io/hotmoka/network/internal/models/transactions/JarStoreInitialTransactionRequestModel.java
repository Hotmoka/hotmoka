package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageModel;

import java.util.List;

public class JarStoreInitialTransactionRequestModel {
    private String jar;
    private List<StorageModel> dependencies;

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public List<StorageModel> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<StorageModel> dependencies) {
        this.dependencies = dependencies;
    }
}
