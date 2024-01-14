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

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.node.api.Node;

/**
 * An object that helps with gas operations.
 */
public class GasHelperImpl implements GasHelper {
	private final Node node;
	private final StorageReference gasStation;

	/**
	 * Creates an object that helps with gas operations.
	 * 
	 * @param node the node whose gas is considered
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 */
	public GasHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;

		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		var _100_000 = BigInteger.valueOf(100_000);

		this.gasStation = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAS_STATION, manifest));
	}

	@Override
	public BigInteger getGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException {
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		var _100_000 = BigInteger.valueOf(100_000);

		boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.IGNORES_GAS_PRICE, gasStation))).getValue();

		// this helps with testing, since otherwise previous tests might make the gas price explode for the subsequent tests
		if (ignoresGasPrice)
			return BigInteger.ONE;

		// we double the minimal price, to be sure that the transaction won't be rejected
		return ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation))).getValue();
	}

	@Override
	public BigInteger getSafeGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return BigInteger.TWO.multiply(getGasPrice());
	}
}