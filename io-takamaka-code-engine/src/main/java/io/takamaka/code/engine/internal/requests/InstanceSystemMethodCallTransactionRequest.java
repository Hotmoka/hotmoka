package io.takamaka.code.engine.internal.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SystemTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling an instance method of a storage object in a node.
 * This request is not signed, hence it is only used for calls started by the same
 * node. Users cannot run a transaction from this request.
 */
@Immutable
public class InstanceSystemMethodCallTransactionRequest extends AbstractInstanceMethodCallTransactionRequest implements SystemTransactionRequest {

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceSystemMethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, BigInteger.ZERO, classpath, method, receiver, actuals);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":\n"
	       	+ "  caller: " + caller + "\n"
	       	+ "  nonce: " + nonce + "\n"
	       	+ "  gas limit: " + gasLimit + "\n"
	       	+ "  class path: " + classpath + "\n"
	       	+ "  receiver: " + receiver + "\n"
	       	+ toStringMethod();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InstanceSystemMethodCallTransactionRequest && super.equals(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(EXPANSION_SELECTOR);
		// after the expansion selector, the qualified name of the class must follow
		context.oos.writeUTF(InstanceSystemMethodCallTransactionRequest.class.getName());
		caller.intoWithoutSelector(context);
		marshal(gasLimit, context);
		classpath.into(context);
		marshal(nonce, context);
		intoArray(actuals().toArray(Marshallable[]::new), context);
		method.into(context);
		receiver.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static InstanceSystemMethodCallTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		StorageReference caller = StorageReference.from(ois);
		BigInteger gasLimit = unmarshallBigInteger(ois);
		TransactionReference classpath = TransactionReference.from(ois);
		BigInteger nonce = unmarshallBigInteger(ois);
		StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
		MethodSignature method = (MethodSignature) CodeSignature.from(ois);
		StorageReference receiver = StorageReference.from(ois);

		return new InstanceSystemMethodCallTransactionRequest(caller, nonce, gasLimit, classpath, method, receiver, actuals);
	}
}