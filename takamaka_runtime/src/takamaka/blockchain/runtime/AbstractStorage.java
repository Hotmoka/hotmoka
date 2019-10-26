package takamaka.blockchain.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import takamaka.blockchain.ClassTag;
import takamaka.blockchain.DeserializationError;
import takamaka.blockchain.FieldSignature;
import takamaka.blockchain.Update;
import takamaka.blockchain.UpdateOfBalance;
import takamaka.blockchain.UpdateOfBigInteger;
import takamaka.blockchain.UpdateOfBoolean;
import takamaka.blockchain.UpdateOfByte;
import takamaka.blockchain.UpdateOfChar;
import takamaka.blockchain.UpdateOfDouble;
import takamaka.blockchain.UpdateOfEnumEager;
import takamaka.blockchain.UpdateOfEnumLazy;
import takamaka.blockchain.UpdateOfFloat;
import takamaka.blockchain.UpdateOfInt;
import takamaka.blockchain.UpdateOfLong;
import takamaka.blockchain.UpdateOfShort;
import takamaka.blockchain.UpdateOfStorage;
import takamaka.blockchain.UpdateOfString;
import takamaka.blockchain.UpdateToNullEager;
import takamaka.blockchain.UpdateToNullLazy;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageReferenceAlreadyInBlockchain;
import takamaka.blockchain.values.StorageReferenceInCurrentTransaction;

/**
 * The superclass of classes whose objects can be kept in blockchain.
 * A storage class can only have fields of types allowed in blockchain.
 * Its updates are stored in blockchain at the end of the execution of a transaction.
 */
public abstract class AbstractStorage {

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
	protected AbstractStorage() {
		// when the object is first created, it is not yet in blockchain
		this.inStorage = false;

		// assigns a fresh unique identifier to the object, that will later
		// used to refer to the object once serialized in blockchain
		this.storageReference = new StorageReferenceInCurrentTransaction(AbstractTakamaka.generateNextProgressive());
	}

