package io.hotmoka.service.internal.services;

import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.StorageValueModel;

public interface RunService {
    StorageValueModel runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel runStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}