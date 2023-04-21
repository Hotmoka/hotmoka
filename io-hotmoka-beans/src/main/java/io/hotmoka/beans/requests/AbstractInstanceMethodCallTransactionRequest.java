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

package io.hotmoka.beans.requests;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.marshalling.MarshallingContext;

/**
 * A request for calling an instance method of a storage object in a node.
 */
@Immutable
public abstract class AbstractInstanceMethodCallTransactionRequest extends MethodCallTransactionRequest {

	/**
	 * The receiver of the call.
	 */
	public final StorageReference receiver;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	protected AbstractInstanceMethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);

		if (receiver == null)
			throw new IllegalArgumentException("receiver cannot be null");

		this.receiver = receiver;
	}

	@Override
	public String toString() {
        return super.toString()
        	+ "\n  receiver: " + receiver;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AbstractInstanceMethodCallTransactionRequest &&
			super.equals(other) && receiver.equals(((AbstractInstanceMethodCallTransactionRequest) other).receiver);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ receiver.hashCode();
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(receiver.size(gasCostModel));
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		super.intoWithoutSignature(context);
		receiver.intoWithoutSelector(context);
	}
}