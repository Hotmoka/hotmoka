package io.takamaka.code.blockchain.requests;

import java.math.BigInteger;

import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.StorageReference;

@Immutable
public abstract class NonInitialTransactionRequest implements TransactionRequest {
	private static final long serialVersionUID = 8584281399116101538L;

	/**
	 * The externally owned caller contract that pays for the transaction.
	 */
	public final StorageReference caller;

	/**
	 * The gas provided to the transaction.
	 */
	public final BigInteger gas;

	/**
	 * The class path that specifies where the {@code caller} should be interpreted.
	 */
	public final Classpath classpath;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 */
	protected NonInitialTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath) {
		this.caller = caller;
		this.gas = gas;
		this.classpath = classpath;
	}
}