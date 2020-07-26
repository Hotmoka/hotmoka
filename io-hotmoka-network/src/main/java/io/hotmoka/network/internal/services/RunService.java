package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.values.StorageValueModel;

public interface RunService {
    StorageValueModel runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel runStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}