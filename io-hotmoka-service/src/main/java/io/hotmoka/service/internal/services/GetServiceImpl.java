package io.hotmoka.service.internal.services;

import org.springframework.stereotype.Service;

import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;

@Service
public class GetServiceImpl extends AbstractService implements GetService {

    @Override
    public TransactionReferenceModel getTakamakaCode() {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().getTakamakaCode()));
    }

    @Override
    public StorageReferenceModel getManifest() {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().getManifest()));
    }

    @Override
    public StateModel getState(StorageReferenceModel request) {
        return wrapExceptions(() -> new StateModel(getNode().getState(request.toBean())));
    }

    @Override
    public ClassTagModel getClassTag(StorageReferenceModel request) {
        return wrapExceptions(() -> new ClassTagModel(getNode().getClassTag(request.toBean())));
    }

	@Override
	public TransactionRestRequestModel<?> getRequest(TransactionReferenceModel reference) {
		return wrapExceptions(() -> TransactionRestRequestModel.from(getNode().getRequest(reference.toBean())));
	}

	@Override
	public SignatureAlgorithmResponseModel getSignatureAlgorithmForRequests() {
		return wrapExceptions(() -> new SignatureAlgorithmResponseModel(getNode().getSignatureAlgorithmForRequests().toLowerCase()));
	}

    @Override
    public TransactionRestResponseModel<?> getResponse(TransactionReferenceModel reference) {
        return wrapExceptions(() -> TransactionRestResponseModel.from(getNode().getResponse(reference.toBean())));
    }

    @Override
    public TransactionRestResponseModel<?> getPolledResponse(TransactionReferenceModel reference) {
        return wrapExceptions(() -> TransactionRestResponseModel.from(getNode().getPolledResponse(reference.toBean())));
    }
}