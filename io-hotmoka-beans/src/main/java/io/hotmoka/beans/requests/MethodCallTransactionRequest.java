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
import java.util.Objects;
import java.util.stream.Collectors;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * A request for calling a static method of a storage class in a node.
 */
@Immutable
public abstract class MethodCallTransactionRequest extends CodeExecutionTransactionRequest<MethodCallTransactionResponse> {

	/**
	 * The constructor to call.
	 */
	public final MethodSignature method;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 */
	protected MethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		this.method = Objects.requireNonNull(method, "method cannot be null");

		if (method.getFormals().count() != actuals.length)
			throw new IllegalArgumentException("Argument count mismatch between formals and actuals");
	}

	@Override
	public String toString() {
		return super.toString() + "\n" + toStringMethod();
	}

	protected final String toStringMethod() {
		if (actuals().count() == 0L)
			return "  method: " + method;
		else
			return "  method: " + method + "\n" + "  actuals:" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "\n    ", ""));
	}

	@Override
	public final MethodSignature getStaticTarget() {
		return method;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodCallTransactionRequest && super.equals(other) && method.equals(((MethodCallTransactionRequest) other).method);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ method.hashCode();
	}

	@Override
	protected void intoWithoutSignature(MarshallingContext context) throws IOException {
		super.intoWithoutSignature(context);
		method.into(context);
	}
}