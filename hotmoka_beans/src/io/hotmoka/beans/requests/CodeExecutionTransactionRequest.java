package io.hotmoka.beans.requests;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.values.StorageReference;

@Immutable
public abstract class CodeExecutionTransactionRequest<R extends CodeExecutionTransactionResponse> extends NonInitialTransactionRequest<R> {
	private static final long serialVersionUID = 6397242982438061162L;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 */
	protected CodeExecutionTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath) {
		super(caller, gas, classpath);
	}
}