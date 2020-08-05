package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.requests.TransactionRestRequestModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public interface GetService {
    TransactionReferenceModel getTakamakaCode();
    StorageReferenceModel getManifest();
    StateModel getState(StorageReferenceModel request);
    ClassTagModel getClassTag(StorageReferenceModel request);
    TransactionRestRequestModel<?> getRequestAt(TransactionReferenceModel reference);
	String getSignatureAlgorithmForRequests();
    TransactionRestResponseModel<?> getResponseAt(TransactionReferenceModel reference);
    TransactionRestResponseModel<?> getPolledResponseAt(TransactionReferenceModel reference);
}