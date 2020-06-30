package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
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
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param actuals the actual arguments passed to the method
	 */
	protected CodeExecutionTransactionRequest(StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, StorageValue... actuals) {
		super(caller, nonce, chainId, gasLimit, gasPrice, classpath);

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
	public void intoWithoutSignature(ObjectOutputStream oos) throws IOException {
		super.intoWithoutSignature(oos);
		intoArray(actuals, oos);
	}

	@Override
	public void check() throws TransactionRejectedException {
		if (getStaticTarget().formals().count() != actuals.length)
			throw new TransactionRejectedException("argument count mismatch between formals and actuals");

		super.check();
	}
}