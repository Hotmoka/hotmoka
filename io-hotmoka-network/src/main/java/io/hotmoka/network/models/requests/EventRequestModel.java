package io.hotmoka.network.models.requests;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.models.values.StorageReferenceModel;

public class EventRequestModel {
    public StorageReferenceModel key;
    public StorageReferenceModel event;

    public EventRequestModel() {}

    public EventRequestModel(StorageReference key, StorageReference event) {
        this.key = new StorageReferenceModel(key);
        this.event = new StorageReferenceModel(event);
    }
}
