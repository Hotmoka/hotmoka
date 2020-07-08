package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageModel;

public class InitializationTransactionRequestModel extends TransactionModel {
    private StorageModel manifest;

    public StorageModel getManifest() {
        return manifest;
    }

    public void setManifest(StorageModel manifest) {
        this.manifest = manifest;
    }
}
