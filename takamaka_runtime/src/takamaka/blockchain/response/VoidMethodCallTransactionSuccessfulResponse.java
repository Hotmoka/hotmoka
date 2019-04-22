package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;

import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a successful transaction that calls a method
 * in blockchain. The method has been called without problems and
 * without generating exceptions. The method returns {@code void}.
 */
@Immutable
public class VoidMethodCallTransactionSuccessfulResponse extends MethodCallTransactionResponse {

	private static final long serialVersionUID = -2888023047206147277L;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public VoidMethodCallTransactionSuccessfulResponse(Set<Update> updates, BigInteger consumedGas) {
		super(updates, consumedGas);
	}
}