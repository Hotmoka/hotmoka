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

package io.hotmoka.network.responses;

import io.hotmoka.beans.responses.InitializationTransactionResponse;

/**
 * The model of a response for a transaction that initializes a node.
 * After that, no more initial transactions can be executed.
 */
public class InitializationTransactionResponseModel extends TransactionResponseModel {

    public InitializationTransactionResponseModel() {}

    public InitializationTransactionResponse toBean() {
        return new InitializationTransactionResponse();
    }
}