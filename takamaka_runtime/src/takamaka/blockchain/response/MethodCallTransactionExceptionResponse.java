package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;

import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a successful transaction that calls a method in blockchain.
 * The method is annotated as {@link takamaka.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an instance of {@link java.lang.Exception}.
 */
@Immutable
public class MethodCallTransactionExceptionResponse extends MethodCallTransactionResponse {

	private static final long serialVersionUID = 5236790249190745461L;

	/**
	 * The exception that has been thrown by the method.
	 */
	public final transient Exception exception;

	/**
	 * Builds the transaction response.
	 * 
	 * @param exception the exception that has been thrown by the method
	 * @param updates the updates resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public MethodCallTransactionExceptionResponse(Exception exception, Set<Update> updates, BigInteger consumedGas) {
		super(updates, consumedGas);

		this.exception = exception;
	}
}