package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;

public interface NodePostService {
    TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel postInstanceMethodCallTransaction(MethodCallTransactionRequestModel request);
    StorageValueModel postStaticMethodCallTransaction(MethodCallTransactionRequestModel request);
}
