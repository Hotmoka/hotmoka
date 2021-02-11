package io.hotmoka.service.internal.services;

import io.hotmoka.service.models.requests.TransactionRestRequestModel;
import io.hotmoka.service.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.service.models.responses.TransactionRestResponseModel;
import io.hotmoka.service.models.updates.ClassTagModel;
import io.hotmoka.service.models.updates.StateModel;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

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
		// we yield the name of the static method of io.hotmoka.crypto.SignatureAlgorithm used for signing requests
		// if no such static method exists, we throw an exception
		return wrapExceptions(() -> {
			Class<?> clazz = getNode().getSignatureAlgorithmForRequests().getClass();
			if (clazz.getName().startsWith("io.hotmoka.crypto.internal."))
				return new SignatureAlgorithmResponseModel(clazz.getSimpleName().toLowerCase());
			else
				throw new NoSuchElementException("cannot determine the signature algorithm for requests");
		});
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