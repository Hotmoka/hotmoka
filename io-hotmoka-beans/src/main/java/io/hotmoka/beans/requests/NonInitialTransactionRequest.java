package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
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
	public final Classpath classpath;

	/**
	 * The nonce used for transaction ordering and to forbid transaction replay.
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
	protected NonInitialTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath) {
		this.caller = caller;
		this.gasLimit = gasLimit;
		this.gasPrice = gasPrice;
		this.classpath = classpath;
		this.nonce = nonce;
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

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		caller.into(oos);
		marshal(gasLimit, oos);
		marshal(gasPrice, oos);
		classpath.into(oos);
		marshal(nonce, oos);
	}
}