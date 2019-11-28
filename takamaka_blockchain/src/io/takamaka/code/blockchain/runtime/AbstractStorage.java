package io.takamaka.code.blockchain.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.takamaka.code.blockchain.ClassTag;
import io.takamaka.code.blockchain.Update;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.instrumentation.Constants;

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

	public static void extractUpdates2(AbstractStorage object, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet) {
		Class<?> clazz = object.getClass();
		StorageReference storageReference = object.storageReference;
		boolean inStorage = object.inStorage;

		if (!inStorage)
			updates.add(new ClassTag(storageReference, clazz.getName(), Runtime.getBlockchain().transactionThatInstalledJarFor(clazz)));

		while (clazz != AbstractStorage.class) {
			addUpdatesForFieldsDefinedInClass(clazz, object, storageReference, inStorage, updates, seen, workingSet);
			clazz = clazz.getSuperclass();
		}
	}

	private static void addUpdatesForFieldsDefinedInClass(Class<?> clazz, AbstractStorage object, StorageReference storageReference, boolean inStorage, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet) {
		for (Field field: clazz.getDeclaredFields())
			if (!isStaticOrTransient(field)) {
				field.setAccessible(true); // it might be private
				Object currentValue, oldValue;

				try {
					currentValue = field.get(object);
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					throw new IllegalStateException("cannot access field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
				}

				String oldName = Constants.OLD_PREFIX + field.getName();
				try {
					Field oldField = field.getDeclaringClass().getDeclaredField(oldName);
					oldField.setAccessible(true); // it is always private
					oldValue = oldField.get(object);
				}
				catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					throw new IllegalStateException("cannot access old value for field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
				}

				if (!inStorage || !Objects.equals(oldValue, currentValue))
					addUpdateFor(field, storageReference, updates, seen, workingSet, currentValue);

				if (inStorage && Runtime.getBlockchain().isLazilyLoaded(field.getType()))
					Runtime.recursiveExtract(oldValue, updates, seen, workingSet);
			}
	}

	private static void addUpdateFor(Field field, StorageReference storageReference, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet, Object currentValue) {
		Class<?> fieldType = field.getType();
		String fieldDefiningClass = field.getDeclaringClass().getName();
		String fieldName = field.getName();

		if (fieldType == char.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (char) currentValue);
		else if (fieldType == boolean.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (boolean) currentValue);
		else if (fieldType == byte.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (byte) currentValue);
		else if (fieldType == short.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (short) currentValue);
		else if (fieldType == int.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (int) currentValue);
		else if (fieldType == long.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (long) currentValue);
		else if (fieldType == float.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (float) currentValue);
		else if (fieldType == double.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (double) currentValue);
		else if (fieldType == BigInteger.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (BigInteger) currentValue);
		else if (fieldType == String.class)
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, (String) currentValue);
		else if (fieldType.isEnum())
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, fieldType.getName(), (Enum<?>) currentValue);
		else if (Runtime.getBlockchain().isLazilyLoaded(fieldType))
			Runtime.addUpdateFor(storageReference, fieldDefiningClass, fieldName, updates, seen, workingSet, fieldType.getName(), currentValue);
		else
			throw new IllegalStateException("unexpected field in storage object: " + fieldDefiningClass + '.' + fieldName);
	}

	private static boolean isStaticOrTransient(Field field) {
		int modifiers = field.getModifiers();
		return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
	}
}