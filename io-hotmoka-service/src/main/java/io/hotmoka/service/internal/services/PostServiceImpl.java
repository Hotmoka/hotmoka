package io.hotmoka.service.internal.services;

import io.hotmoka.service.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;
import org.springframework.stereotype.Service;

import io.hotmoka.service.models.requests.JarStoreTransactionRequestModel;

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