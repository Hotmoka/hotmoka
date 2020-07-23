package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.storage.TransactionReferenceModel;

public interface PostService {
    TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel postStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}
