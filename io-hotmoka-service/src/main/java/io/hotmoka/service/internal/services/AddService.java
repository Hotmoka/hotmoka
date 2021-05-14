/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.service.internal.services;

import io.hotmoka.network.requests.*;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;
import io.hotmoka.network.values.TransactionReferenceModel;

import org.springframework.http.ResponseEntity;

public interface AddService {
    TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request);
    StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request);
    TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel addStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}