package io.hotmoka.network.internal.services;

import java.util.NoSuchElementException;

import io.hotmoka.network.models.responses.TransactionResponseModel;
import org.springframework.stereotype.Service;

import io.hotmoka.network.models.requests.TransactionRequestModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

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
	public TransactionRequestModel getRequestAt(TransactionReferenceModel reference) {
		return wrapExceptions(() -> TransactionRequestModel.from(getNode().getRequestAt(reference.toBean())));
	}

	@Override
	public String getSignatureAlgorithmForRequests() {
		// we yield the name of the static method of io.hotmoka.crypto.SignatureAlgorithm used for signing requests
		// if no such static method exists, we throw an exception
		return wrapExceptions(() -> {
			Class<?> clazz = getNode().getSignatureAlgorithmForRequests().getClass();
			if (clazz.getName().startsWith("io.hotmoka.crypto.internal."))
				return clazz.getSimpleName().toLowerCase();
			else
				throw new NoSuchElementException("cannot determine the signature algorithm for requests");
		});
	}

    @Override
    public TransactionResponseModel getResponseAt(TransactionReferenceModel reference) {
        return wrapExceptions(() -> TransactionResponseModel.from(getNode().getResponseAt(reference.toBean())));
    }

    @Override
    public TransactionResponseModel getPolledResponseAt(TransactionReferenceModel reference) {
        return wrapExceptions(() -> TransactionResponseModel.from(getNode().getPolledResponseAt(reference.toBean())));
    }
}