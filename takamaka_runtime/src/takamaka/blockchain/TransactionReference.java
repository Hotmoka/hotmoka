package takamaka.blockchain;

import takamaka.lang.Immutable;

/**
 * A transaction reference is a reference to a transaction in the blockchain.
 * They are comparable and ordered according to their occurrence in the blockchain.
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