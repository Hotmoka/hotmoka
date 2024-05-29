/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
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

import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.takamaka.code.constants.Constants;

/**
 * An extractor of the updates to the state reachable, in RAM, from some storage objects.
 * This extractor is used after a transaction, to collect the updates to fields
 * that have changed their value during the transaction.
 */
public class UpdatesExtractor {

	/**
	 * The class loader that loaded the objects.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * Builds an extractor of the updates to the state reachable from some storage objects.
	 * 
	 * @param classLoader the class loader used to load the objects later passed to {@link #extractUpdatesFrom(Stream)}
	 */
	UpdatesExtractor(EngineClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Yields the updates extracted from the given storage objects and from the objects
	 * reachable from them, recursively.
	 * 
	 * @param objects the storage objects whose updates must be computed (for them and
	 *                for the objects recursively reachable from them); this must have been
	 *                loaded with the class loader provided to the constructor and to be
	 *                instances of {@code io.takamaka.code.lang.Storage}
	 * @return the updates, sorted
	 * @throws UpdatesExtractionException if the updates cannot be extracted, because for instance an illegal
	 *                                    value has been stored into some field
	 * @throws StoreException if the operation cannot be completed
	 */
	public Stream<Update> extractUpdatesFrom(Stream<Object> objects) throws UpdatesExtractionException, StoreException {
		return new Processor(objects).updates.stream();
	}

	/**
	 * Internal scope for extracting the updates to some objects.
	 */
	private class Processor {

		/**
		 * The set of objects to process. This gets expanded as soon as new objects are found to be reachable.
		 */
		private final List<Object> workingSet;

		/**
		 * The set of all objects processed so far. This is needed to avoid processing the same object twice.
		 */
		private final Set<StorageReference> seen = new HashSet<>();

		/**
		 * The extracted updates so far.
		 */
		private final SortedSet<Update> updates = new TreeSet<>();

		/**
		 * Builds an internal scope to extract the updates to the given objects,
		 * and to those reachable from them, recursively.
		 * 
		 * @param objects the storage objects whose updates must be computed (for them and
		 *                for the objects recursively reachable from them)
		 * @throws UpdatesExtractionException if the updates cannot be extracted, because for instance an illegal
		 *                                    value has been stored into some field
		 * @throws StoreException if the operation cannot be completed
		 */
		private Processor(Stream<Object> objects) throws UpdatesExtractionException, StoreException {
			this.workingSet = objects
				.filter(object -> seen.add(classLoader.getStorageReferenceOf(object)))
				.collect(Collectors.toList());

			do {
				// removes the next storage object to scan for updates and continues recursively
				// with the objects that can be reached from it, until no new object can be reached
				new ExtractedUpdatesSingleObject(workingSet.remove(workingSet.size() - 1));
			}
			while (!workingSet.isEmpty());
		}

		/**
		 * The internal scope to extract the updates to a given object.
		 */
		private class ExtractedUpdatesSingleObject {

			/**
			 * The reference of the object.
			 */
			private final StorageReference storageReference;

			/**
			 * True if and only if the object was already in storage.
			 */
			private final boolean inStorage;

			/**
			 * Builds the scope to extract the updates to a given object.
			 * 
			 * @param object the object
			 * @throws UpdatesExtractionException if the updates cannot be extracted, because for instance an illegal
			 *                                    value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private ExtractedUpdatesSingleObject(Object object) throws UpdatesExtractionException, StoreException {
				Class<?> clazz = object.getClass();
				this.storageReference = classLoader.getStorageReferenceOf(object);
				this.inStorage = classLoader.getInStorageOf(object);

				if (!inStorage)
					updates.add(Updates.classTag(storageReference, StorageTypes.classOf(clazz), classLoader.transactionThatInstalledJarFor(clazz)));

				Class<?> previous = null;
				while (previous != classLoader.getStorage()) {
					addUpdatesForFieldsDefinedInClass(clazz, object);
					previous = clazz;
					if (clazz == null)
						// the objects where expected to be instances of io.takamaka.code.lang.Storage
						throw new StoreException("Cannot extract the updates of an object that is not subclass of " + Constants.STORAGE_NAME);

					clazz = clazz.getSuperclass();
				}
			}

			/**
			 * Adds to the set of updates the one stating that a field has been assigned to a new value.
			 * 
			 * @param fieldDefiningClass the class of the field; this can only be the class of the storage object defining the field or one of its superclasses
			 * @param fieldName the name of the field
			 * @param fieldClassName the name of the type of the field
			 * @param newValue the value set to the field
			 * @throws UpdatesExtractionException if the updates cannot be extracted, because for instance an illegal
			 *                                    value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String fieldClassName, Object newValue) throws UpdatesExtractionException, StoreException {
				var field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.classNamed(fieldClassName));

				if (newValue == null)
					// the field has been set to null
					updates.add(Updates.toNull(storageReference, field, false));
				else if (classLoader.getStorage().isAssignableFrom(newValue.getClass())) {
					// the field has been set to a storage object
					var storageReference2 = classLoader.getStorageReferenceOf(newValue);
					updates.add(Updates.ofStorage(storageReference, field, storageReference2));

					// if the new value has not yet been considered, we put in the list of object still to be processed
					if (seen.add(storageReference2))
						workingSet.add(newValue);
				}
				// the following cases occur if the declared type of the field is Object but it is updated
				// to an object whose type is allowed in storage
				else if (newValue instanceof String s)
					updates.add(Updates.ofString(storageReference, field, s));
				else if (newValue instanceof BigInteger bi)
					updates.add(Updates.ofBigInteger(storageReference, field, bi));
				else if (newValue instanceof Enum<?> e) {
					var clazz = e.getClass();
					if (hasInstanceFields(clazz))
						throw new UpdatesExtractionException("Field " + field + " of a storage object cannot hold an enumeration of class " + clazz.getName() + ": it has instance non-transient fields");

					updates.add(Updates.ofEnum(storageReference, field, clazz.getName(), e.name(), false));
				}
				else
					throw new UpdatesExtractionException("Field " + field + " of a storage object cannot hold a " + newValue.getClass().getName()); // TODO: OK
			}

			/**
			 * Determines if the given enumeration type has at least an instance, non-transient field.
			 * 
			 * @param clazz the class
			 * @return true only if that condition holds
			 * @throws StoreException if the operation cannot be completed
			 */
			private static boolean hasInstanceFields(Class<?> clazz) throws StoreException {
				try {
					return Stream.of(clazz.getDeclaredFields())
							.map(Field::getModifiers)
							.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
				}
				catch (SecurityException e) {
					throw new StoreException(e);
				}
			}

			/**
			 * Takes note that a field of {@code boolean} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, boolean s) {
				updates.add(Updates.ofBoolean(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BOOLEAN), s));
			}

			/**
			 * Takes note that a field of {@code byte} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, byte s) {
				updates.add(Updates.ofByte(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BYTE), s));
			}

			/**
			 * Takes note that a field of {@code char} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, char s) {
				updates.add(Updates.ofChar(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.CHAR), s));
			}

			/**
			 * Takes note that a field of {@code double} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, double s) {
				updates.add(Updates.ofDouble(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.DOUBLE), s));
			}

			/**
			 * Takes note that a field of {@code float} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, float s) {
				updates.add(Updates.ofFloat(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.FLOAT), s));
			}

			/**
			 * Takes note that a field of {@code int} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, int s) {
				updates.add(Updates.ofInt(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.INT), s));
			}

			/**
			 * Takes note that a field of {@code long} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, long s) {
				updates.add(Updates.ofLong(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.LONG), s));
			}

			/**
			 * Takes note that a field of {@code short} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param s the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, short s) {
				updates.add(Updates.ofShort(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.SHORT), s));
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
					updates.add(Updates.toNull(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.STRING), true));
				else
					updates.add(Updates.ofString(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.STRING), s));
			}

			/**
			 * Takes note that a field of {@link java.math.BigInteger} type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param bi the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, BigInteger bi) {
				FieldSignature field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BIG_INTEGER);
				if (bi == null)
					updates.add(Updates.toNull(storageReference, field, true));
				else
					updates.add(Updates.ofBigInteger(storageReference, field, bi));
			}

			/**
			 * Takes note that a field of enumeration type has changed its value and consequently adds it to the set of updates.
			 * 
			 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
			 * @param fieldName the name of the field
			 * @param fieldClassName the name of the type of the field
			 * @param element the value set to the field
			 */
			private void addUpdateFor(String fieldDefiningClass, String fieldName, String fieldClassName, Enum<?> element) {
				FieldSignature field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.classNamed(fieldClassName));
				if (element == null)
					updates.add(Updates.toNull(storageReference, field, true));
				else
					updates.add(Updates.ofEnum(storageReference, field, element.getClass().getName(), element.name(), true));
			}

			/**
			 * Takes note of updates to the fields of the given object, defined in the given class.
			 * 
			 * @param clazz the class
			 * @param object the object
			 * @throws UpdatesExtractionException if the updates cannot be extracted, because for instance an illegal
			 *                                    value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private void addUpdatesForFieldsDefinedInClass(Class<?> clazz, Object object) throws UpdatesExtractionException, StoreException {
				Field[] declaredFields;

				try {
					declaredFields = clazz.getDeclaredFields();
				}
				catch (SecurityException e) {
					// the class loader is the same used to load clazz: this exception should be impossible
					throw new StoreException("Cannot access the fields defined in class " + clazz.getName(), e);
				}

				for (Field field: declaredFields)
					if (!isStaticOrTransient(field)) {
						try {
							field.setAccessible(true); // it might be private
						}
						catch (SecurityException | InaccessibleObjectException e) {
							throw new StoreException("Cannot make field " + field.getDeclaringClass().getName() + "." + field.getName() + " accessible", e);
						}

						Object currentValue, oldValue;

						try {
							currentValue = field.get(object);
						}
						catch (IllegalArgumentException | IllegalAccessException | ExceptionInInitializerError e) {
							throw new StoreException("Cannot access field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
						}

						String oldName = InstrumentationFields.OLD_PREFIX + field.getName();
						try {
							Field oldField = clazz.getDeclaredField(oldName);
							oldField.setAccessible(true); // it is always private
							oldValue = oldField.get(object);
						}
						catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | InaccessibleObjectException | ExceptionInInitializerError e) {
							throw new StoreException("Cannot access the old value of field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
						}

						if (!inStorage || !Objects.equals(oldValue, currentValue))
							addUpdateFor(field, currentValue);

						if (oldValue != null && inStorage && classLoader.isLazilyLoaded(field.getType()))
							recursiveExtract(oldValue);
					}
			}

			/**
			 * Recurs on the old value of the fields of reference type.
			 * 
			 * @param object the storage object whose fields are considered
			 * @throws StoreException if the operation cannot be completed
			 */
			private void recursiveExtract(Object object) throws StoreException {
				Class<?> clazz = object.getClass();
				if (classLoader.getStorage().isAssignableFrom(clazz)) {
					if (seen.add(classLoader.getStorageReferenceOf(object)))
						workingSet.add(object);
				}
				else if (classLoader.isLazilyLoaded(clazz)) // eager types are not recursively followed
					// there was an illegal value in this field: this should never happen
					throw new StoreException("A field of a storage object cannot hold a " + clazz.getName());
			}

			/**
			 * Yields the update that represents the fact that a field has been updated to a new value.
			 * 
			 * @param field the field
			 * @param newValue the new value of the field
			 * @throws UpdatesExtractionException if the updates cannot be extracted, because for instance an illegal
			 *                                    value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private void addUpdateFor(Field field, Object newValue) throws UpdatesExtractionException, StoreException {
				Class<?> fieldType = field.getType();
				String fieldDefiningClass = field.getDeclaringClass().getName();
				String fieldName = field.getName();

				if (fieldType == char.class)
					addUpdateFor(fieldDefiningClass, fieldName, (char) newValue);
				else if (fieldType == boolean.class)
					addUpdateFor(fieldDefiningClass, fieldName, (boolean) newValue);
				else if (fieldType == byte.class)
					addUpdateFor(fieldDefiningClass, fieldName, (byte) newValue);
				else if (fieldType == short.class)
					addUpdateFor(fieldDefiningClass, fieldName, (short) newValue);
				else if (fieldType == int.class)
					addUpdateFor(fieldDefiningClass, fieldName, (int) newValue);
				else if (fieldType == long.class)
					addUpdateFor(fieldDefiningClass, fieldName, (long) newValue);
				else if (fieldType == float.class)
					addUpdateFor(fieldDefiningClass, fieldName, (float) newValue);
				else if (fieldType == double.class)
					addUpdateFor(fieldDefiningClass, fieldName, (double) newValue);
				else if (fieldType == BigInteger.class)
					addUpdateFor(fieldDefiningClass, fieldName, (BigInteger) newValue);
				else if (fieldType == String.class)
					addUpdateFor(fieldDefiningClass, fieldName, (String) newValue);
				else if (fieldType.isEnum())
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), (Enum<?>) newValue);
				else if (classLoader.isLazilyLoaded(fieldType))
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), newValue);
				else
					// for instance, arrays: they should have been forbidden when verifying the installed jars
					throw new StoreException("Unexpected type " + fieldType.getName() + " for a field of a storage object: " + fieldDefiningClass + '.' + fieldName);
			}

			/**
			 * Determines if the given field is static or transient, hence its updates are not extracted.
			 * 
			 * @param field the field
			 * @return true if and only if that condition holds
			 */
			private static boolean isStaticOrTransient(Field field) {
				int modifiers = field.getModifiers();
				return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
			}
		}
	}
}