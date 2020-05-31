package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a static method of a storage class in a node.
 */
@Immutable
public class StaticMethodCallTransactionRequest extends MethodCallTransactionRequest {
	final static byte SELECTOR = 6;

	private final byte[] signature;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 */
	public StaticMethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		this(caller, nonce, gasLimit, gasPrice, classpath, method, new byte[40], actuals);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param signature the signature of the request
	 * @param actuals the actual arguments passed to the method
	 */
	StaticMethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, byte[] signature, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);

		this.signature = signature;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StaticMethodCallTransactionRequest && super.equals(other);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		writeLength(signature.length, oos);
		oos.write(signature);
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}
}