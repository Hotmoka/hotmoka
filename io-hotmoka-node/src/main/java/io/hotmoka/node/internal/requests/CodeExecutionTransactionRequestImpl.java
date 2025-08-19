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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.requests.CodeExecutionTransactionRequest;
import io.hotmoka.node.api.responses.CodeExecutionTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.json.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * A request for executing a constructor or a method.
 * 
 * @param <R> the type of the corresponding response
 */
@Immutable
public abstract class CodeExecutionTransactionRequestImpl<R extends CodeExecutionTransactionResponse> extends NonInitialTransactionRequestImpl<R> implements CodeExecutionTransactionRequest<R> {

	/**
	 * The actual arguments passed to the method.
	 */
	private final StorageValue[] actuals;

	/**
	 * Builds the transaction request.
	 * 
	 * @param <E> the type of the exception thrown if some argument passed to this constructor is illegal
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param actuals the actual arguments passed to the method
	 * @param onIllegalArgs the creator of the exception thrown if some argument passed to this constructor is illegal
	 * @throws E if some argument passed to this constructor is illegal
	 */
	protected <E extends Exception> CodeExecutionTransactionRequestImpl(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, StorageValue[] actuals, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(caller, nonce, gasLimit, gasPrice, classpath, onIllegalArgs);

		this.actuals = Objects.requireNonNull(actuals, "actuals cannot be null", onIllegalArgs);
		for (var actual: actuals)
			Objects.requireNonNull(actual, "actuals cannot hold null elements", onIllegalArgs);
	}

	@Override
	public final Stream<StorageValue> actuals() {
		return Stream.of(actuals);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CodeExecutionTransactionRequestImpl<?> cetri)
			return super.equals(other) && Arrays.equals(actuals, cetri.actuals); // optimization
		else
			return other instanceof CodeExecutionTransactionRequest<?> cetr && super.equals(other) && Arrays.equals(actuals, cetr.actuals().toArray(StorageValue[]::new));
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

	public final byte[] toByteArrayWithoutSignature() {
		try (var baos = new ByteArrayOutputStream(); var context = NodeMarshallingContexts.of(baos)) {
			intoWithoutSignature(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			throw new UncheckedIOException("Unexpected exception", e);
		}
	}

	protected static StorageValue[] convertedActuals(TransactionRequestJson json) throws InconsistentJsonException {
		var actuals = json.getActuals().toArray(StorageValues.Json[]::new);
		var result = new StorageValue[actuals.length];
		for (int pos = 0; pos < result.length; pos++)
			result[pos] = Objects.requireNonNull(actuals[pos], "actuals cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
	}
}