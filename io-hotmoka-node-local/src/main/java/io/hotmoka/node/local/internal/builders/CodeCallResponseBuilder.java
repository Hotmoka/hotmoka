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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.functions.ConsumerWithExceptions2;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.CodeExecutionTransactionRequest;
import io.hotmoka.node.api.responses.CodeExecutionTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.AbstractNonInitialResponseBuilder;
import io.hotmoka.node.local.DeserializationException;
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

		protected ResponseCreator() throws TransactionRejectedException, StoreException {}

		@Override
		protected void checkConsistency() throws TransactionRejectedException {
			super.checkConsistency();

			try {
				// calls to @View methods are allowed to receive non-exported values
				if (transactionIsSigned()) 
					argumentsAreExported();
			}
			catch (StoreException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Yields the actual arguments of the call.
		 * 
		 * @return the actual arguments
		 */
		protected abstract Stream<Object> getDeserializedActuals();

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
					// we avoid messages in synthetic code or code in the Takamaka library
					if (line >= 0 && !cursor.getClassName().startsWith(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME))
						try {
							Class<?> clazz = classLoader.loadClass(cursor.getClassName());
							if (clazz.getClassLoader() instanceof WhiteListingClassLoader)
								return cursor.getFileName() + ":" + line;
						}
						catch (Exception e) {}
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
			List<Class<?>> classes = new ArrayList<>();
			for (StorageType type: request.getStaticTarget().getFormals().collect(Collectors.toList()))
				classes.add(classLoader.loadClass(type));
		
			return classes.toArray(Class<?>[]::new);
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
			List<Class<?>> classes = new ArrayList<>();
			for (StorageType type: request.getStaticTarget().getFormals().collect(Collectors.toList()))
				classes.add(classLoader.loadClass(type));
		
			classes.add(classLoader.getContract());
			classes.add(Dummy.class);
		
			return classes.toArray(Class<?>[]::new);
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
		 * @throws IllegalArgumentException if the type of {@code object} is not allowed in store
		 */
		protected final StorageValue serialize(Object object) throws IllegalArgumentException {
			if (isStorage(object))
				return classLoader.getStorageReferenceOf(object);
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
				throw new IllegalArgumentException("An object of class " + object.getClass().getName()
					+ " cannot be kept in store since it does not implement " + Constants.STORAGE_NAME);
		}

		private boolean isStorage(Object object) {
			return object != null && classLoader.getStorage().isAssignableFrom(object.getClass());
		}

		/**
		 * Checks that all the arguments and the receiver passed to the method or constructor have exported type.
		 * 
		 * @throws TransactionRejectedException if that condition does not hold
		 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
		 * @throws NodeException 
		 * @throws UnknownReferenceException 
		 * @throws NoSuchElementException 
		 */
		private void argumentsAreExported() throws TransactionRejectedException, StoreException {
			ConsumerWithExceptions2<StorageReference, TransactionRejectedException, StoreException> enforceExported = this::enforceExported;
			CheckRunnable.check(TransactionRejectedException.class, StoreException.class, () -> request.actuals()
				.filter(actual -> actual instanceof StorageReference)
				.map(actual -> (StorageReference) actual)
				.forEachOrdered(UncheckConsumer.uncheck(TransactionRejectedException.class, StoreException.class, enforceExported)));
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

		/**
		 * Scans the objects reachable from the context of the caller of the transaction
		 * that might have been affected during the execution of the transaction
		 * and consumes each of them. Such objects do not include the returned value of
		 * a method or the object created by a constructor, if any.
		 * 
		 * @param consumer the consumer
		 */
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			consumer.accept(getDeserializedCaller());
			getDeserializedValidators().ifPresent(consumer);

			Class<?> storage = classLoader.getStorage();
			getDeserializedActuals()
				.filter(actual -> actual != null && storage.isAssignableFrom(actual.getClass()))
				.forEach(consumer);
		
			// events are accessible from outside, hence they count as side-effects
			events.forEach(consumer);
		}

		/**
		 * Collects all updates that can be seen from the context of the caller of the method or constructor.
		 * 
		 * @return the updates, sorted
		 * @throws DeserializationException 
		 */
		protected final Stream<Update> updates() throws UpdatesExtractionException, StoreException {
			List<Object> potentiallyAffectedObjects = new ArrayList<>();
			scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
			return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream());
		}

		/**
		 * Collects all updates that can be seen from the context of the caller of the transaction,
		 * including the returned value of a method or the created object of a constructor.
		 * 
		 * @param result the returned value or created object
		 * @return the updates, sorted
		 * @throws DeserializationException 
		 */
		protected final Stream<Update> updates(Object result) throws UpdatesExtractionException, StoreException {
			List<Object> potentiallyAffectedObjects = new ArrayList<>();

			Class<?> storage = classLoader.getStorage();
			if (result != null && storage.isAssignableFrom(result.getClass()))
				potentiallyAffectedObjects.add(result);

			scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
			return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream());
		}

		/**
		 * Yields the storage references of the events generated so far.
		 * 
		 * @return the storage references
		 */
		protected final Stream<StorageReference> storageReferencesOfEvents() {
			return events.stream().map(classLoader::getStorageReferenceOf);
		}
	}
}