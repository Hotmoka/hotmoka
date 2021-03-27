package io.hotmoka.service.internal.services;

import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;

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