package io.hotmoka.network.internal.services;

import org.springframework.stereotype.Service;

import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@Service
public class PostServiceImpl extends AbstractService implements PostService {

    @Override
    public TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().postJarStoreTransaction(request.toBean()).getReferenceOfRequest()));
    }

    @Override
    public TransactionReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().postConstructorCallTransaction(request.toBean()).getReferenceOfRequest()));
    }

    @Override
    public TransactionReferenceModel postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().postInstanceMethodCallTransaction(request.toBean()).getReferenceOfRequest()));
    }

    @Override
    public TransactionReferenceModel postStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().postStaticMethodCallTransaction(request.toBean()).getReferenceOfRequest()));
    }
}