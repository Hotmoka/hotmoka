package io.hotmoka.network.internal.models.storage;

public class StorageValueModel extends StorageModel {
    private Object value;
    private String type;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
