package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public interface GetService {
    TransactionReferenceModel getTakamakaCode();
    StorageReferenceModel getManifest();
    StateModel getState(StorageReferenceModel request);
    ClassTagModel getClassTag(StorageReferenceModel request);
}