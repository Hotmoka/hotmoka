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
	public Classpath(TransactionReference transaction, boolean recursive) {
		this.transaction = transaction;
		this.recursive = recursive;
	}

	/**
	 * Builds a classpath from a string. The format of the string is the
	 * same that would be returned by {@code toString()}. Hence
	 * {@code c.equals(new Classpath(c.toString()))} holds for every {@code Classpath c}.
	 * 
	 * @param s the string
	 * @throws NumberFormatException if the format of the string does not correspond
	 *                               to a {@code Classpath}
	 */
	public Classpath(String s) throws NumberFormatException {
		if (s == null || s.length() <= 21 || s.charAt(20) != ';')
			throw new NumberFormatException("Illegal Classpath format: " + s);

		String transactionPart = s.substring(0, 20);
		String recursivePart = s.substring(20);

		this.transaction = new TransactionReference(transactionPart);
		this.recursive = Boolean.getBoolean(recursivePart);
	}

	@Override
	public String toString() {
		return String.format("%s;%b", transaction, recursive);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Classpath && ((Classpath) other).transaction == transaction && ((Classpath) other).recursive == recursive;
	}

	@Override
	public int hashCode() {
		return transaction.hashCode();
	}
}