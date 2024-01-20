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

package io.hotmoka.beans.api.responses;

import io.hotmoka.annotations.Immutable;

/**
 * A response for a transaction that should call a method in blockchain.
 */
@Immutable
public interface MethodCallTransactionResponse extends CodeExecutionTransactionResponse {

	/**
	 * Determines if the called method was annotated as {@code @@SelfCharged}, hence the
	 * execution was charged to its receiver.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean getSelfCharged();
}