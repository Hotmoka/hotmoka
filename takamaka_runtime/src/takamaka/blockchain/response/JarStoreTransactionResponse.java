package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a transaction that should install a jar in the blockchain.
 */
@Immutable
public abstract class JarStoreTransactionResponse implements TransactionResponse {

	private static final long serialVersionUID = -8888957484092351352L;

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
	public JarStoreTransactionResponse(Set<Update> updates, BigInteger consumedGas) {
		this.updates = updates.toArray(new Update[updates.size()]);
		this.consumedGas = consumedGas;
	}

	public Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  consumed gas: " + consumedGas + "\n"
        	+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}