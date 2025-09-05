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

package io.hotmoka.node;

import java.io.IOException;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.Transaction;
import io.hotmoka.node.internal.transactions.TransactionImpl;

/**
 * Providers of transactions.
 */
public abstract class Transactions {

	private Transactions() {}

	/**
	 * Yields a transaction with the given request and response.
	 * 
	 * @param request the request
	 * @param response the response
	 * @return the transaction
	 */
	public static Transaction of(TransactionRequest<?> request, TransactionResponse response) {
		return new TransactionImpl(request, response);
	}

	/**
	 * Yields a transaction unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction
	 * @throws IOException if the transaction could not be unmarshalled
     */
	public static Transaction from(UnmarshallingContext context) throws IOException {
		return new TransactionImpl(context);
	}
}