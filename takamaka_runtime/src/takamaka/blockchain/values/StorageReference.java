package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.lang.Immutable;
import takamaka.lang.Storage;

/**
 * A storage reference is a reference to an object that lives in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */

@Immutable
public final class StorageReference implements StorageValue {

	/**
	 * The transaction that created the object.
	 */
	public final TransactionReference transaction;

	/**
	 * The progressive number of the object among those that have been created
	 * during the same transaction.
	 */
	public final BigInteger progressive;

	/**
	 * Builds a storage reference.
	 * 
	 * @param transaction The transaction that created the object.
	 * @param progressive The progressive number of the object among those that have been created
	 *                    during the same transaction.
	 */
	public StorageReference(TransactionReference transaction, BigInteger progressive) {
		this.transaction = transaction;
		this.progressive = progressive;
	}

	/**
	 * Builds a storage reference from a string. The format of the string is the
	 * same that would be returned by {@code toString()}. Hence
	 * {@code r.equals(new StorageReference(s.toString()))} holds for
	 * every {@code StorageReference r}.
	 * 
	 * @param blokchain the blockchain for which the reference is being created
	 * @param s the string
	 * @throws NumberFormatException if the format of the string does not correspond
	 *                               to a {@code StorageReference}
	 */
	public StorageReference(AbstractBlockchain blokchain, String s) throws NumberFormatException {
		int index;

		if (s == null || (index = s.indexOf('@')) < 0)
			throw new NumberFormatException("Illegal transaction reference format: " + s);

		String transactionPart = s.substring(0, index);
		String progressivePart = s.substring(index + 1);
		
		this.transaction = blokchain.mkTransactionReferenceFrom(transactionPart);
		this.progressive = new BigInteger(progressivePart);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference && ((StorageReference) other).progressive.equals(progressive) && ((StorageReference) other).transaction.equals(transaction);
	}

	@Override
	public int hashCode() {
		return progressive.hashCode() ^ transaction.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = transaction.compareTo(((StorageReference) other).transaction);
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReference) other).progressive);
	}

	@Override
	public String toString() {
		return String.format("%s@%s", transaction, progressive);
	}

	@Override
	public Storage deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) throws TransactionException {
		return blockchain.deserialize(classLoader, this);
	}
}