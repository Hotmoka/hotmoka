package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.stream.Collectors;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a constructor of a storage class in a node.
 */
@Immutable
public class ConstructorCallTransactionRequest extends CodeExecutionTransactionRequest<ConstructorCallTransactionResponse> {
	final static byte SELECTOR = 4;

	/**
	 * The constructor to call.
	 */
	public final ConstructorSignature constructor;

	/**
	 * The signature of the request.
	 */
	private final byte[] signature;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 */
	public ConstructorCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) {
		this(caller, nonce, gasLimit, gasPrice, classpath, constructor, new byte[40], actuals);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param signature the signature of the request
	 * @param actuals the actual arguments passed to the constructor
	 */
	ConstructorCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, byte[] signature, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		this.constructor = constructor;
		this.signature = signature;
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
			+ "  constructor: " + constructor + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public ConstructorSignature getStaticTarget() {
		return constructor;
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ConstructorCallTransactionRequest && super.equals(other) && constructor.equals(((ConstructorCallTransactionRequest) other).constructor);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ constructor.hashCode();
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		constructor.into(oos);
		writeLength(signature.length, oos);
		oos.write(signature);
	}
}