package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.values.StorageValueModel;

public interface RunService {
    StorageValueModel runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel runStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}