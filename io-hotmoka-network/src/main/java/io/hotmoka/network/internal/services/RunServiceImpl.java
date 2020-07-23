package io.hotmoka.network.internal.services;

import org.springframework.stereotype.Service;

import io.hotmoka.network.internal.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;

@Service
public class RunServiceImpl extends AbstractService implements RunService {

    @Override
    public StorageValueModel runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageValueModel(getNode().runInstanceMethodCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel runStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageValueModel(getNode().runStaticMethodCallTransaction(request.toBean())));
    }
}