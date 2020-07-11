package io.hotmoka.network.internal.models.storage;

public class ValueModel extends StorageValueModel {
    private String type;
    private StorageReferenceModel reference;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public StorageReferenceModel getReference() {
        return reference;
    }

    public void setReference(StorageReferenceModel reference) {
        this.reference = reference;
    }
}
