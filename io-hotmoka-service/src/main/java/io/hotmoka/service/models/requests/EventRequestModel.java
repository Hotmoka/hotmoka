package io.hotmoka.service.models.requests;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.service.models.values.StorageReferenceModel;

public class EventRequestModel {
    public StorageReferenceModel creator;
    public StorageReferenceModel event;

    public EventRequestModel() {}

    public EventRequestModel(StorageReference creator, StorageReference event) {
        this.creator = new StorageReferenceModel(creator);
        this.event = new StorageReferenceModel(event);
    }
}
