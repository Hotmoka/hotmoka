package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.storage.StorageValueModel;

public class StorageValueMapper implements Mapper<StorageValue, StorageValueModel> {

    @Override
    public StorageValueModel map(StorageValue input) {
        StorageValueModel storageValueModel = new StorageValueModel();
        storageValueModel.setValue(input.toString());
        return storageValueModel;
    }
}
