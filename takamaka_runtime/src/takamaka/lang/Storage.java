package takamaka.lang;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import takamaka.blockchain.FieldSignature;
import takamaka.blockchain.Update;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.ByteValue;
import takamaka.blockchain.values.CharValue;
import takamaka.blockchain.values.DoubleValue;
import takamaka.blockchain.values.FloatValue;
import takamaka.blockchain.values.GenericStorageReference;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.NullValue;
import takamaka.blockchain.values.ShortValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageReferenceInCurrentTransaction;
import takamaka.blockchain.values.StringValue;

/**
 * The superclass of classes whose objects can be kept in blockchain.
 * A storage class can only have fields of types allowed in blockchain.
 * Its updates are stored in blockchain at the end of the execution of a transaction.
 */
public abstract class Storage {

	/**
	 * The abstract pointer used to refer to this object in blockchain.
	 */
	public final StorageReference storageReference;

	/**
	 * True if the object reflects an object serialized in blockchain.
	 * False otherwise. The latter case occurs if the object has been
	 * created with the {@code new} statement but has not yet been
	 * serialized into blockchain.
	 */
	protected final boolean inStorage;

	/**
	 * Constructs an object that can be stored in blockchain.
	 */
	@WhiteListed
	protected Storage() {
		// when the object is first created, it is not yet in blockchain
		this.inStorage = false;

		// assigns a fresh unique identifier to the object, that will later
		// used to refer to the object once serialized in blockchain
		this.storageReference = new StorageReferenceInCurrentTransaction(Takamaka.generateNextProgressive());
	}

	// ALL SUBSEQUENT METHODS ARE USED IN INSTRUMENTED CODE

	/**
	 * Collects the updates to this object and to the objects that are reachable from it.
	 * This is used at the end of a transaction, to collect and then store the updates
	 * resulting from the transaction.
	 * 
	 * @param result the set where the updates will be added
	 * @param seen a set of storage references that have already been scanned
	 */
	public final void updates(Set<Update> result, Set<StorageReference> seen) {
		if (seen.add(storageReference)) {
			// the set of storage objects that we have to scan
			List<Storage> workingSet = new ArrayList<>(16);
			// initially, there is only this object to scan
			workingSet.add(this);

			do {
				// removes the next storage object to scan for updates and continues
				// recursively with the objects that can be reached from it, until
				// no new object can be reached
				workingSet.remove(workingSet.size() - 1).extractUpdates(result, seen, workingSet);
			}
			while (!workingSet.isEmpty());
		}
	}

	/**
	 * Constructor used for deserialization from blockchain, in instrumented code.
	 * 
	 * @param storageReference the reference to deserialize
	 */
	protected Storage(GenericStorageReference storageReference) {
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
	protected void extractUpdates(Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet) {
		if (!inStorage)
			updates.add(Update.mkForClassTag(storageReference, getClass().getName()));

		// subclasses will override, call this super-implementation and add potential updates to their instance fields
	}

	/**
	 * Utility method that will be used in subclasses to implement redefinitions of
	 * {@link takamaka.lang.Storage#extractUpdates(Set, Set, List)} to recur on the old value of fields of reference type.
	 * 
	 * @param s the storage objects whose fields are considered
	 * @param updates the set where updates are added
	 * @param seen the set of storage references already scanned
	 * @param workingSet the set of storage objects that still need to be processed
	 */
	protected final void recursiveExtract(Object s, Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet) {
		if (s instanceof Storage) {
			if (seen.add(((Storage) s).storageReference))
				workingSet.add((Storage) s);
		}
		else if (s instanceof String || s instanceof BigInteger) {} // these types are not recursively followed
		else if (s != null)
			throw new IllegalStateException("a field of a storage object cannot hold a " + s.getClass().getName());
	}

	/**
	 * Yields the last value assigned to the given lazy field of this storage object.
	 * 
	 * @param definingClass the class of the field. This can only be the class of this storage object
	 *                      of one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws Exception if the value could not be found
	 */
	protected final Object deserializeLastLazyUpdateFor(String definingClass, String name, String fieldClassName) throws Exception {
		return Takamaka.getBlockchain().deserializeLastLazyUpdateFor((GenericStorageReference) storageReference, new FieldSignature(definingClass, name, fieldClassName));
	}

	/**
	 * Takes note that a field of reference type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param seen the set of storage references already processed
	 * @param workingSet the set of storage objects that still need to be processed
	 * @param fieldClassName the name of the type of the field
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet, String fieldClassName, Object s) {
		// these values are not recursively followed
		FieldSignature field = new FieldSignature(fieldDefiningClass, fieldName, fieldClassName);

		if (s == null)
			//the field has been set to null
			updates.add(new Update(storageReference, field, NullValue.INSTANCE));
		else if (s instanceof Storage) {
			// the field has been set to a storage object
			Storage storage = (Storage) s;
			updates.add(new Update(storageReference, field, storage.storageReference));

			// if the new value has not yet been consider, we put in the list of object still to be processed
			if (seen.add(storage.storageReference))
				workingSet.add(storage);
		}
		else
			throw new IllegalStateException("field " + field + " of a storage class cannot hold a " + s.getClass().getName());
	}

	/**
	 * Takes note that a field of {@code boolean} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, boolean s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.BOOLEAN), new BooleanValue(s)));
	}

	/**
	 * Takes note that a field of {@code byte} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, byte s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.BYTE), new ByteValue(s)));
	}

	/**
	 * Takes note that a field of {@code char} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, char s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.CHAR), new CharValue(s)));
	}

	/**
	 * Takes note that a field of {@code double} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, double s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.DOUBLE), new DoubleValue(s)));
	}

	/**
	 * Takes note that a field of {@code float} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, float s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.FLOAT), new FloatValue(s)));
	}

	/**
	 * Takes note that a field of {@code int} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, int s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.INT), new IntValue(s)));
	}

	/**
	 * Takes note that a field of {@code long} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, long s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.LONG), new LongValue(s)));
	}

	/**
	 * Takes note that a field of {@code short} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, short s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, BasicTypes.SHORT), new ShortValue(s)));
	}

	/**
	 * Takes note that a field of {@link java.lang.String} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, String s) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, ClassType.STRING), s == null ? NullValue.INSTANCE : new StringValue(s)));
	}

	/**
	 * Takes note that a field of {@link java.math.BigInteger} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param bi the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, BigInteger bi) {
		updates.add(new Update(storageReference, new FieldSignature(fieldDefiningClass, fieldName, ClassType.BIG_INTEGER), bi == null ? NullValue.INSTANCE : new BigIntegerValue(bi)));
	}
}