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

import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An object that helps with gas operations.
 */
public class GasHelperImpl implements GasHelper {
	private final Node node;
	private final InstanceMethodCallTransactionRequest request;

	/**
	 * Creates an object that helps with gas operations.
	 * 
	 * @param node the node whose gas is considered
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws CodeExecutionException if some transaction threw an exception
	 * @throws TransactionException if some transaction failed
	 * @throws TransactionRejectedException if some transaction was rejected
	 */
	public GasHelperImpl(Node node) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		StorageReference gasStation = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, BigInteger.valueOf(100_000), takamakaCode, MethodSignatures.GET_GAS_STATION, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAS_STATION + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_GAS_STATION, NodeException::new);
		this.request = TransactionRequests.instanceViewMethodCall
				(manifest, BigInteger.valueOf(100_000), takamakaCode, MethodSignatures.GET_GAS_PRICE, gasStation);
	}

	@Override
	public BigInteger getGasPrice() throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException {
		// this helps with testing, since otherwise previous tests might make the gas price explode for the subsequent tests
		if (node.getConfig().ignoresGasPrice())
			return BigInteger.ONE;

		return node.runInstanceMethodCallTransaction(request)
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAS_PRICE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_GAS_PRICE, NodeException::new);
	}

	@Override
	public BigInteger getSafeGasPrice() throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException {
		// we double the minimal price, to be sure that the transaction won't be rejected
		return BigInteger.valueOf(2L).multiply(getGasPrice()); // BigInteger.TWO crashes the Android client
	}
}