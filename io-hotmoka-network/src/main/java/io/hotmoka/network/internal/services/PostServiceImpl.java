package io.hotmoka.network.internal.services;

import org.springframework.stereotype.Service;

import io.hotmoka.network.internal.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.storage.TransactionReferenceModel;

@Service
public class PostServiceImpl extends AbstractService implements PostService {

    @Override
    public TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request) {
    	// TODO: this is an add...
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().postJarStoreTransaction(request.toBean()).get()));
    }

    @Override
    public StorageReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
    	// TODO
        return wrapExceptions(() -> new StorageReferenceModel(getNode().postConstructorCallTransaction(request.toBean()).get()));
    }

    @Override
    public StorageValueModel postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
    	// TODO
        return wrapExceptions(() -> new StorageValueModel(getNode().postInstanceMethodCallTransaction(request.toBean()).get()));
    }

    @Override
    public StorageValueModel postStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
    	// TODO
        return wrapExceptions(() -> new StorageValueModel(getNode().postStaticMethodCallTransaction(request.toBean()).get()));
    }
}
