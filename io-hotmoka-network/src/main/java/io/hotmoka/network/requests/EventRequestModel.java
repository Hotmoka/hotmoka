package io.hotmoka.network.requests;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.values.StorageReferenceModel;

public class EventRequestModel {
    public StorageReferenceModel creator;
    public StorageReferenceModel event;

    public EventRequestModel() {}

    public EventRequestModel(StorageReference creator, StorageReference event) {
        this.creator = new StorageReferenceModel(creator);
        this.event = new StorageReferenceModel(event);
    }
}
