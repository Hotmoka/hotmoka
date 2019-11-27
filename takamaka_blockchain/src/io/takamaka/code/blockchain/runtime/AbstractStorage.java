package io.takamaka.code.blockchain.runtime;

import java.util.List;
import java.util.Set;

import io.takamaka.code.blockchain.ClassTag;
import io.takamaka.code.blockchain.Update;
import io.takamaka.code.blockchain.values.StorageReference;

/**
 * This class will be set as superclass of {@code io.takamaka.code.lang.Storage}
 * by instrumentation. This way, the methods and fields in this class do not
 * appear in IDE as suggestions inside storage classes. Moreover, this class
 * can be used in this module without importing the runtime of Takamaka.
 */
public abstract class AbstractStorage {

	/**
	 * The abstract pointer used to refer to this object in blockchain.
	 */
	public final StorageReference storageReference;

	/**
	 * True if the object reflects an object serialized in blockchain.
	 * False otherwise. The latter case occurs if the object has been
	 * created during the current transaction but has not been yet
	 * serialized into blockchain.
	 */
	protected final boolean inStorage;

	/**
	 * Constructs an object that can be stored in blockchain.
	 */
	protected AbstractStorage() {
		// when the object is first created, it is not yet in blockchain
		this.inStorage = false;

		// assigns a fresh unique identifier to the object, that will later
		// be used to refer to the object once serialized in blockchain
		this.storageReference = StorageReference.mk(Runtime.getBlockchain().getCurrentTransaction(), Runtime.generateNextProgressive());
	}

	/**
	 * Constructor used for deserialization from blockchain, in instrumented code.
	 * 
	 * @param storageReference the reference to deserialize
	 */
	protected AbstractStorage(StorageReference storageReference) {
		// this object reflects something already in blockchain
		this.inStorage = true;

		// the storage reference of this object must be the same used in blockchain
		this.storageReference = storageReference;
	}

	/**
	 * Collects the updates to this object and to those reachable from it.
	 * The instrumentation of storage classes redefines this to include updates to all their fields.
	 * 
	 * @param updates the set where storage updates will be collected
	 * @param seen the storage references of the objects already considered during the scan of the storage objects
	 * @param workingSet the list of storage objects that still need to be processed. This can get enlarged by a call to this method,
	 *                   in order to simulate recursive calls without risking a Java stack overflow
	 */
	//TODO: this method might conflict with methods in subclasses
	public void extractUpdates(Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet) {
		if (!inStorage)
			updates.add(new ClassTag(storageReference, getClass().getName(), Runtime.getBlockchain().transactionThatInstalledJarFor(getClass())));

		// subclasses will override, call this super-implementation and add potential updates to their instance fields
	}
}