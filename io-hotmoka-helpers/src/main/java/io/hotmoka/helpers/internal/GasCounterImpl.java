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

package io.hotmoka.helpers.internal;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.helpers.api.GasCounter;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.FailedTransactionResponse;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * Implementation of a counter of the gas consumed for the execution of a set of requests.
 */
public class GasCounterImpl implements GasCounter {

	/**
	 * The total gas consumed.
	 */
	private final BigInteger total;

	/**
	 * The gas consumed for CPU.
	 */
	private final BigInteger forCPU;

	/**
	 * The gas consumed for RAM allocation.
	 */
	private final BigInteger forRAM;

	/**
	 * The gas consumed for storage.
	 */
	private final BigInteger forStorage;

	/**
	 * The gas consumed for penalty.
	 */
	private final BigInteger forPenalty;

	/**
	 * Creates the counter of the gas consumed for the execution of a set of requests.
	 * 
	 * @param node the node that executed the requests
	 * @param requests the requests whose consumed gas must be computed
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request has not been processed by the node
	 * @throws TransactionRejectedException if some request has been rejected by the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 */
	public GasCounterImpl(Node node, TransactionRequest<?>... requests) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, UnknownReferenceException {
		Hasher<TransactionRequest<?>> hasher;

		try {
			hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
		}
		catch (NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}

		// the hashing algorithm generates hashes of the correct length, hence no checked exception
		var references = Stream.of(requests).map(hasher::hash).map(hash -> TransactionReferences.of(hash, IllegalArgumentException::new)).toArray(TransactionReference[]::new);
		var forPenalty = BigInteger.ZERO;
		var forCPU = BigInteger.ZERO;
		var forRAM = BigInteger.ZERO;
		var forStorage = BigInteger.ZERO;
	
		for (var reference: references)
			if (node.getResponse(reference) instanceof NonInitialTransactionResponse responseWithGas) {
				forCPU = forCPU.add(responseWithGas.getGasConsumedForCPU());
				forRAM = forRAM.add(responseWithGas.getGasConsumedForRAM());
				forStorage = forStorage.add(responseWithGas.getGasConsumedForStorage());
				if (responseWithGas instanceof FailedTransactionResponse ftr)
					forPenalty = forPenalty.add(ftr.getGasConsumedForPenalty());
			}
	
		this.total = forCPU.add(forRAM).add(forStorage).add(forPenalty);
		this.forCPU = forCPU;
		this.forRAM = forRAM;
		this.forStorage = forStorage;
		this.forPenalty = forPenalty;
	}

	@Override
	public BigInteger total() {
		return total;
	}

	@Override
	public BigInteger forCPU() {
		return forCPU;
	}

	@Override
	public BigInteger forRAM() {
		return forRAM;
	}

	@Override
	public BigInteger forStorage() {
		return forStorage;
	}

	@Override
	public BigInteger forPenalty() {
		return forPenalty;
	}
}