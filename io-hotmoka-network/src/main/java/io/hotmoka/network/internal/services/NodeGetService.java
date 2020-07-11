package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.updates.ClassTagModel;
import io.hotmoka.network.internal.models.updates.StateModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;

public interface NodeGetService {
    TransactionReferenceModel getTakamakaCode();
    StorageReferenceModel getManifest();
    StateModel getState(StorageReferenceModel request);
    ClassTagModel getClassTag(StorageReferenceModel request);
}
