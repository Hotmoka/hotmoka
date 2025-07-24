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

package io.hotmoka.moka.internal.json;

import io.hotmoka.moka.api.transactions.TransactionsShowOutput;
import io.hotmoka.moka.internal.transactions.Show;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka transactions show} command.
 */
public abstract class TransactionsShowOutputJson implements JsonRepresentation<TransactionsShowOutput> {
	private final TransactionRequests.Json request;
	private final TransactionResponses.Json response;

	protected TransactionsShowOutputJson(TransactionsShowOutput output) {
		this.request = new TransactionRequests.Json(output.getRequest());
		this.response = new TransactionResponses.Json(output.getResponse());
	}

	public TransactionRequests.Json getRequest() {
		return request;
	}

	public TransactionResponses.Json getResponse() {
		return response;
	}

	@Override
	public TransactionsShowOutput unmap() throws InconsistentJsonException {
		return new Show.Output(this);
	}
}