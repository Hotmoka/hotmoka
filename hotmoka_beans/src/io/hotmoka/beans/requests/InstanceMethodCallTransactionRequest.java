package io.hotmoka.beans.requests;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling an instance method of a storage object in blockchain.
 */
@Immutable
public class InstanceMethodCallTransactionRequest extends MethodCallTransactionRequest {

	private static final long serialVersionUID = -1016861794592561931L;

	/**
	 * The receiver of the call.
	 */
	public final StorageReference receiver;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceMethodCallTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, gas, classpath, method, actuals);

		this.receiver = receiver;
	}

	@Override
	public String toString() {
        return super.toString() + "\n  receiver: " + receiver;
	}
}