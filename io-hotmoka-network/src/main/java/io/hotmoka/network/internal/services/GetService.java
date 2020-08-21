package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.requests.TransactionRestRequestModel;
import io.hotmoka.network.models.responses.SignatureAlgorithmResponseModel;
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
    TransactionRestRequestModel<?> getRequest(TransactionReferenceModel reference);
	SignatureAlgorithmResponseModel getSignatureAlgorithmForRequests();
    TransactionRestResponseModel<?> getResponse(TransactionReferenceModel reference);
    TransactionRestResponseModel<?> getPolledResponse(TransactionReferenceModel reference);
}