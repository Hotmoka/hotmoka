package takamaka.blockchain;

import takamaka.lang.Immutable;

/**
 * A class path, that points to a given jar in the blockchain.
 */

@Immutable
public final class Classpath {

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

	/**
	 * Builds a class path from a string. The format of the string is the
	 * same that would be returned by {@link takamaka.blockchain.Classpath#toString()}. Hence
	 * {@code c.equals(new Classpath(c.toString()))} holds for every {@code Classpath c}.
	 * 
	 * @param blockchain the blockchain for which the class path is being created
	 * @param s the string
	 */
	public Classpath(AbstractBlockchain blockchain, String s) {
		int semicolonPos;
		if (s == null || (semicolonPos = s.indexOf(';')) < 0)
			throw new IllegalArgumentException("Illegal Classpath format: " + s);

		String transactionPart = s.substring(0, semicolonPos);
		String recursivePart = s.substring(semicolonPos + 1);

		this.transaction = blockchain.mkTransactionReferenceFrom(transactionPart);
		this.recursive = Boolean.parseBoolean(recursivePart);
	}

	@Override
	public String toString() {
		return String.format("%s;%b", transaction, recursive);
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