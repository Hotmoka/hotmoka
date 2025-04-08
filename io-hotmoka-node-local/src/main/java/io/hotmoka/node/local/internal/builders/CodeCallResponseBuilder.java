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

import java.lang.reflect.Executable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.api.SerializationException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.UnknownTypeException;
import io.hotmoka.node.api.requests.CodeExecutionTransactionRequest;
import io.hotmoka.node.api.responses.CodeExecutionTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.AbstractNonInitialResponseBuilder;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.whitelisting.Dummy;
import io.hotmoka.whitelisting.api.WhiteListingClassLoader;
import io.takamaka.code.constants.Constants;

/**
 * The creator of a response for a non-initial transaction that executes a method or constructor of Takamaka code.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class CodeCallResponseBuilder<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse>
		extends AbstractNonInitialResponseBuilder<Request, Response> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	protected CodeCallResponseBuilder(TransactionReference reference, Request request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	protected abstract class ResponseCreator extends AbstractNonInitialResponseBuilder<Request, Response>.ResponseCreator {

		/**
		 * The events accumulated during the transaction.
		 */
		private final List<Object> events = new ArrayList<>();

		/**
		 * The deserialized actual arguments of the method or constructor.
		 */
		private Object[] deserializedActuals;

		protected ResponseCreator() throws TransactionRejectedException, StoreException {}

		@Override
		protected void checkConsistency() throws TransactionRejectedException, StoreException {
			super.checkConsistency();

			// calls to @View methods are allowed to receive non-exported values
			if (transactionIsSigned()) 
				argumentsAreExported();
		}

		/**
		 * Yields the actual arguments of the call.
		 * 
		 * @return the actual arguments
		 */
		protected final Object[] getDeserializedActuals() {
			return deserializedActuals;
		}

		/**
		 * Yields the actual arguments of the call, followed by the implicit actuals that are passed
		 * to {@code io.takamaka.code.lang.FromContract} methods or constructors. They are the caller of
		 * the method or constructor and {@code null} for the dummy argument.
		 * 
		 * @return the actual arguments, followed by the implicit actuals
		 */
		protected final Object[] getDeserializedActualsForFromContract() {
			int al = getDeserializedActuals().length;
			var result = new Object[al + 2];
			System.arraycopy(getDeserializedActuals(), 0, result, 0, al);
			result[al] = getDeserializedCaller();
			result[al + 1] = null; // Dummy is not used
		
			return result;
		}

		/**
		 * Deserialize the actual arguments of the call.
		 */
		protected final void deserializeActuals() throws HotmokaException, StoreException {
			var actuals = request.actuals().toArray(StorageValue[]::new);
			deserializedActuals = new Object[actuals.length];
			int pos = 0;
			for (StorageValue actual: actuals)
				deserializedActuals[pos++] = deserializer.deserialize(actual);
		}

		/**
		 * Enforces that the given transaction reference points to an exported object in store.
		 * 
		 * @param reference the transaction reference
		 * @throws TransactionRejectedException of the type of the object in store is not exported
		 * @throws StoreException if the class tag of {@code reference} cannot be found in the Takamaka program
		 */
		protected final void enforceExported(StorageReference reference) throws TransactionRejectedException, StoreException {
			try {
				var clazz = environment.getClassTag(reference).getClazz();
		
				try {
					if (!classLoader.isExported(clazz.getName()))
						throw new TransactionRejectedException("Class " + clazz + " of the parameter " + reference + " is not exported: add @Exported to " + clazz, consensus);
				}
				catch (ClassNotFoundException e) {
					throw new TransactionRejectedException("Class " + clazz + " of the parameter " + reference + " cannot be resolved", consensus);
				}
			}
			catch (UnknownReferenceException e) {
				throw new TransactionRejectedException("Object " + reference + " cannot be found in store", consensus);
			}
		}

		@Override
		public final void event(Object event) {
			events.add(Objects.requireNonNull(event));
		}

		/**
		 * Determines if the given method or constructor is annotated with an annotation with the given name.
		 * 
		 * @param executable the method or constructor
		 * @param annotationName the name of the annotation
		 * @return true if and only if that condition holds
		 */
		protected static boolean hasAnnotation(Executable executable, String annotationName) {
			return Stream.of(executable.getAnnotations())
				.anyMatch(annotation -> annotation.annotationType().getName().equals(annotationName));
		}

		/**
		 * Yields a description of the program point where the given exception should be reported
		 * to the user. It scans its stack trace, backwards, looking for a stack element that
		 * was loaded by a classloader of Takamaka. If it does not find it, yields {@code null}.
		 * The idea is that users should not get any program point related to the Java library,
		 * but only program points inside the code that they wrote.
		 * 
		 * @param throwable the exception
		 * @return the program point, if available. Otherwise {@code null}
		 */
		protected final String where(Throwable throwable) {
			StackTraceElement[] stackTrace = throwable.getStackTrace();
			if (stackTrace != null)
				for (StackTraceElement cursor: stackTrace) {
					int line = cursor.getLineNumber();
					String className = cursor.getClassName();
					// we avoid messages in synthetic code or code in the Takamaka library
					if (line >= 0 && !className.startsWith(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME))
						try {
							Class<?> clazz = classLoader.loadClass(className);
							if (clazz.getClassLoader() instanceof WhiteListingClassLoader)
								return cursor.getFileName() + ":" + line;
						}
						catch (ClassNotFoundException e) {}
				}
		
			return null;
		}

		/**
		 * Yields the classes of the formal arguments of the method or constructor.
		 * 
		 * @return the array of classes, in the same order as the formals
		 * @throws ClassNotFoundException if some class cannot be found
		 */
		protected final Class<?>[] formalsAsClass() throws ClassNotFoundException {
			var formals = request.getStaticTarget().getFormals().toArray(StorageType[]::new);
			var classes  = new Class<?>[formals.length];
			int pos = 0;
			for (var formal: formals)
				classes[pos++] = classLoader.loadClass(formal);
		
			return classes;
		}

		/**
		 * Yields the classes of the formal arguments of the method or constructor.
		 * 
		 * @return the array of classes, in the same order as the formals
		 * @throws ClassNotFoundException if some class cannot be found
		 */
		protected final Class<?>[] formalsAsClass2() throws UnknownTypeException { // TODO: rename at then end
			var formals = request.getStaticTarget().getFormals().toArray(StorageType[]::new);
			var classes  = new Class<?>[formals.length];
			int pos = 0;
			for (var formal: formals) {
				try {
					classes[pos++] = classLoader.loadClass(formal);
				}
				catch (ClassNotFoundException e) {
					throw new UnknownTypeException(formal);
				}
			}
		
			return classes;
		}

		/**
		 * Yields the classes of the formal arguments of the method or constructor, assuming that it is
		 * an {@link io.takamaka.code.lang.FromContract}. These are instrumented with the addition of a
		 * trailing contract formal argument (the caller) and of a dummy type.
		 * 
		 * @return the array of classes, in the same order as the formals
		 * @throws ClassNotFoundException if some class cannot be found
		 */
		protected final Class<?>[] formalsAsClassForFromContract() throws ClassNotFoundException {
			var formals = request.getStaticTarget().getFormals().toArray(StorageType[]::new);
			var classes  = new Class<?>[formals.length + 2];
			int pos = 0;
			for (var formal: formals)
				classes[pos++] = classLoader.loadClass(formal);

			classes[pos++] = classLoader.getContract();
			classes[pos] = Dummy.class;
		
			return classes;
		}

		/**
		 * Yields the classes of the formal arguments of the method or constructor, assuming that it is
		 * an {@link io.takamaka.code.lang.FromContract}. These are instrumented with the addition of a
		 * trailing contract formal argument (the caller) and of a dummy type.
		 * 
		 * @return the array of classes, in the same order as the formals
		 * @throws UnknownTypeException if some class cannot be found
		 */
		protected final Class<?>[] formalsAsClassForFromContract2() throws UnknownTypeException { // TODO: rename at the end
			var formals = request.getStaticTarget().getFormals().toArray(StorageType[]::new);
			var classes  = new Class<?>[formals.length + 2];
			int pos = 0;
			for (var formal: formals) {
				try {
					classes[pos++] = classLoader.loadClass(formal);
				}
				catch (ClassNotFoundException e) {
					throw new UnknownTypeException(formal);
				}
			}

			classes[pos++] = classLoader.getContract();
			classes[pos] = Dummy.class;
		
			return classes;
		}

		/**
		 * Yields the serialization of the given RAM object, that is, yields its
		 * representation in the store.
		 * 
		 * @param object the object to serialize. This must be a storage object, a Java wrapper
		 *               object for numerical types, an enumeration
		 *               or a special Java object that is allowed
		 *               in store, such as a {@link java.lang.String} or {@link java.math.BigInteger}
		 * @return the serialization of {@code object}, if any
		 */
		protected final StorageValue serialize(Object object) throws SerializationException, StoreException {
			if (isStorage(object))
				return classLoader.getStorageReferenceOf(object, StoreException::new);
			else if (object instanceof BigInteger bi)
				return StorageValues.bigIntegerOf(bi);
			else if (object instanceof Boolean b)
				return StorageValues.booleanOf(b);
			else if (object instanceof Byte b)
				return StorageValues.byteOf(b);
			else if (object instanceof Character c)
				return StorageValues.charOf(c);
			else if (object instanceof Double d)
				return StorageValues.doubleOf(d);
			else if (object instanceof Float f)
				return StorageValues.floatOf(f);
			else if (object instanceof Integer i)
				return StorageValues.intOf(i);
			else if (object instanceof Long l)
				return StorageValues.longOf(l);
			else if (object instanceof Short s)
				return StorageValues.shortOf(s);
			else if (object instanceof String s)
				return StorageValues.stringOf(s);
			else if (object == null)
				return StorageValues.NULL;
			else
				throw new SerializationException("An object of class " + object.getClass().getName()
					+ " cannot be serialized into a storage value since it does not implement " + Constants.STORAGE_NAME);
		}

		private boolean isStorage(Object object) {
			return object != null && classLoader.getStorage().isAssignableFrom(object.getClass());
		}

		/**
		 * Checks that all the arguments and the receiver passed to the method or constructor have exported type.
		 * 
		 * @throws TransactionRejectedException if that condition does not hold
		 * @throws StoreException if the store is misbehaving
		 */
		private void argumentsAreExported() throws TransactionRejectedException, StoreException {
			for (var actual: request.actuals().toArray(StorageValue[]::new))
				if (actual instanceof StorageReference sr)
					enforceExported(sr);
		}

		/**
		 * Determines if the given annotation is checked and the given method or constructor
		 * is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
		 *
		 * @param cause the exception
		 * @param executable the method or constructor whose execution has thrown the exception
		 * @return true if and only if that condition holds
		 */
		protected final boolean isCheckedForThrowsExceptions(Throwable cause, Executable executable) {
			return isChecked(cause) && hasAnnotation(executable, Constants.THROWS_EXCEPTIONS_NAME);
		}

		/**
		 * Determines if the given throwable is a checked exception.
		 * 
		 * @param t the throwable
		 * @return true if and only if {@code t} is a checked exception
		 */
		private boolean isChecked(Throwable t) {
			return !(t instanceof RuntimeException || t instanceof Error);
		}

		@Override
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			super.scanPotentiallyAffectedObjects(consumer);

			Class<?> storage = classLoader.getStorage();
			for (Object actual: getDeserializedActuals())
				if (actual != null && storage.isAssignableFrom(actual.getClass()))
					consumer.accept(actual);
		
			// events are accessible from outside, hence they count as side-effects
			events.forEach(consumer);
		}

		/**
		 * Collects all updates that can be seen from the context of the caller of the transaction,
		 * including the returned value of a method or the created object of a constructor.
		 * 
		 * @param result the returned value or created object
		 * @return the updates, sorted
		 */
		protected final Stream<Update> updates(Object result) throws IllegalAssignmentToFieldInStorageException, StoreException {
			List<Object> potentiallyAffectedObjects = new ArrayList<>();

			Class<?> storage = classLoader.getStorage();
			if (result != null && storage.isAssignableFrom(result.getClass()))
				potentiallyAffectedObjects.add(result);

			scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
			return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects);
		}

		/**
		 * Yields the storage references of the events generated so far.
		 * 
		 * @return the storage references
		 * @throws StoreException 
		 */
		protected final Stream<StorageReference> storageReferencesOfEvents() throws StoreException {
			var result = new ArrayList<StorageReference>();
			for (var event: events)
				result.add(classLoader.getStorageReferenceOf(event, StoreException::new));

			return result.stream();
		}
	}
}