package io.takamaka.code.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.Update;
import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.StorageReference;

/**
 * A response for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class GameteCreationTransactionResponse implements TransactionResponse, TransactionResponseWithUpdates {

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

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  gamete: " + gamete + "\n"
       		+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		// this response is for a free transaction, at initialization of the blockchain
		return BigInteger.ZERO;
	}
}