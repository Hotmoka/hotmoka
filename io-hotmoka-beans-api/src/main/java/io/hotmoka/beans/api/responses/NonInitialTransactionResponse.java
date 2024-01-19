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

package io.hotmoka.beans.api.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.updates.Update;

/**
 * A response for a non-initial transaction.
 */
@Immutable
public interface NonInitialTransactionResponse extends TransactionResponseWithUpdates {

	/**
	 * Yields the updates resulting from the execution of the transaction.
	 * 
	 * @return the updates
	 */
	Stream<Update> getUpdates();

	/**
	 * Yields the amount of gas consumed by the transaction for CPU execution.
	 * 
	 * @return the amount of gas consumed by the transaction for CPU execution
	 */
	BigInteger getGasConsumedForCPU();

	/**
	 * Yields the amount of gas consumed by the transaction for RAM allocation.
	 * 
	 * @return the amount of gas consumed by the transaction for RAM allocation
	 */
	BigInteger getGasConsumedForRAM();

	/**
	 * Yields the amount of gas consumed by the transaction for storage consumption.
	 * 
	 * @return the amount of gas consumed by the transaction for storage consumption
	 */
	BigInteger getGasConsumedForStorage();
}