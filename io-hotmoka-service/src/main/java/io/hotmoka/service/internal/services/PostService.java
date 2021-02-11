package io.hotmoka.service.internal.services;

import io.hotmoka.service.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

public interface PostService {
	// these yield the transaction that has been started, but possibly not yet concluded;
	// one can later poll the result of the transaction, if needed
    TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request);
    TransactionReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    TransactionReferenceModel postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    TransactionReferenceModel postStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}