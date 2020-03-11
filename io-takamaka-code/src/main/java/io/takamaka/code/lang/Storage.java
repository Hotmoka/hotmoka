package io.takamaka.code.lang;

/**
 * The superclass of classes whose objects can be kept in blockchain.
 * A storage class can only have fields of types allowed in blockchain.
 * Its updates are stored in blockchain at the end of the execution of a transaction.
 */
public abstract class Storage {

	/**
	 * The abstract pointer used to refer to this object in blockchain.
	 * This will contain a {@link io.takamaka.code.blockchain.values.StorageReference}
	 * at run time. It is private so that it does not appear accessible in subclasses
	 * and will be accessed by reflection.
	 */
	@SuppressWarnings("unused")
	private transient Object storageReference;

	/**
	 * True if the object reflects an object serialized in blockchain.
	 * False otherwise. The latter case occurs if the object has been
	 * created during the current transaction but has not been yet
	 * serialized into blockchain. It is private so that it does not appear
	 * accessible in subclasses and will be accessed by reflection.
	 */
	@SuppressWarnings("unused")
	private transient boolean inStorage;

	/**
	 * Constructs an object that can be stored in blockchain.
	 */
	protected Storage() {
		// this constructor gets instrumented as follows:

		// when the object is first created, it is not yet in blockchain
		//this.inStorage = false;

		// assigns a fresh unique identifier to the object, that will later
		// be used to refer to the object once serialized in blockchain
		//this.storageReference = Runtime.getNextStorageReference();
	}

	/**
	 * Constructor used for deserialization from blockchain, in instrumented code.
	 * 
	 * @param storageReference the reference to deserialize
	 */
	// the following constructor gets added by instrumentation
	/*protected Storage(StorageReference storageReference) {
		// this object reflects something already in blockchain
		this.inStorage = true;

		// the storage reference of this object must be the same used in blockchain
		this.storageReference = storageReference;
	}*/

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
		//   return Runtime.compareStorageReferencesOf(this, other);
		// which works since this class is made subclass of AbstractStorage by instrumentation
		return 0;
	}
}