	/**
	 * Implements a chronological order on storage objects.
	 * 
	 * @param other the other object that must be compared to this
	 * @return -1 if this object is older than {@code other}; 1 if {@code other}
	 *         is older than this object; 0 if they are the same object
	 */
	protected final int compareAge(AbstractStorage other) {
		return storageReference.compareTo(other.storageReference);
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
			List<AbstractStorage> workingSet = new ArrayList<>(16);
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
	protected AbstractStorage(StorageReferenceAlreadyInBlockchain storageReference) {
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
	protected void extractUpdates(Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet) {
		if (!inStorage)
			updates.add(new ClassTag(storageReference, getClass().getName(), AbstractTakamaka.getBlockchain().transactionThatInstalledJarFor(getClass())));

		// subclasses will override, call this super-implementation and add potential updates to their instance fields
	}

	/**
	 * Utility method that will be used in subclasses to implement redefinitions of
	 * {@link takamaka.lang.AbstractStorage#extractUpdates(Set, Set, List)} to recur on the old value of fields of reference type.
	 * 
	 * @param s the storage objects whose fields are considered
	 * @param updates the set where updates are added
	 * @param seen the set of storage references already scanned
	 * @param workingSet the set of storage objects that still need to be processed
	 */
	protected final void recursiveExtract(Object s, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet) {
		if (s instanceof AbstractStorage) {
			if (seen.add(((AbstractStorage) s).storageReference))
				workingSet.add((AbstractStorage) s);
		}
		else if (s instanceof String || s instanceof BigInteger || s instanceof Enum<?>) {} // these types are not recursively followed
		else if (s != null)
			throw new DeserializationError("a field of a storage object cannot hold a " + s.getClass().getName());
	}

	/**
	 * Yields the last value assigned to the given lazy, non-{@code final} field of this storage object.
	 * 
	 * @param definingClass the class of the field. This can only be the class of this storage object
	 *                      of one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws Exception if the value could not be found
	 */
	protected final Object deserializeLastLazyUpdateFor(String definingClass, String name, String fieldClassName) throws Exception {
		return AbstractTakamaka.getBlockchain().deserializeLastLazyUpdateFor((StorageReferenceAlreadyInBlockchain) storageReference, FieldSignature.mk(definingClass, name, ClassType.mk(fieldClassName)));
	}

	/**
	 * Yields the last value assigned to the given lazy, {@code final} field of this storage object.
	 * 
	 * @param definingClass the class of the field. This can only be the class of this storage object
	 *                      of one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws Exception if the value could not be found
	 */
	protected final Object deserializeLastLazyUpdateForFinal(String definingClass, String name, String fieldClassName) throws Exception {
		return AbstractTakamaka.getBlockchain().deserializeLastLazyUpdateForFinal((StorageReferenceAlreadyInBlockchain) storageReference, FieldSignature.mk(definingClass, name, ClassType.mk(fieldClassName)));
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
	@SuppressWarnings("unchecked")
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet, String fieldClassName, Object s) {
		// these values are not recursively followed
		FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.mk(fieldClassName));

		if (s == null)
			//the field has been set to null
			updates.add(new UpdateToNullLazy(storageReference, field));
		else if (s instanceof AbstractStorage) {
			// the field has been set to a storage object
			AbstractStorage storage = (AbstractStorage) s;
			updates.add(new UpdateOfStorage(storageReference, field, storage.storageReference));

			// if the new value has not yet been considered, we put in the list of object still to be processed
			if (seen.add(storage.storageReference))
				workingSet.add(storage);
		}
		// the following cases occur if the declared type of the field is Object but it is updated
		// to an object whose type is allowed in storage
		else if (s instanceof String)
			updates.add(new UpdateOfString(storageReference, field, (String) s));
		else if (s instanceof BigInteger)
			updates.add(new UpdateOfBigInteger(storageReference, field, (BigInteger) s));
		else if (s instanceof Enum<?>) {
			if (hasInstanceFields((Class<? extends Enum<?>>) s.getClass()))
				throw new DeserializationError("field " + field + " of a storage object cannot hold an enumeration of class " + s.getClass().getName() + ": it has instance non-transient fields");

			updates.add(new UpdateOfEnumLazy(storageReference, field, s.getClass().getName(), ((Enum<?>) s).name()));
		}
		else
			throw new DeserializationError("field " + field + " of a storage object cannot hold a " + s.getClass().getName());
	}

	/**
	 * Determines if the given enumeration type has at least an instance, non-transient field.
	 * 
	 * @param clazz the class
	 * @return true only if that condition holds
	 */
	private boolean hasInstanceFields(Class<? extends Enum<?>> clazz) {
		return Stream.of(clazz.getDeclaredFields())
			.map(Field::getModifiers)
			.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
	}

	/**
	 * Takes note that a field of {@code boolean} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, boolean s) {
		updates.add(new UpdateOfBoolean(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.BOOLEAN), s));
	}

	/**
	 * Takes note that a field of {@code byte} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, byte s) {
		updates.add(new UpdateOfByte(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.BYTE), s));
	}

	/**
	 * Takes note that a field of {@code char} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, char s) {
		updates.add(new UpdateOfChar(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.CHAR), s));
	}

	/**
	 * Takes note that a field of {@code double} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, double s) {
		updates.add(new UpdateOfDouble(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.DOUBLE), s));
	}

	/**
	 * Takes note that a field of {@code float} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, float s) {
		updates.add(new UpdateOfFloat(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.FLOAT), s));
	}

	/**
	 * Takes note that a field of {@code int} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, int s) {
		updates.add(new UpdateOfInt(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.INT), s));
	}

	/**
	 * Takes note that a field of {@code long} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, long s) {
		updates.add(new UpdateOfLong(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.LONG), s));
	}

	/**
	 * Takes note that a field of {@code short} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, short s) {
		updates.add(new UpdateOfShort(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.SHORT), s));
	}

	/**
	 * Takes note that a field of {@link java.lang.String} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, String s) {
		if (s == null)
			updates.add(new UpdateToNullEager(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.STRING)));
		else
			updates.add(new UpdateOfString(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.STRING), s));
	}

	/**
	 * Takes note that a field of {@link java.math.BigInteger} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param bi the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, BigInteger bi) {
		FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.BIG_INTEGER);
		if (bi == null)
			updates.add(new UpdateToNullEager(storageReference, field));
		else if (field.equals(FieldSignature.BALANCE_FIELD))
			updates.add(new UpdateOfBalance(storageReference, bi));
		else
			updates.add(new UpdateOfBigInteger(storageReference, field, bi));
	}

	/**
	 * Takes note that a field of {@code enum} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object
	 *                           or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param fieldClassName the name of the type of the field
	 * @param element the value set to the field
	 */
	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, String fieldClassName, Enum<?> element) {
		FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.mk(fieldClassName));
		if (element == null)
			updates.add(new UpdateToNullEager(storageReference, field));
		else
			updates.add(new UpdateOfEnumEager(storageReference, field, element.getClass().getName(), element.name()));
	}
}