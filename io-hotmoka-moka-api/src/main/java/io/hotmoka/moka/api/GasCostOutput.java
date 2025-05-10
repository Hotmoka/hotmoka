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

package io.hotmoka.moka.api;

import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * The output of a command that reports the gas cost of a transaction in its output.
 */
@Immutable
public interface GasCostOutput {

	/**
	 * Yields the transaction that consumed the gas.
	 * 
	 * @return the transaction that consumed the gas
	 */
	TransactionReference getTransaction();

	/**
	 * Yields the gas cost of the transaction.
	 * 
	 * @return the gas cost of the transaction; this is missing if the transaction has just been posted
	 *         rather than added, or if it has failed
	 */
	Optional<GasCost> getGasCost();

	/**
	 * Yields the error message of the transaction.
	 * 
	 * @return the error message of the transaction; this is missing if the transaction has just been posted
	 *         rather than added, or if it was successful 
	 */
	Optional<String> getErrorMessage();
}