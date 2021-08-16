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

package io.hotmoka.views;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseFailed;
import io.hotmoka.nodes.Node;

/**
 * A counter of the gas consumed for the execution of a set of requests.
 */
public class GasCounter {

	/**
	 * The total gas consumed.
	 */
	public final BigInteger total;

	/**
	 * The gas consumed for CPU.
	 */
	public final BigInteger forCPU;

	/**
	 * The gas consumed for RAM allocation.
	 */
	public final BigInteger forRAM;

	/**
	 * The gas consumed for storage.
	 */
	public final BigInteger forStorage;

	/**
	 * The gas consumed for penalty.
	 */
	public final BigInteger forPenalty;

	/**
	 * Creates the counter of the gas consumed for the execution of a set of requests.
	 * 
	 * @param node the node that executed the requests
	 * @param requests the requests
	 */
	public GasCounter(Node node, TransactionRequest<?>... requests) {
		TransactionReference[] references = Stream.of(requests).map(TransactionRequest::getReference).toArray(TransactionReference[]::new);
		BigInteger forPenalty = BigInteger.ZERO;
		BigInteger forCPU = BigInteger.ZERO;
		BigInteger forRAM = BigInteger.ZERO;
		BigInteger forStorage = BigInteger.ZERO;

		for (TransactionReference reference: references)
			try {
				TransactionResponse response = node.getResponse(reference);
				if (response instanceof NonInitialTransactionResponse) {
					NonInitialTransactionResponse responseWithGas = (NonInitialTransactionResponse) response;
					forCPU = forCPU.add(responseWithGas.gasConsumedForCPU);
					forRAM = forRAM.add(responseWithGas.gasConsumedForRAM);
					forStorage = forStorage.add(responseWithGas.gasConsumedForStorage);
					if (responseWithGas instanceof TransactionResponseFailed)
						forPenalty = forPenalty.add(((TransactionResponseFailed) responseWithGas).gasConsumedForPenalty());
				}
			}
			catch (TransactionRejectedException | NoSuchElementException e) {}

		this.total = forCPU.add(forRAM).add(forStorage).add(forPenalty);
		this.forCPU = forCPU;
		this.forRAM = forRAM;
		this.forStorage = forStorage;
		this.forPenalty = forPenalty;
	}
}