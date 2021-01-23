package io.takamaka.code.lang;

/**
 * The superclass of classes whose objects can be kept in store.
 * A storage class can only have fields of types allowed in store.
 * Its updates are saved in store at the end of the execution of a transaction.
 */
public abstract class Storage {

	/**
	 * The caller of the entry method or constructor currently
	 * being executed. This is set at the beginning of an entry and refers
	 * to the contract that called the entry.
	 */
	private transient Contract caller;

	/**
	 * The abstract pointer used to refer to this object in store.
	 * This will contain a {@code io.hotmoka.beans.values.StorageReference}
	 * at run time. It is private so that it does not appear accessible in subclasses
	 * and will be accessed by reflection.
	 */
	@SuppressWarnings("unused")
	private transient Object storageReference;

	/**
	 * True if the object reflects an object serialized in store.
	 * False otherwise. The latter case occurs if the object has been
	 * created during the current transaction but has not been yet
	 * serialized in store. It is private so that it does not appear
	 * accessible in subclasses and will be accessed by reflection.
	 */
	@SuppressWarnings("unused")
	private transient boolean inStorage;

	/**
	 * Constructs an object that can be kept in store.
	 */
	protected Storage() {
		// this constructor gets instrumented as follows:

		// when the object is first created, it is not yet in store
		//this.inStorage = false;

		// assigns a fresh unique identifier to the object, that will later
		// be used to refer to the object once serialized in store
		//this.storageReference = Runtime.getNextStorageReference();
	}

	

	// the following constructor gets added by instrumentation
	/*protected Storage(StorageReference storageReference) {
		// this object reflects something already in store
		this.inStorage = true;

		// the storage reference of this object must be the same used in blockchain
		this.storageReference = storageReference;
	}*/

	@Override
	public String toString() {
		return "storage";
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
		return 0;
	}
	
	/**
	 * Yields the caller of the entry currently being executed.
	 * 
	 * @return the caller
	 */
	protected final Contract caller() {
		return caller;
	}

	/**
	 * Called at the beginning of the instrumentation of a {@code @@FromContract} method or constructor.
	 * It sets the caller of the code. It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param caller the caller of the method or constructor
	 */
	@SuppressWarnings("unused")
	private void fromContract(Contract caller) {
		// the caller is always non-null in correctly instrumented Takamaka code;
		// however, we check it to avoid calls from illegal bytecode
		Takamaka.require(caller != null, "A @FromContract method or constructor cannot receive a null caller");
		this.caller = caller;
	}
}