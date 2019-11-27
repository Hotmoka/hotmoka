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
	 * Implements an order on storage object that delegates to the
	 * {@code compareTo()} on the storage references of the compared objects.
	 * This method is guaranteed to implement a total order relation.
	 * 
	 * @param other the other object that must be compared to this
	 * @return the result of comparison the storage references of the two objects
	 */
	public final int compareByStorageReference(Storage other) {
		// the following actual code will be provided by instrumentation:
		//   storageReference.compareTo(other.storageReference)
		// which works since this class is made subclass of AbstractStorage by instrumentation
		return 0;
	}
}