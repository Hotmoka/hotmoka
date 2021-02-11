package io.hotmoka.service.internal.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.hotmoka.service.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.service.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.service.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.service.models.requests.RedGreenGameteCreationTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.StorageValueModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

@Service
public class AddServiceImpl extends AbstractService implements AddService {

	@Override
	public TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		return wrapExceptions(() -> new TransactionReferenceModel(getNode().addJarStoreInitialTransaction(request.toBean())));
	}

    @Override
    public StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addGameteCreationTransaction(request.toBean())));
    }

    @Override
    public StorageReferenceModel addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addRedGreenGameteCreationTransaction(request.toBean())));
    }

    @Override
    public ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request) {
        return wrapExceptions(() -> {
            getNode().addInitializationTransaction(request.toBean());
            return ResponseEntity.noContent().build();
        });
    }

    @Override
    public TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().addJarStoreTransaction(request.toBean())));
    }

    @Override
    public StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addConstructorCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> StorageValueModel.modelOfValueReturned(request, getNode().addInstanceMethodCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel addStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> StorageValueModel.modelOfValueReturned(request, getNode().addStaticMethodCallTransaction(request.toBean())));
    }
}