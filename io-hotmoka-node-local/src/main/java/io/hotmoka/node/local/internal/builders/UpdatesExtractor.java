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

package io.hotmoka.node.local.internal.builders;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.CodeExecutionTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
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

	private final ExecutionEnvironment environment;

	private final TransactionRequest<?> request;

	/**
	 * Builds an extractor of the updates to the state reachable from some storage objects.
	 * 
	 * @param classLoader the class loader used to load the objects later passed to {@link #extractUpdatesFrom(Iterable)}
	 */
	public UpdatesExtractor(EngineClassLoader classLoader, ExecutionEnvironment environment, TransactionRequest<?> request) {
		this.classLoader = classLoader;
		this.environment = environment;
		this.request = request;
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
	 * @throws IllegalAssignmentToFieldInStorageException if the updates cannot be extracted, because an illegal
	 *                                           value has been stored into some field
	 * @throws StoreException if the operation cannot be completed
	 */
	Stream<Update> extractUpdatesFrom(Iterable<Object> objects) throws IllegalAssignmentToFieldInStorageException, StoreException {
		return new Processor(objects).updates.stream();
	}

	/**
	 * Internal scope for extracting the updates to some objects.
	 */
	private class Processor {

		/**
		 * The set of objects to process. This gets expanded as soon as new objects are found to be reachable.
		 */
		private final List<Object> workingSet = new ArrayList<>();

		/**
		 * The set of all objects processed so far. This is needed to avoid processing the same object twice.
		 */
		private final Set<StorageReference> seen = new HashSet<>();

		/**
		 * The extracted updates so far.
		 */
		private final SortedSet<Update> updates = new TreeSet<>();

		/**
		 * Builds an internal scope to extract the updates to the given objects
		 * and to those reachable from them, recursively.
		 * 
		 * @param objects the storage objects whose updates must be computed (for them and
		 *                for the objects recursively reachable from them)
		 * @throws IllegalAssignmentToFieldInStorageException if the updates cannot be extracted, because an illegal
		 *                                           value has been stored into some field
		 * @throws StoreException if the operation cannot be completed
		 */
		private Processor(Iterable<Object> objects) throws IllegalAssignmentToFieldInStorageException, StoreException {
			for (var object: objects)
				if (seen.add(classLoader.getStorageReferenceOf(object, StoreException::new)))
					workingSet.add(object);

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
			 * The classpath used for creating the object.
			 */
			private final TransactionReference classpathAtCreationTimeOfObject;

			/**
			 * Builds the scope to extract the updates to a given storage object.
			 * 
			 * @param object the storage object
			 * @throws IllegalAssignmentToFieldInStorageException if the updates cannot be extracted, because an illegal
			 *                                           value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private ExtractedUpdatesSingleObject(Object object) throws IllegalAssignmentToFieldInStorageException, StoreException {
				Class<?> clazz = object.getClass();
				this.storageReference = classLoader.getStorageReferenceOf(object, StoreException::new);
				this.classpathAtCreationTimeOfObject = getClasspathAtCreationTimeOf(storageReference);
				this.inStorage = classLoader.getInStorageOf(object, StoreException::new);

				if (!inStorage)
					// storage objects can only have class type, hence the conversion must succeed
					updates.add(Updates.classTag(storageReference, StorageTypes.classFromClass(clazz),
						classLoader.transactionThatInstalledJarFor(clazz)
							.orElseThrow(() -> new StoreException("Object " + storageReference + " is in store, therefore it must have been installed in the store with a jar"))));

				Class<?> previous = null;
				var storage = classLoader.getStorage();
				while (previous != storage) {
					if (clazz == null)
						// the objects where expected to be instances of io.takamaka.code.lang.Storage
						throw new StoreException("Cannot extract the updates of an object that is not subclass of " + Constants.STORAGE_NAME);

					addUpdatesForFieldsDefinedInClass(clazz, object);
					previous = clazz;
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
			 * @throws IllegalAssignmentToFieldInStorageException if the updates cannot be extracted, because an illegal value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private void addUpdateFor(ClassType fieldDefiningClass, String fieldName, String fieldClassName, Object newValue) throws IllegalAssignmentToFieldInStorageException, StoreException {
				var field = FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.classNamed(fieldClassName));

				if (newValue == null)
					// the field has been set to null
					updates.add(Updates.toNull(storageReference, field, false));
				else if (classLoader.getStorage().isAssignableFrom(newValue.getClass())) {
					// the field has been set to a storage object
					var storageReferenceOfNewValue = classLoader.getStorageReferenceOf(newValue, StoreException::new);
					updates.add(Updates.ofStorage(storageReference, field, storageReferenceOfNewValue));

					TransactionReference classpathAtCreationOfNewValue = getClasspathAtCreationTimeOf(storageReferenceOfNewValue);

					if (!classLoader.includes(classpathAtCreationTimeOfObject, classpathAtCreationOfNewValue))
						throw new IllegalAssignmentToFieldInStorageException("Field " + field + " of "
								+ storageReference + " (created with classpath " + classpathAtCreationTimeOfObject + ") cannot hold a "
								+ newValue.getClass().getName() + " created with classpath " + classpathAtCreationOfNewValue + ": " + classpathAtCreationTimeOfObject + " does not include " + classpathAtCreationOfNewValue);

					// if the new value has not yet been considered, we put it in the list of object still to be processed
					if (seen.add(storageReferenceOfNewValue))
						workingSet.add(newValue);
				}
				// the following two cases occur if the declared type of the field is Object but it is updated
				// to an object whose type is allowed in storage
				else if (newValue instanceof String s)
					updates.add(Updates.ofString(storageReference, field, s));
				else if (newValue instanceof BigInteger bi)
					updates.add(Updates.ofBigInteger(storageReference, field, bi));
				else
					throw new IllegalAssignmentToFieldInStorageException("Field " + field + " of " + storageReference + " cannot hold a " + newValue.getClass().getName());
			}

			// TODO: add a component to the ClassTag of the object, so that we do not need to look for the classpath of the creation transaction of the objects
			private TransactionReference getClasspathAtCreationTimeOf(StorageReference storageReference) throws StoreException {
				TransactionRequest<?> request;
				try {
					request = environment.getRequest(storageReference.getTransaction());
				}
				catch (UnknownReferenceException e) {
					request = UpdatesExtractor.this.request;
				}

				if (request instanceof CodeExecutionTransactionRequest<?> cetr)
					return cetr.getClasspath();
				else if (request instanceof GameteCreationTransactionRequest gctr)
					return gctr.getClasspath();
				else
					throw new StoreException("Object " + storageReference + " has been unexpectedly created with a " + request.getClass().getName());
			}

			/**
			 * Takes note of the updates to the fields of the given object, defined in the given class.
			 * 
			 * @param clazz the class
			 * @param object the object
			 * @throws IllegalAssignmentToFieldInStorageException if the updates cannot be extracted, because an illegal
			 *                                           value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private void addUpdatesForFieldsDefinedInClass(Class<?> clazz, Object object) throws IllegalAssignmentToFieldInStorageException, StoreException {
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
					if (seen.add(classLoader.getStorageReferenceOf(object, StoreException::new)))
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
			 * @throws IllegalAssignmentToFieldInStorageException if the updates cannot be extracted, because an illegal value has been stored into some field
			 * @throws StoreException if the operation cannot be completed
			 */
			private void addUpdateFor(Field field, Object newValue) throws IllegalAssignmentToFieldInStorageException, StoreException {
				Class<?> fieldType = field.getType();
				// the field is defined in a storage object, hence the subsequent conversion cannot fail
				ClassType fieldDefiningClass = StorageTypes.classFromClass(field.getDeclaringClass());
				String fieldName = field.getName();

				if (fieldType == char.class)
					updates.add(Updates.ofChar(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.CHAR), (char) newValue));
				else if (fieldType == boolean.class)
					updates.add(Updates.ofBoolean(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BOOLEAN), (boolean) newValue));
				else if (fieldType == byte.class)
					updates.add(Updates.ofByte(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BYTE), (byte) newValue));
				else if (fieldType == short.class)
					updates.add(Updates.ofShort(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.SHORT), (short) newValue));
				else if (fieldType == int.class)
					updates.add(Updates.ofInt(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.INT), (int) newValue));
				else if (fieldType == long.class)
					updates.add(Updates.ofLong(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.LONG), (long) newValue));
				else if (fieldType == float.class)
					updates.add(Updates.ofFloat(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.FLOAT), (float) newValue));
				else if (fieldType == double.class)
					updates.add(Updates.ofDouble(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.DOUBLE), (double) newValue));
				else if (fieldType == BigInteger.class) {
					if (newValue == null)
						updates.add(Updates.toNull(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BIG_INTEGER), true));
					else
						updates.add(Updates.ofBigInteger(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.BIG_INTEGER), (BigInteger) newValue));
				}
				else if (fieldType == String.class) {
					if (newValue == null)
						updates.add(Updates.toNull(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.STRING), true));
					else
						updates.add(Updates.ofString(storageReference, FieldSignatures.of(fieldDefiningClass, fieldName, StorageTypes.STRING), (String) newValue));
				}
				else if (classLoader.isLazilyLoaded(fieldType))
					addUpdateFor(fieldDefiningClass, fieldName, fieldType.getName(), newValue);
				else
					// for example arrays: they should have been forbidden when verifying the installed jars
					throw new StoreException("Unexpected type " + fieldType.getName() + " for a field of a storage object: " + fieldDefiningClass.getName() + '.' + fieldName);
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