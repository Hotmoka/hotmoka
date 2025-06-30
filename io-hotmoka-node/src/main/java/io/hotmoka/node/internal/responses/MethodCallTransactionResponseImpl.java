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

package io.hotmoka.node.internal.responses;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A response for a transaction that should call a method in blockchain.
 */
@Immutable
public abstract class MethodCallTransactionResponseImpl extends CodeExecutionTransactionResponseImpl implements MethodCallTransactionResponse {

	/**
	 * Builds the transaction response.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param updates the updates resulting from the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> MethodCallTransactionResponseImpl(Update[] updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, onIllegalArgs);
	}

	protected final static StorageReference[] NO_REFERENCES = new StorageReference[0];
}