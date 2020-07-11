package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public class StorageReferenceMapper implements Mapper<StorageReference, StorageReferenceModel> {

    @Override
    public StorageReferenceModel map(io.hotmoka.beans.values.StorageReference input) throws Exception {
        StorageReferenceModel storageModel = new StorageReferenceModel();
        storageModel.setHash(input.transaction.getHash());
        storageModel.setProgressive(input.progressive);
        return storageModel;
    }
}
