package io.takamaka.code.blockchain;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.signatures.FieldSignature;
import io.takamaka.code.blockchain.types.BasicTypes;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.updates.ClassTag;
import io.takamaka.code.blockchain.updates.Update;
import io.takamaka.code.blockchain.updates.UpdateOfBalance;
import io.takamaka.code.blockchain.updates.UpdateOfBigInteger;
import io.takamaka.code.blockchain.updates.UpdateOfBoolean;
import io.takamaka.code.blockchain.updates.UpdateOfByte;
import io.takamaka.code.blockchain.updates.UpdateOfChar;
import io.takamaka.code.blockchain.updates.UpdateOfDouble;
import io.takamaka.code.blockchain.updates.UpdateOfEnumEager;
import io.takamaka.code.blockchain.updates.UpdateOfEnumLazy;
import io.takamaka.code.blockchain.updates.UpdateOfFloat;
import io.takamaka.code.blockchain.updates.UpdateOfInt;
import io.takamaka.code.blockchain.updates.UpdateOfLong;
import io.takamaka.code.blockchain.updates.UpdateOfRedBalance;
import io.takamaka.code.blockchain.updates.UpdateOfShort;
import io.takamaka.code.blockchain.updates.UpdateOfStorage;
import io.takamaka.code.blockchain.updates.UpdateOfString;
import io.takamaka.code.blockchain.updates.UpdateToNullEager;
import io.takamaka.code.blockchain.updates.UpdateToNullLazy;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.instrumentation.Constants;

public class ExtractedUpdates {
	private final SortedSet<Update> updates = new TreeSet<>();

	/**
	 * Builds a collection of the updates to the given storage objects and to those reachable from them.
	 * 
	 * @param blockchain the blockchain for which the extraction is performed
	 * @param objects the storage objects whose updates must be computed
	 */
	public ExtractedUpdates(AbstractBlockchain blockchain, Stream<Object> objects) {
		new Builder(blockchain, objects);
	}

	/**
	 * Yields the updates computed by this object.
	 * 
	 * @return the updates
	 */
	public SortedSet<Update> getUpdates() {
		return updates;
	}

	private class Builder {
		private final AbstractBlockchain blockchain;
		private final List<Object> workingSet;
		private final Set<StorageReference> seen = new HashSet<>();

		private Builder(AbstractBlockchain blockchain, Stream<Object> objects) {
			this.blockchain = blockchain;
			this.workingSet = objects
				.filter(object -> seen.add(blockchain.getStorageReferenceOf(object)))
				.collect(Collectors.toList());

			do {
				// removes the next storage object to scan for updates and continues recursively
				// with the objects that can be reached from it, until no new object can be reached
				new ExtractedUpdatesSingleObject(workingSet.remove(workingSet.size() - 1));
			}
			while (!workingSet.isEmpty());
		}

		private class ExtractedUpdatesSingleObject {
			private final StorageReference storageReference;
			private final boolean inStorage;

			private ExtractedUpdatesSingleObject(Object object) {
				Class<?> clazz = object.getClass();
				this.storageReference = blockchain.getStorageReferenceOf(object);
				this.inStorage = blockchain.getInStorageOf(object);

				if (!inStorage)
					updates.add(new ClassTag(storageReference, clazz.getName(), blockchain.transactionThatInstalledJarFor(clazz)));

				while (clazz != blockchain.getStorage()) {
					addUpdatesForFieldsDefinedInClass(clazz, object);
					clazz = clazz.getSuperclass();
				}
			}

			/**
			 * Utility method called for update extraction to recur on the old value
			 * of fields of reference type.
			 * 
			 * @param s the storage objects whose fields are considered
			 */
			private void recursiveExtract(Object s) {
				if (s != null) {
					Class<?> clazz = s.getClass();
					if (blockchain.getStorage().isAssignableFrom(clazz)) {
						if (seen.add(blockchain.getStorageReferenceOf(s)))
							workingSet.add(s);
					}
					else if (blockchain.isLazilyLoaded(clazz)) // eager types are not recursively followed
						throw new DeserializationError("a field of a storage object cannot hold a " + clazz.getName());
				}
			}

