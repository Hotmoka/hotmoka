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
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Implementation of a request for a transaction that can only be run after the node has been initialized.
 *
 * @param <R> the type of the corresponding response
 */
@Immutable
public abstract class NonInitialTransactionRequestImpl<R extends NonInitialTransactionResponse> extends TransactionRequestImpl<R> implements NonInitialTransactionRequest<R> {

	/**
	 * The externally owned caller contract that pays for the transaction.
	 */
	private final StorageReference caller;

	/**
	 * The gas provided to the transaction.
	 */
	private final BigInteger gasLimit;

	/**
	 * The coins payed for each unit of gas consumed by the transaction.
	 */
	private final BigInteger gasPrice;

	/**
	 * The class path that specifies where the {@code caller} should be interpreted.
	 */
	private final TransactionReference classpath;

	/**
	 * The nonce used for transaction ordering and to forbid transaction replay on the same chain.
	 * It is relative to the caller.
	 */
	private final BigInteger nonce;

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	protected <E extends Exception> NonInitialTransactionRequestImpl(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.caller = Objects.requireNonNull(caller, "caller cannot be null", onIllegalArgs);
		this.gasLimit = Objects.requireNonNull(gasLimit, "gasLimit cannot be null", onIllegalArgs);
		this.gasPrice = Objects.requireNonNull(gasPrice, "gasPrice cannot be null", onIllegalArgs);
		this.classpath = Objects.requireNonNull(classpath, "classpath cannot be null", onIllegalArgs);
		this.nonce = Objects.requireNonNull(nonce, "nonce cannot be null", onIllegalArgs);

		if (gasLimit.signum() < 0)
			throw onIllegalArgs.apply("gasLimit cannot be negative");

		if (gasPrice.signum() < 0)
			throw onIllegalArgs.apply("gasPrice cannot be negative");

		if (nonce.signum() < 0)
			throw onIllegalArgs.apply("nonce cannot be negative");
	}

	@Override
	public final StorageReference getCaller() {
		return caller;
	}

	@Override
	public final BigInteger getGasLimit() {
		return gasLimit;
	}

	@Override
	public final BigInteger getGasPrice() {
		return gasPrice;
	}

	@Override
	public final BigInteger getNonce() {
		return nonce;
	}

	@Override
	public final TransactionReference getClasspath() {
		return classpath;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  nonce: " + nonce + "\n"
        	+ "  gas limit: " + gasLimit + "\n"
        	+ "  gas price: " + gasPrice + "\n"
        	+ "  class path: " + classpath;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NonInitialTransactionRequest<?> nitr && caller.equals(nitr.getCaller())
			&& gasLimit.equals(nitr.getGasLimit()) && gasPrice.equals(nitr.getGasPrice())
			&& classpath.equals(nitr.getClasspath()) && nonce.equals(nitr.getNonce());
	}

	@Override
	public int hashCode() {
		return caller.hashCode() ^ gasLimit.hashCode() ^ gasPrice.hashCode() ^ classpath.hashCode() ^ nonce.hashCode();
	}

	/**
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * The difference with {@link #into(MarshallingContext)} is that the signature (if any)
	 * is not marshalled into the stream.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if the request cannot be unmarshalled
	 */
	protected void intoWithoutSignature(MarshallingContext context) throws IOException {
		caller.intoWithoutSelector(context);
		context.writeBigInteger(gasLimit);
		context.writeBigInteger(gasPrice);
		classpath.into(context);
		context.writeBigInteger(nonce);
	}
}