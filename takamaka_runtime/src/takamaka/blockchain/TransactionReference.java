package takamaka.blockchain;

import takamaka.lang.Immutable;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link takamaka.blockchain.Blockchain} will
 * provide its implementation of this class.
 * They must be comparable and ordered according to their occurrence in the blockchain.
 */
@Immutable
public abstract class TransactionReference implements Comparable<TransactionReference> {

	public final boolean isOlderThan(TransactionReference other) {
		return compareTo(other) < 0;
	}

	@Override
	public abstract boolean equals(Object other);

	@Override
	public abstract int hashCode();

	@Override
	public abstract String toString();
}