			/**
			 * Takes note that a field of lazy type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object of one of its superclasses
			 * @param fieldName the name of the field
			 * @param fieldClassName the name of the type of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String fieldClassName, Object s) {
				FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.mk(fieldClassName));

				if (s == null)
					//the field has been set to null
					updates.add(new UpdateToNullLazy(storageReference, field));
				else if (blockchain.getStorage().isAssignableFrom(s.getClass())) {
					// the field has been set to a storage object
					StorageReference storageReference2 = blockchain.getStorageReferenceOf(s);
					updates.add(new UpdateOfStorage(storageReference, field, storageReference2));

					// if the new value has not yet been considered, we put in the list of object still to be processed
					if (seen.add(storageReference2))
						workingSet.add(s);
				}
				// the following cases occur if the declared type of the field is Object but it is updated
				// to an object whose type is allowed in storage
				else if (s instanceof String)
					updates.add(new UpdateOfString(storageReference, field, (String) s));
				else if (s instanceof BigInteger)
					updates.add(new UpdateOfBigInteger(storageReference, field, (BigInteger) s));
				else if (s instanceof Enum<?>) {
					if (hasInstanceFields(s.getClass()))
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
			private boolean hasInstanceFields(Class<?> clazz) {
				return Stream.of(clazz.getDeclaredFields())
					.map(Field::getModifiers)
					.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
			}

			/**
			 * Takes note that a field of {@code boolean} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, boolean s) {
				updates.add(new UpdateOfBoolean(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.BOOLEAN), s));
			}

			/**
			 * Takes note that a field of {@code byte} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, byte s) {
				updates.add(new UpdateOfByte(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.BYTE), s));
			}

			/**
			 * Takes note that a field of {@code char} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, char s) {
				updates.add(new UpdateOfChar(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.CHAR), s));
			}

			/**
			 * Takes note that a field of {@code double} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, double s) {
				updates.add(new UpdateOfDouble(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.DOUBLE), s));
			}

			/**
			 * Takes note that a field of {@code float} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, float s) {
				updates.add(new UpdateOfFloat(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.FLOAT), s));
			}

			/**
			 * Takes note that a field of {@code int} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, int s) {
				updates.add(new UpdateOfInt(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.INT), s));
			}

			/**
			 * Takes note that a field of {@code long} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, long s) {
				updates.add(new UpdateOfLong(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.LONG), s));
			}

			/**
			 * Takes note that a field of {@code short} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, short s) {
				updates.add(new UpdateOfShort(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.SHORT), s));
			}

			/**
			 * Takes note that a field of {@link java.lang.String} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String s) {
				if (s == null)
					updates.add(new UpdateToNullEager(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.STRING)));
				else
					updates.add(new UpdateOfString(storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.STRING), s));
			}

			/**
			 * Takes note that a field of {@link java.math.BigInteger} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param bi the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, BigInteger bi) {
				FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.BIG_INTEGER);
				if (bi == null)
					updates.add(new UpdateToNullEager(storageReference, field));
				else if (field.equals(FieldSignature.BALANCE_FIELD))
					updates.add(new UpdateOfBalance(storageReference, bi));
				else if (field.equals(FieldSignature.RED_BALANCE_FIELD))
					updates.add(new UpdateOfRedBalance(storageReference, bi));
				else
					updates.add(new UpdateOfBigInteger(storageReference, field, bi));
			}

			/**
			 * Takes note that a field of {@code enum} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param fieldClassName the name of the type of the field
			 * @param element the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String fieldClassName, Enum<?> element) {
				FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.mk(fieldClassName));
				if (element == null)
					updates.add(new UpdateToNullEager(storageReference, field));
				else
					updates.add(new UpdateOfEnumEager(storageReference, field, element.getClass().getName(), element.name()));
			}

			private void addUpdatesForFieldsDefinedInClass(Class<?> clazz, Object object) {
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
							addUpdateFor(field, currentValue);

						if (inStorage && blockchain.isLazilyLoaded(field.getType()))
							recursiveExtract(oldValue);
					}
			}

			private void addUpdateFor(Field field, Object currentValue) {
				Class<?> fieldType = field.getType();
				String fieldDefiningClass = field.getDeclaringClass().getName();
				String fieldName = field.getName();

				if (fieldType == char.class)
					addUpdateFor(fieldDefiningClass, fieldName, (char) currentValue);
				else if (fieldType == boolean.class)
					addUpdateFor(fieldDefiningClass, fieldName, (boolean) currentValue);
				else if (fieldType == byte.class)
					addUpdateFor(fieldDefiningClass, fieldName, (byte) currentValue);
				else if (fieldType == short.class)
					addUpdateFor(fieldDefiningClass, fieldName, (short) currentValue);
				else if (fieldType == int.class)
					addUpdateFor(fieldDefiningClass, fieldName, (int) currentValue);
				else if (fieldType == long.class)
					addUpdateFor(fieldDefiningClass, fieldName, (long) currentValue);
				else if (fieldType == float.class)
					addUpdateFor(fieldDefiningClass, fieldName, (float) currentValue);
				else if (fieldType == double.class)
					addUpdateFor(fieldDefiningClass, fieldName, (double) currentValue);
				else if (fieldType == BigInteger.class)
					addUpdateFor(fieldDefiningClass, fieldName, (BigInteger) currentValue);
				else if (fieldType == String.class)
					addUpdateFor(fieldDefiningClass, fieldName, (String) currentValue);
				else if (fieldType.isEnum())
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), (Enum<?>) currentValue);
				else if (blockchain.isLazilyLoaded(fieldType))
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), currentValue);
				else
					throw new IllegalStateException("unexpected field in storage object: " + fieldDefiningClass + '.' + fieldName);
			}

			private boolean isStaticOrTransient(Field field) {
				int modifiers = field.getModifiers();
				return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
			}
		}
	}
}