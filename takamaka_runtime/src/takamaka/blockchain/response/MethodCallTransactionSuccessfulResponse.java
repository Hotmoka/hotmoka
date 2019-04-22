package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;

import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * A response for a successful transaction that calls a method
 * in blockchain. The method has been called without problems and
 * without generating exceptions. The method does not return {@code void}.
 */
@Immutable
public class MethodCallTransactionSuccessfulResponse extends MethodCallTransactionResponse {

	private static final long serialVersionUID = 2888406427592732867L;

	/**
	 * The return value of the method.
	 */
	public final StorageValue result;

	/**
	 * Builds the transaction response.
	 * 
	 * @param result the value returned by the method
	 * @param updates the updates resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public MethodCallTransactionSuccessfulResponse(StorageValue result, Set<Update> updates, BigInteger consumedGas) {
		super(updates, consumedGas);

		this.result = result;
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  returned value: " + result;
	}
}