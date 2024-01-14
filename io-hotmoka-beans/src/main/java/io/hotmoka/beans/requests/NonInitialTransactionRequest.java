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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.marshalling.api.MarshallingContext;

@Immutable
public abstract class NonInitialTransactionRequest<R extends NonInitialTransactionResponse> extends TransactionRequest<R> {

	/**
	 * The externally owned caller contract that pays for the transaction.
	 */
	public final StorageReference caller;

	/**
	 * The gas provided to the transaction.
	 */
	public final BigInteger gasLimit;

	/**
	 * The coins payed for each unit of gas consumed by the transaction.
	 */
	public final BigInteger gasPrice;

	/**
	 * The class path that specifies where the {@code caller} should be interpreted.
	 */
	public final TransactionReference classpath;

	/**
	 * The nonce used for transaction ordering and to forbid transaction replay on the same chain.
	 * It is relative to the caller.
	 */
	public final BigInteger nonce;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 */
	protected NonInitialTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath) {
		this.caller = Objects.requireNonNull(caller, "caller cannot be null");
		this.gasLimit = Objects.requireNonNull(gasLimit, "gasLimit cannot be null");
		this.gasPrice = Objects.requireNonNull(gasPrice, "gasPrice cannot be null");
		this.classpath = Objects.requireNonNull(classpath, "classpath cannot be null");
		this.nonce = Objects.requireNonNull(nonce, "nonce cannot be null");

		if (gasLimit.signum() < 0)
			throw new IllegalArgumentException("gasLimit cannot be negative");

		if (gasPrice.signum() < 0)
			throw new IllegalArgumentException("gasPrice cannot be negative");

		if (nonce.signum() < 0)
			throw new IllegalArgumentException("nonce cannot be negative");
	}

	/**
	 * Yields the caller of the request.
	 * 
	 * @return the caller
	 */
	public final StorageReference getCaller() {
		return caller;
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
		return other instanceof NonInitialTransactionRequest<?> nitr && caller.equals(nitr.caller)
			&& gasLimit.equals(nitr.gasLimit) && gasPrice.equals(nitr.gasPrice)
			&& classpath.equals(nitr.classpath) && nonce.equals(nitr.nonce);
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