package io.hotmoka.service.internal.services;

import io.hotmoka.service.models.requests.TransactionRestRequestModel;
import io.hotmoka.service.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.service.models.responses.TransactionRestResponseModel;
import io.hotmoka.service.models.updates.ClassTagModel;
import io.hotmoka.service.models.updates.StateModel;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

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