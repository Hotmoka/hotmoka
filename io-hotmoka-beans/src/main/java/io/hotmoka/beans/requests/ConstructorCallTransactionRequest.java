package io.hotmoka.beans.requests;

import java.math.BigInteger;
import java.util.stream.Collectors;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a constructor of a storage class in blockchain.
 */
@Immutable
public class ConstructorCallTransactionRequest extends CodeExecutionTransactionRequest<ConstructorCallTransactionResponse> {

	private static final long serialVersionUID = -6485399240275200765L;

	/**
	 * The constructor to call.
	 */
	public final ConstructorSignature constructor;

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
	public ConstructorCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, ConstructorSignature constructor, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		this.constructor = constructor;
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
			+ "  constructor: " + constructor + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}