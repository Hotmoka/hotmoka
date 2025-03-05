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

package io.hotmoka.node.api;

import io.hotmoka.node.api.nodes.ConsensusConfig;

/**
 * An exception raised when a transaction cannot even be started. Typically,
 * this means that the payer of the transaction cannot be identified or it has
 * not enough money to pay for a failed transaction or that its signature is invalid.
 */
@SuppressWarnings("serial")
public class TransactionRejectedException extends Exception {

	/**
	 * Builds an exception with the given message.
	 * 
	 * @param message the message
	 */
	public TransactionRejectedException(String message) {
		super(message);
	}

	/**
	 * Builds an exception with the given message.
	 * 
	 * @param message the message
	 * @param consensus the consensus of the node generating this message; this is used to trim the
	 *                  message to the maximal length allowed in the consensus
	 */
	public TransactionRejectedException(String message, ConsensusConfig<?,?> consensus) {
		super(trim(message, consensus.getMaxErrorLength()));
	}

	/**
	 * Builds an exception with the given cause.
	 * 
	 * @param cause the cause
	 * @param consensus the consensus of the node generating this message; this is used to trim the
	 *                  message to the maximal length allowed in the consensus
	 */
	public TransactionRejectedException(Throwable cause, ConsensusConfig<?,?> consensus) {
		super(trim(cause.getClass().getName() + messageOf(cause), consensus.getMaxErrorLength()), cause); // TODO: getMaxErrorLength should be moved to LocalConfig
	}

	private static String trim(String s, int maxLength) {
		return s.length() <= maxLength ? s : s.substring(maxLength);
	}

	private static String messageOf(Throwable cause) {
		return cause.getMessage() == null ? "" : (": " + cause.getMessage());
	}
}