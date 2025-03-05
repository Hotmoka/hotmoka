/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.api.responses;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A response for a successful transaction that calls a method
 * in the store of a node. The method has been called without problems and
 * without generating exceptions. The method does not return {@code void}.
 */
@Immutable
public interface NonVoidMethodCallTransactionSuccessfulResponse extends MethodCallTransactionResponse, TransactionResponseWithEvents {

	/**
	 * Yields the return value of the method.
	 * 
	 * @return the return value of the method
	 */
	StorageValue getResult();
}