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
import java.util.stream.Collectors;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.requests.MethodCallTransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of a request for calling a method in a node.
 */
@Immutable
public abstract class MethodCallTransactionRequestImpl extends CodeExecutionTransactionRequestImpl<MethodCallTransactionResponse> implements MethodCallTransactionRequest {

	/**
	 * The constructor to call.
	 */
	private final MethodSignature method;

	/**
	 * Used as empty signature for view transaction requests.
	 */
	protected static byte[] NO_SIG = new byte[0];

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	protected <E extends Exception> MethodCallTransactionRequestImpl(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue[] actuals, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals, onIllegalArgs);

		this.method = Objects.requireNonNull(method, "method cannot be null", onIllegalArgs);

		if (method.getFormals().count() != actuals.length)
			throw onIllegalArgs.apply("Argument count mismatch: " + method.getFormals().count() + " formals vs " + actuals.length + " actuals");
	}

	@Override
	public String toString() {
		return super.toString() + "\n" + toStringMethod();
	}

	/**
	 * Yields a string description of the method called by this request.
	 * 
	 * @return the description
	 */
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
		return other instanceof MethodCallTransactionRequest mctr && super.equals(other) && method.equals(mctr.getStaticTarget());
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