/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.service.internal.services;

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