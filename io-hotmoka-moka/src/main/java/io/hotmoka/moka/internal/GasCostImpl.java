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

package io.hotmoka.moka.internal;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.internal.json.GasCostJson;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.responses.FailedTransactionResponse;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of the gas cost incurred for some execution.
 */
@Immutable
public class GasCostImpl implements GasCost {

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
	 * The price of a unit of gas used for the execution.
	 */
	private final BigInteger price;

	/**
	 * Creates the gas cost incurred for the already occurred execution of a set of requests, identified by their references.
	 * 
	 * @param node the node that executed the requests
	 * @param references the references of the requests whose consumed gas must be computed
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request transaction cannot be found in the store of the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 */
	public GasCostImpl(Node node, BigInteger price, TransactionReference... references) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		this.price = price;

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
	
		this.forCPU = forCPU;
		this.forRAM = forRAM;
		this.forStorage = forStorage;
		this.forPenalty = forPenalty;
	}

	/**
	 * Creates the gas cost from their given JSON representation.
	 * 
	 * @param gasCostJson the JSON representation
	 * @throws InconsistentJsonException if the JSON representation is inconsistent
	 */
	public GasCostImpl(GasCostJson gasCostJson) throws InconsistentJsonException {
		if ((this.forCPU = Objects.requireNonNull(gasCostJson.getForCPU(), "forCPU cannot be null", InconsistentJsonException::new)).signum() < 0)
			throw new InconsistentJsonException("forCPU cannot be negative");

		if ((this.forRAM = Objects.requireNonNull(gasCostJson.getForRAM(), "forRAM cannot be null", InconsistentJsonException::new)).signum() < 0)
			throw new InconsistentJsonException("forRAM cannot be negative");
		
		if ((this.forStorage = Objects.requireNonNull(gasCostJson.getForStorage(), "forStorage cannot be null", InconsistentJsonException::new)).signum() < 0)
			throw new InconsistentJsonException("forStorage cannot be negative");

		if ((this.forPenalty = Objects.requireNonNull(gasCostJson.getForPenalty(), "forPenalty cannot be null", InconsistentJsonException::new)).signum() < 0)
			throw new InconsistentJsonException("forPenalty cannot be negative");

		if ((this.price = Objects.requireNonNull(gasCostJson.getPrice(), "price cannot be null", InconsistentJsonException::new)).signum() < 0)
			throw new InconsistentJsonException("price cannot be negative");
	}

	@Override
	public BigInteger getPrice() {
		return price;
	}

	@Override
	public BigInteger getForCPU() {
		return forCPU;
	}

	@Override
	public BigInteger getForRAM() {
		return forRAM;
	}

	@Override
	public BigInteger getForStorage() {
		return forStorage;
	}

	@Override
	public BigInteger getForPenalty() {
		return forPenalty;
	}

	@Override
	public void toString(StringBuilder sb) {
		sb.append("Gas consumption:\n");
		BigInteger totalGasConsumed = forCPU.add(forRAM).add(forStorage).add(forPenalty);
		sb.append(" * total: " + totalGasConsumed + "\n");
		sb.append("   * for CPU: " + forCPU + "\n");
		sb.append("   * for RAM: " + forRAM + "\n");
		sb.append("   * for storage: " + forStorage + "\n");
		sb.append("   * for penalty: " + forPenalty + "\n");
		sb.append(" * price per unit: " + panas(price) + "\n");
		sb.append(" * total price: " + panas(price.multiply(totalGasConsumed)) + "\n");
	}

	private static String panas(BigInteger cost) {
		return cost.equals(BigInteger.ONE) ? "1 pana" : (cost + " panas");
	}
}