package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public class StorageReferenceMapper implements Mapper<StorageReference, StorageReferenceModel> {

    @Override
    public StorageReferenceModel map(StorageReference input) {
        StorageReferenceModel storageModel = new StorageReferenceModel();
        storageModel.setTransaction(input.transaction.getHash());
        storageModel.setProgressive(input.progressive);
        return storageModel;
    }
}
