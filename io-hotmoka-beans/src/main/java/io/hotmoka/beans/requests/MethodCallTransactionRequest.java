package io.hotmoka.beans.requests;

import java.math.BigInteger;
import java.util.stream.Collectors;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a static method of a storage class in blockchain.
 */
@Immutable
public abstract class MethodCallTransactionRequest extends CodeExecutionTransactionRequest<MethodCallTransactionResponse> {
	private static final long serialVersionUID = -501977352886002289L;

	/**
	 * The constructor to call.
	 */
	public final MethodSignature method;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 */
	protected MethodCallTransactionRequest(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageValue... actuals) {
		super(caller, gasLimit, gasPrice, classpath, actuals);
		
		this.method = method;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  gas limit: " + gasLimit + "\n"
        	+ "  gas price: " + gasPrice + "\n"
        	+ "  class path: " + classpath + "\n"
			+ "  method: " + method + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}