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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.signatures.CodeSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * A request for executing a constructor or a method.
 * 
 * @param <R> the type of the corresponding response
 */
@Immutable
public abstract class CodeExecutionTransactionRequest<R extends CodeExecutionTransactionResponse> extends NonInitialTransactionRequest<R> {

	/**
	 * The actual arguments passed to the method.
	 */
	private final StorageValue[] actuals;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param actuals the actual arguments passed to the method
	 */
	protected CodeExecutionTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath);

		this.actuals = Objects.requireNonNull(actuals, "actuals cannot be null");
		Stream.of(actuals).forEach(actual -> Objects.requireNonNull(actual, "actuals cannot hold null"));
	}

	/**
	 * Yields the actual arguments passed to the method.
	 * 
	 * @return the actual arguments
	 */
	public final Stream<StorageValue> actuals() {
		return Stream.of(actuals);
	}

	/**
	 * Yields the method or constructor referenced in this request.
	 * 
	 * @return the method or constructor
	 */
	public abstract CodeSignature getStaticTarget();

	@Override
	public boolean equals(Object other) {
		return other instanceof CodeExecutionTransactionRequest<?> cetr && super.equals(other) && Arrays.equals(actuals, cetr.actuals);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.deepHashCode(actuals);
	}

	@Override
	protected void intoWithoutSignature(MarshallingContext context) throws IOException {
		super.intoWithoutSignature(context);
		context.writeLengthAndArray(actuals);
	}

	/**
	 * Marshals this object into a byte array, without taking its signature into account.
	 * 
	 * @return the byte array resulting from marshalling this object
	 */
	public byte[] toByteArrayWithoutSignature() {
		try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
			intoWithoutSignature(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			// impossible with a byte array output stream
			throw new RuntimeException("Unexpected exception", e);
		}
	}
}