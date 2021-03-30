package io.hotmoka.service.internal.services;

import io.hotmoka.network.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.values.TransactionReferenceModel;

public interface PostService {
	// these yield the transaction that has been started, but possibly not yet concluded;
	// one can later poll the result of the transaction, if needed
    TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request);
    TransactionReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    TransactionReferenceModel postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    TransactionReferenceModel postStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}