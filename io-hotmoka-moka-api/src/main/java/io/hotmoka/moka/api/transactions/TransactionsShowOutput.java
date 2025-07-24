/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.api.transactions;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;

/**
 * The output of the {@code moka transactions show} command.
 */
@Immutable
public interface TransactionsShowOutput {

	/**
	 * Yields the request of the transaction.
	 * 
	 * @return the request of the transaction
	 */
	TransactionRequest<?> getRequest();

	/**
	 * Yields the response of the transaction.
	 * 
	 * @return the response of the transaction
	 */
	TransactionResponse getResponse();
}