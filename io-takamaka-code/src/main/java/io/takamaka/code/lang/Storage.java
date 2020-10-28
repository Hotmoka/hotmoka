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
	 * True if the object can be passed as parameter from outside the node.
	 */
	private final boolean exported;

	/**
	 * Constructs an object that can be kept in store.
	 * 
	 * @param exported true if and only if the object cannot be passed
	 *                 as argument from outside the node
	 */
	protected Storage(boolean exported) {
		this.exported = exported;

		// this constructor gets instrumented as follows:

		// sets the exported flag
		// this.exported = exported;

		// when the object is first created, it is not yet in store
		//this.inStorage = false;

		// assigns a fresh unique identifier to the object, that will later
		// be used to refer to the object once serialized in store
		//this.storageReference = Runtime.getNextStorageReference();
	}

	/**
	 * Constructs an object that can be stored in blockchain
	 * and cannot be passed as argument from outside the node.
	 */
	protected Storage() {
		this(false);
	}

	// the following constructor gets added by instrumentation and is used for deserialization from store
	/*protected Storage(Object storageReference, boolean exported, Dummy dummy) {
		this.exported = exported;

		// this object reflects something already in store
		this.inStorage = true;

		// the storage reference of this object must be the same used in store
		this.storageReference = storageReference;
	}*/

	@Override
	public String toString() {
		return "storage";
	}

	/**
	 * Determines if this object can be passed as argument from outside the node.
	 * 
	 * @return true if and only if that condition holds
	 */
	public final boolean isExported() {
		return exported;
	}

	/**
	 * Yields the name of the class of this object.
	 * 
	 * @return the name of the class of this object
	 */
	public final @View String getClassName() {
		return getClass().getName();
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