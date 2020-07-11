package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.ClassTagModel;
import io.hotmoka.network.internal.models.StateModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public interface NodeGetService {
    StorageReferenceModel getTakamakaCode();
    StorageReferenceModel getManifest();
    StateModel getState(StorageReferenceModel request);
    ClassTagModel getClassTag(StorageReferenceModel request);
}
