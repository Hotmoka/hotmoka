package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
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
	final static byte SELECTOR = 5;

	/**
	 * The receiver of the call.
	 */
	public final StorageReference receiver;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceMethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, method, actuals);

		this.receiver = receiver;
	}

	@Override
	public String toString() {
        return super.toString() + "\n  receiver: " + receiver;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InstanceMethodCallTransactionRequest && super.equals(other) && receiver.equals(((InstanceMethodCallTransactionRequest) other).receiver);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ receiver.hashCode();
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		receiver.into(oos);
	}
}