package takamaka.blockchain.response;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Immutable;

/**
 * A response for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class GameteCreationTransactionResponse implements TransactionResponse, AbstractTransactionResponseWithUpdates {

	private static final long serialVersionUID = -95476487153660743L;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The created gamete.
	 */
	public final StorageReference gamete;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gamete the created gamete
	 */
	public GameteCreationTransactionResponse(Stream<Update> updates, StorageReference gamete) {
		this.updates = updates.toArray(Update[]::new);
		this.gamete = gamete;
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
        	+ "  gamete: " + gamete + "\n"
       		+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}