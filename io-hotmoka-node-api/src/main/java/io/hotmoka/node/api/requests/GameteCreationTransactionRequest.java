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

package io.hotmoka.node.api.requests;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * A request for creating an initial gamete, that is, an account of class
 * {@code io.takamaka.code.lang.Gamete} that holds the initial coins of the network.
 */
@Immutable
public interface GameteCreationTransactionRequest extends InitialTransactionRequest<GameteCreationTransactionResponse> {

	/**
	 * Yields the reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 * 
	 * @return the reference
	 */
	TransactionReference getClasspath();

	/**
	 * Yields the amount of coins provided to the gamete.
	 *
	 * @return the amount of coins provided to the gamete
	 */
	BigInteger getInitialAmount();

	/**
	 * Yields the amount of red coins provided to the gamete.
	 * 
	 * @return the amount of red coins provided to the gamete
	 */
	BigInteger getRedInitialAmount();

	/**
	 * Yields the Base64-encoded public key that will be assigned to the gamete.
	 * 
	 * @return the Base64-encoded public key that will be assigned to the gamete
	 */
	String getPublicKey();
}