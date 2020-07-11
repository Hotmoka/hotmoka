package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;

public interface NodeRunService {
    StorageValueModel runInstanceMethodCallTransaction(MethodCallTransactionRequestModel request);
    StorageValueModel runStaticMethodCallTransaction(MethodCallTransactionRequestModel request);
}
