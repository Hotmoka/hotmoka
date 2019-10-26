package io.takamaka.code.lang;

/**
 * The superclass of classes whose objects can be kept in blockchain.
 * A storage class can only have fields of types allowed in blockchain.
 * Its updates are stored in blockchain at the end of the execution of a transaction.
 */
public abstract class Storage {

	/**
	 * Constructs an object that can be stored in blockchain.
	 */
	protected Storage() {}

	@Override
	public String toString() {
		return "storage";
	}

	/**
	 * Implements a chronological order on storage objects.
	 * 
	 * @param other the other object that must be compared to this
	 * @return -1 if this object is older than {@code other}; 1 if {@code other}
	 *         is older than this object; 0 if they are the same object
	 */
	public int compareAge(Storage other) {
		return 0; // code will be provided by instrumentation
	}
}