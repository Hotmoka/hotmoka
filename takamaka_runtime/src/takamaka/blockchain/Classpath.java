package takamaka.blockchain;

import java.io.Serializable;

import takamaka.lang.Immutable;

/**
 * A class path, that points to a given jar in the blockchain.
 */

@Immutable
public final class Classpath implements Serializable {

	private static final long serialVersionUID = -9014808081346651444L;

	/**
	 * The transaction that stored the jar.
	 */
	public final TransactionReference transaction;

	/**
	 * True if the dependencies of the jar must be included in the class path.
	 */
	public final boolean recursive;

	/**
	 * Builds a class path.
	 * 
	 * @param transaction The transaction that stored the jar
	 * @param recursive True if the dependencies of the jar must be included in the class path
	 */
	public Classpath(TransactionReference transaction, boolean recursive) {
		this.transaction = transaction;
		this.recursive = recursive;
	}

	@Override
	public String toString() {
		return transaction + (recursive ? " recursively revolved" : " non recursively resolved");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Classpath && ((Classpath) other).transaction.equals(transaction) && ((Classpath) other).recursive == recursive;
	}

	@Override
	public int hashCode() {
		return transaction.hashCode();
	}
}