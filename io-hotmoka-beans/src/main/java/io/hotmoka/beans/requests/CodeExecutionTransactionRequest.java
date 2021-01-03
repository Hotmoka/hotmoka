package io.hotmoka.beans.requests;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

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

		if (actuals == null)
			throw new IllegalArgumentException("actuals cannot be null");

		for (StorageValue actual: actuals)
			if (actual == null)
				throw new IllegalArgumentException("actuals cannot hold null");

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

	/**
	 * Yields the method or constructor referenced in this request.
	 * 
	 * @return the method or constructor
	 */
	public abstract CodeSignature getStaticTarget();

	@Override
	public boolean equals(Object other) {
		return other instanceof CodeExecutionTransactionRequest<?> && super.equals(other) && Arrays.equals(actuals, ((CodeExecutionTransactionRequest<?>) other).actuals);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.deepHashCode(actuals);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(actuals().map(actual -> actual.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	protected void intoWithoutSignature(MarshallingContext context) throws IOException {
		super.intoWithoutSignature(context);
		intoArray(actuals, context);
	}
}