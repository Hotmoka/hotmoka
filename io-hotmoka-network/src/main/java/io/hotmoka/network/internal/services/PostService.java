package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public interface PostService {
    TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel postStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}
