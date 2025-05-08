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

package io.hotmoka.moka.api.jars;

import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * The output of the {@code moka jars install} command.
 */
@Immutable
public interface JarsInstallOutput {

	/**
	 * Yields the reference of the installed jar, if any.
	 * 
	 * @return the reference of the installed jar; this is missing if the transaction has just been posted
	 *         rather than added, or if the transaction failed
	 */
	Optional<TransactionReference> getJar();

	/**
	 * Yields the transaction that installed the jar.
	 * 
	 * @return the transaction that installed the jar
	 */
	TransactionReference getTransaction();

	/**
	 * Yields the gas cost of the install transaction.
	 * 
	 * @return the gas cost of the install transaction; this is missing if the transaction has just been posted
	 *         rather than added, or if it has been rejected
	 */
	Optional<GasCost> getGasCost();

	/**
	 * Yields the error message of the install transaction.
	 * 
	 * @return the error message of the install transaction; this is missing if the transaction has just been posted
	 *         rather than added, or if it was successful, or if it was rejected 
	 */
	Optional<String> getErrorMessage();
}