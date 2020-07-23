package io.hotmoka.network.internal.models.storage;

import io.hotmoka.beans.values.StorageValue;

public class StorageValueModel {
    private String value;

    public StorageValueModel() {}

    public StorageValueModel(StorageValue input) {
    	value = input.toString();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
