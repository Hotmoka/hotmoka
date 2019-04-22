package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a transaction that should call a constructor of a storage class in blockchain.
 */
@Immutable
public abstract class ConstructorCallTransactionResponse implements TransactionResponse, AbstractTransactionResponseWithUpdates {

	private static final long serialVersionUID = 6999069256965379003L;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The amount of gas consumed by the transaction.
	 */
	public final BigInteger consumedGas;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public ConstructorCallTransactionResponse(Set<Update> updates, BigInteger consumedGas) {
		this.updates = updates.toArray(new Update[updates.size()]);
		this.consumedGas = consumedGas;
	}

	/**
	 * Yields the updates induced by the execution of this trsnaction.
	 * 
	 * @return the updates
	 */
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  consumed gas: " + consumedGas + "\n"
        	+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}