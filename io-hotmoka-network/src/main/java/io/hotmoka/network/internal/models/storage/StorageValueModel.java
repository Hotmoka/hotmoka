package io.hotmoka.network.internal.models.storage;

public class StorageValueModel {
    private String type;
    private String value;
    private StorageModel reference;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public StorageModel getReference() {
        return reference;
    }

    public void setReference(StorageModel reference) {
        this.reference = reference;
    }
}
