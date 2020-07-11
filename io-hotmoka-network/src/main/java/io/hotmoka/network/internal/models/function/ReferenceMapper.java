package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public class ReferenceMapper implements Mapper<TransactionReference, StorageReferenceModel> {

    @Override
    public StorageReferenceModel map(TransactionReference input) throws Exception {
        StorageReferenceModel storageModel = new StorageReferenceModel();
        storageModel.setHash(input.getHash());
        return storageModel;
    }
}
