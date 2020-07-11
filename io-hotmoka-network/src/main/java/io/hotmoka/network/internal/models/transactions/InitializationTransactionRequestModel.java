package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public class InitializationTransactionRequestModel extends TransactionModel {
    private StorageReferenceModel manifest;

    public StorageReferenceModel getManifest() {
        return manifest;
    }

    public void setManifest(StorageReferenceModel manifest) {
        this.manifest = manifest;
    }
}
