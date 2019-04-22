package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionException;
import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a failed transaction that should have called a constructor
 * of a storage class in blockchain.
 */
@Immutable
public class ConstructorCallTransactionFailedResponse extends ConstructorCallTransactionResponse {

	private static final long serialVersionUID = 3291328917017257182L;

	/**
	 * The exception that justifies why the transaction failed. This is not reported
	 * in the serialization of this response.
	 */
	public final transient TransactionException cause;

	/**
	 * Builds the transaction response.
	 * 
	 * @param cause the exception that justifies why the transaction failed
	 * @param updates the updates resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public ConstructorCallTransactionFailedResponse(TransactionException cause, Stream<Update> updates, BigInteger consumedGas) {
		super(updates, consumedGas);

		this.cause = cause;
	}
}