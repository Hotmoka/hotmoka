package takamaka.blockchain;

import takamaka.lang.Immutable;

/**
 * A classpath built from a given jar.
 */

@Immutable
public final class Classpath {

	/**
	 * The transaction that stored the jar.
	 */
	public final TransactionReference transaction;

	/**
	 * True if the dependencies of the jar must be included in the classpath.
	 */
	public final boolean recursive;

	/**
	 * Builds a classpath.
	 * 
	 * @param transaction The transaction that stored the jar.
	 * @param recursive True if the dependencies of the jar must be included in the classpath.
	 */
	Classpath(TransactionReference transaction, boolean recursive) {
		this.transaction = transaction;
		this.recursive = recursive;
	}
}