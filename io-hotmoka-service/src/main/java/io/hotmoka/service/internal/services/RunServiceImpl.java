package io.hotmoka.service.internal.services;

import org.springframework.stereotype.Service;

import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.values.StorageValueModel;

@Service
public class RunServiceImpl extends AbstractService implements RunService {

    @Override
    public StorageValueModel runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> StorageValueModel.modelOfValueReturned(request, getNode().runInstanceMethodCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel runStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
    	return wrapExceptions(() -> StorageValueModel.modelOfValueReturned(request, getNode().runStaticMethodCallTransaction(request.toBean())));
    }
}