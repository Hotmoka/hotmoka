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
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * An object that helps with gas operations.
 */
public class GasHelperImpl implements GasHelper {
	private final Node node;
	private final StorageReference gasStation;
	private final TransactionReference takamakaCode;
	private final StorageReference manifest;

	/**
	 * Creates an object that helps with gas operations.
	 * 
	 * @param node the node whose gas is considered
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 */
	public GasHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		this.node = node;
		this.takamakaCode = node.getTakamakaCode();
		this.manifest = node.getManifest();
		var _100_000 = BigInteger.valueOf(100_000);

		this.gasStation = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_STATION, manifest));
	}

	@Override
	public BigInteger getGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		var _100_000 = BigInteger.valueOf(100_000);

		boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))).getValue();

		// this helps with testing, since otherwise previous tests might make the gas price explode for the subsequent tests
		if (ignoresGasPrice)
			return BigInteger.ONE;

		// we double the minimal price, to be sure that the transaction won't be rejected
		return ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_PRICE, gasStation))).getValue();
	}

	@Override
	public BigInteger getSafeGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return BigInteger.TWO.multiply(getGasPrice());
	}
}