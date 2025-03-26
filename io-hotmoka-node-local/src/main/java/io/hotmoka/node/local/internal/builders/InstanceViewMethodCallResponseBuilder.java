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

package io.hotmoka.node.local.internal.builders;

import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * The builder of the response for a transaction that executes an instance method of Takamaka code
 * annotated as {@linkplain io.takamaka.code.lang.View}.
 */
public class InstanceViewMethodCallResponseBuilder extends InstanceMethodCallResponseBuilder {

	/**
	 * Creates the builder of the response. 
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public InstanceViewMethodCallResponseBuilder(TransactionReference reference, InstanceMethodCallTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	protected boolean isView() {
		return true;
	}
}