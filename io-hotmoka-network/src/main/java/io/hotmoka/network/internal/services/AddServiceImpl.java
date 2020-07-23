package io.hotmoka.network.internal.services;

import java.util.Base64;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.network.exception.GenericException;
import io.hotmoka.network.internal.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.RGGameteCreationTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.storage.TransactionReferenceModel;
import io.hotmoka.network.internal.util.StorageResolver;

@Service
public class AddServiceImpl extends AbstractService implements AddService {

	@Override
	public TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		return wrapExceptions(() -> {
		    if (request.getJar() == null)
		        throw new GenericException("Transaction rejected: Jar missing");

            byte[] jar = Base64.getDecoder().decode(request.getJar());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());

            return new TransactionReferenceModel
            	(getNode().addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies)));
		});
	}

    @Override
    public StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addGameteCreationTransaction(request.toBean())));
    }

    @Override
    public StorageReferenceModel addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addRedGreenGameteCreationTransaction(request.toBean())));
    }

    @Override
    public ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request) {
        return wrapExceptions(() -> {
            getNode().addInitializationTransaction(request.toBean());
            return noContentResponse();
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
        return wrapExceptions(() -> new StorageValueModel(getNode().addInstanceMethodCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel addStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageValueModel(getNode().addStaticMethodCallTransaction(request.toBean())));
    }
}