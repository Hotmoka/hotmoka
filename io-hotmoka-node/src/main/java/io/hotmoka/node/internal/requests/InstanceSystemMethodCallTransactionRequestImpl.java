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

package io.hotmoka.node.internal.requests;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a request for calling an instance method of a storage object in a node.
 * This request is not signed, hence it is only used for calls started by the same
 * node. Users cannot run a transaction from this request.
 */
@Immutable
public class InstanceSystemMethodCallTransactionRequestImpl extends AbstractInstanceMethodCallTransactionRequestImpl implements InstanceSystemMethodCallTransactionRequest {
	final static byte SELECTOR = 11;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceSystemMethodCallTransactionRequestImpl(StorageReference caller, BigInteger nonce, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, BigInteger.ZERO, classpath, method, receiver, actuals);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":\n"
	       	+ "  caller: " + getCaller() + "\n"
	       	+ "  nonce: " + getNonce() + "\n"
	       	+ "  gas limit: " + getGasLimit() + "\n"
	       	+ "  class path: " + getClasspath() + "\n"
	       	+ "  receiver: " + getReceiver() + "\n"
	       	+ toStringMethod();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InstanceSystemMethodCallTransactionRequest && super.equals(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		getCaller().intoWithoutSelector(context);
		context.writeBigInteger(getGasLimit());
		getClasspath().into(context);
		context.writeBigInteger(getNonce());
		context.writeLengthAndArray(actuals().toArray(Marshallable[]::new));
		getStaticTarget().into(context);
		getReceiver().intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 */
	public static InstanceSystemMethodCallTransactionRequestImpl from(UnmarshallingContext context) throws IOException {
		var caller = StorageValues.referenceWithoutSelectorFrom(context);
		var gasLimit = context.readBigInteger();
		var classpath = TransactionReferences.from(context);
		var nonce = context.readBigInteger();
		StorageValue[] actuals = context.readLengthAndArray(StorageValues::from, StorageValue[]::new);
		var method = MethodSignatures.from(context);
		var receiver = StorageValues.referenceWithoutSelectorFrom(context);

		return new InstanceSystemMethodCallTransactionRequestImpl(caller, nonce, gasLimit, classpath, method, receiver, actuals);
	}
}