package io.hotmoka.service.internal.services;

import io.hotmoka.network.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.values.TransactionReferenceModel;

import org.springframework.stereotype.Service;

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