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
import java.nio.charset.StandardCharsets;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.BeanMarshallingContext;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.values.StorageReference;

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
	 * The array of hexadecimal digits.
	 */
	private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes();

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
		if (caller == null)
			throw new IllegalArgumentException("caller cannot be null");

		if (gasLimit == null)
			throw new IllegalArgumentException("gasLimit cannot be null");

		if (gasLimit.signum() < 0)
			throw new IllegalArgumentException("gasLimit cannot be negative");

		if (gasPrice == null)
			throw new IllegalArgumentException("gasPrice cannot be null");

		if (gasPrice.signum() < 0)
			throw new IllegalArgumentException("gasPrice cannot be negative");

		if (classpath == null)
			throw new IllegalArgumentException("classpath cannot be null");

		if (nonce == null)
			throw new IllegalArgumentException("nonce cannot be null");

		if (nonce.signum() < 0)
			throw new IllegalArgumentException("nonce cannot be negative");

		this.caller = caller;
		this.gasLimit = gasLimit;
		this.gasPrice = gasPrice;
		this.classpath = classpath;
		this.nonce = nonce;
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
		if (other instanceof NonInitialTransactionRequest) {
			NonInitialTransactionRequest<?> otherCast = (NonInitialTransactionRequest<?>) other;
			return caller.equals(otherCast.caller) && gasLimit.equals(otherCast.gasLimit) && gasPrice.equals(otherCast.gasPrice)
				&& classpath.equals(otherCast.classpath) && nonce.equals(otherCast.nonce);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return caller.hashCode() ^ gasLimit.hashCode() ^ gasPrice.hashCode() ^ classpath.hashCode() ^ nonce.hashCode();
	}

	/**
	 * Yields the size of this request, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of the costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
			.add(caller.size(gasCostModel))
			.add(gasCostModel.storageCostOf(gasLimit))
			.add(gasCostModel.storageCostOf(gasPrice))
			.add(gasCostModel.storageCostOf(classpath))
			.add(gasCostModel.storageCostOf(nonce));
	}

	/**
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * The difference with {@link #into(MarshallingContext)} is that the signature (if any)
	 * is not marshalled into the stream.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	protected void intoWithoutSignature(BeanMarshallingContext context) throws IOException {
		caller.intoWithoutSelector(context);
		context.writeBigInteger(gasLimit);
		context.writeBigInteger(gasPrice);
		classpath.into(context);
		context.writeBigInteger(nonce);
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	protected static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    int pos = 0;
	    for (byte b: bytes) {
	        int v = b & 0xFF;
	        hexChars[pos++] = HEX_ARRAY[v >>> 4];
	        hexChars[pos++] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}
}