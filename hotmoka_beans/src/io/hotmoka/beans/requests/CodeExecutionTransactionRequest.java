package io.hotmoka.beans.requests;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

@Immutable
public abstract class CodeExecutionTransactionRequest<R extends CodeExecutionTransactionResponse> extends NonInitialTransactionRequest<R> {
	private static final long serialVersionUID = 6397242982438061162L;

	/**
	 * The actual arguments passed to the method.
	 */
	private final StorageValue[] actuals;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param actuals the actual arguments passed to the method
	 */
	protected CodeExecutionTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath, StorageValue... actuals) {
		super(caller, gas, classpath);

		this.actuals = actuals;
	}

	/**
	 * Yields the actual arguments passed to the method.
	 * 
	 * @return the actual arguments
	 */
	public final Stream<StorageValue> actuals() {
		return Stream.of(actuals);
	}
}