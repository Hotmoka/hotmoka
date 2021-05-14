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

package io.hotmoka.takamaka.beans.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a request that successfully added or reduced the coins of an account.
 */
@Immutable
public class MintTransactionSuccessfulResponse extends MintTransactionResponse {

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MintTransactionSuccessfulResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MintTransactionSuccessfulResponse && super.equals(other);
	}

	@Override
	public void getOutcome() {}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(EXPANSION_SELECTOR);
		// after the expansion selector, the qualified name of the class must follow
		context.writeUTF(MintTransactionSuccessfulResponse.class.getName());
		super.into(context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static MintTransactionSuccessfulResponse from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		Stream<Update> updates = Stream.of(context.readArray(Update::from, Update[]::new));
		BigInteger gasConsumedForCPU = context.readBigInteger();
		BigInteger gasConsumedForRAM = context.readBigInteger();
		BigInteger gasConsumedForStorage = context.readBigInteger();

		return new MintTransactionSuccessfulResponse(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}