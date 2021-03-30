package io.hotmoka.service.internal.services;

import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.values.StorageValueModel;

public interface RunService {
    StorageValueModel runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel runStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}