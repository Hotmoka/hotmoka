package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.stream.Collectors;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a static method of a storage class in a node.
 */
@Immutable
public abstract class MethodCallTransactionRequest extends CodeExecutionTransactionRequest<MethodCallTransactionResponse> {

	/**
	 * The constructor to call.
	 */
	public final MethodSignature method;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param actuals the actual arguments passed to the method
	 */
	protected MethodCallTransactionRequest(StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) {
		super(caller, nonce, chainId, gasLimit, gasPrice, classpath, actuals);
		
		this.method = method;
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
			+ "  method: " + method + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public final MethodSignature getStaticTarget() {
		return method;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodCallTransactionRequest && super.equals(other) && method.equals(((MethodCallTransactionRequest) other).method);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ method.hashCode();
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(method.size(gasCostModel));
	}

	@Override
	public void intoWithoutSignature(ObjectOutputStream oos) throws IOException {
		super.intoWithoutSignature(oos);
		method.into(oos);
	}
}