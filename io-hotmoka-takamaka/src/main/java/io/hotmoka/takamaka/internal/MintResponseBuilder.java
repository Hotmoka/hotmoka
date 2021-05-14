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

package io.hotmoka.takamaka.internal;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.local.AbstractLocalNode;
import io.hotmoka.local.NonInitialResponseBuilder;
import io.hotmoka.takamaka.beans.requests.MintTransactionRequest;
import io.hotmoka.takamaka.beans.responses.MintTransactionFailedResponse;
import io.hotmoka.takamaka.beans.responses.MintTransactionResponse;
import io.hotmoka.takamaka.beans.responses.MintTransactionSuccessfulResponse;

/**
 * The creator of a response for a transaction that mints or burns coins.
 */
public class MintResponseBuilder extends NonInitialResponseBuilder<MintTransactionRequest, MintTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public MintResponseBuilder(TransactionReference reference, MintTransactionRequest request, AbstractLocalNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = request.gasLimit;
		return new MintTransactionFailedResponse("placeholder for the name of the exception", "placeholder for the message of the exception", Stream.empty(), gas, gas, gas, gas).size(gasCostModel);
	}

	@Override
	public MintTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends NonInitialResponseBuilder<MintTransactionRequest, MintTransactionResponse>.ResponseCreator {
		
		private ResponseCreator() throws TransactionRejectedException {}

		@Override
		protected MintTransactionResponse body() {
			try {
				init();
				Object deserializedCaller = getDeserializedCaller();
				BigInteger greenBalance = classLoader.getBalanceOf(deserializedCaller).add(request.greenAmount);
				if (greenBalance.signum() < 0)
					throw new IllegalStateException("the account has not enough balance to burn " + request.greenAmount.negate() + " green coins");

				// we access the red balance field only if the red balance is requested to be modified
				if (request.redAmount.signum() != 0) {
					BigInteger redBalance = classLoader.getRedBalanceOf(deserializedCaller).add(request.redAmount);
					if (redBalance.signum() < 0)
						throw new IllegalStateException("the account has not enough balance to burn " + request.redAmount.negate() + " red coins");

					// we modify the balances only if both can be charged
					classLoader.setRedBalanceOf(deserializedCaller, redBalance);
				}

				classLoader.setBalanceOf(deserializedCaller, greenBalance);

				chargeGasForStorageOf(new MintTransactionSuccessfulResponse(extractUpdatesFrom(Stream.of(deserializedCaller)), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				return new MintTransactionSuccessfulResponse(extractUpdatesFrom(Stream.of(deserializedCaller)), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				return new MintTransactionFailedResponse(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		@Override
		public void event(Object event) {
		}
	}
}