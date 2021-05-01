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

package io.hotmoka.local.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.local.NonInitialResponseBuilder;
import io.hotmoka.local.internal.NodeInternal;
import io.hotmoka.local.internal.Serializer;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.hotmoka.verification.Dummy;
import io.hotmoka.whitelisting.ResolvingClassLoader;
import io.hotmoka.whitelisting.WhiteListingPredicate;
import io.hotmoka.whitelisting.WhiteListingProofObligation;

/**
 * The creator of a response for a non-initial transaction that executes a method or constructor of Takamaka code.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class CodeCallResponseBuilder<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends NonInitialResponseBuilder<Request, Response> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected CodeCallResponseBuilder(TransactionReference reference, Request request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);

		try {
			// calls to @View methods are allowed to receive non-exported values
			if (transactionIsSigned()) 
				argumentsAreExported();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	/**
	 * Checks that all the arguments and the receiver passed to the method or constructor have exported type.
	 * 
	 * @throws TransactionRejectedException if that condition does not hold
	 */
	private void argumentsAreExported() throws TransactionRejectedException {
		List<StorageReference> args = request.actuals()
			.filter(actual -> actual instanceof StorageReference)
			.map(actual -> (StorageReference) actual)
			.collect(Collectors.toList());

		for (StorageReference arg: args)
			enforceExported(arg);
	}

	/**
	 * Enforces that the given transaction reference points to an exported object in store.
	 * 
	 * @param reference the transaction reference
	 * @throws TransactionRejectedException of the type of the object in store is not exported
	 */
	protected final void enforceExported(StorageReference reference) throws TransactionRejectedException {
		ClassTag classTag = node.getClassTag(reference);
		if (!classLoader.isExported(classTag.clazz.name))
			throw new TransactionRejectedException("cannot pass as argument a value of the non-exported type " + classTag.clazz);
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
						if (clazz.getClassLoader() instanceof ResolvingClassLoader)
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
		for (StorageType type: request.getStaticTarget().formals().collect(Collectors.toList()))
			classes.add(storageTypeToClass.toClass(type));
	
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
		for (StorageType type: request.getStaticTarget().formals().collect(Collectors.toList()))
			classes.add(storageTypeToClass.toClass(type));
	
		classes.add(classLoader.getContract());
		classes.add(Dummy.class);
	
		return classes.toArray(Class<?>[]::new);
	}

	protected abstract class ResponseCreator extends NonInitialResponseBuilder<Request, Response>.ResponseCreator {

		/**
		 * The object that serializes RAM values into storage objects.
		 */
		protected final Serializer serializer;

		/**
		 * The events accumulated during the transaction.
		 */
		private final List<Object> events = new ArrayList<>();

		protected ResponseCreator() throws TransactionRejectedException {
			try {
				this.serializer = new Serializer(CodeCallResponseBuilder.this);
			}
			catch (Throwable t) {
				throw new TransactionRejectedException(t);
			}
		}

		/**
		 * Yields the actual arguments of the call.
		 * 
		 * @return the actual arguments
		 */
		protected abstract Stream<Object> getDeserializedActuals();

		@Override
		public final void event(Object event) {
			if (event == null)
				throw new NullPointerException("an event cannot be null");

			events.add(event);
		}

		/**
		 * Checks run-time proof obligations for the given value. This is used to verify
		 * that some white-listing annotations hold when a value is passed to a white-listed
		 * method that requires some conditions on the passed value.
		 * 
		 * @param methodName the name of the method, into which the value is passed
		 * @param value the value for which the obligation must be proved
		 * @param annotations the annotations that specify what to check on the value
		 * @throws NonWhiteListedCallException if some annotation is not satisfied by the {@code value}
		 */
		protected final void checkWhiteListingProofObligations(String methodName, Object value, Annotation[] annotations) {
			Stream.of(annotations)
				.map(Annotation::annotationType)
				.map(annotationType -> annotationType.getAnnotation(WhiteListingProofObligation.class))
				.filter(Objects::nonNull)
				.map(WhiteListingProofObligation::check)
				.map(this::createWhiteListingPredicateFrom)
				.filter(predicate -> !predicate.test(value, classLoader.getWhiteListingWizard()))
				.map(predicate -> predicate.messageIfFailed(methodName))
				.map(NonWhiteListedCallException::new)
				.findFirst()
				.ifPresent(exception -> { throw exception; });
		}

		/**
		 * Yields an instance of the given white-listing predicate.
		 * 
		 * @param clazz the class of the predicate
		 * @return an instance of that class
		 */
		private WhiteListingPredicate createWhiteListingPredicateFrom(Class<? extends WhiteListingPredicate> clazz) {
			try {
				return clazz.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * Determines if the given annotation is checked and the given method or constructor
		 * is assnotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
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
		 */
		protected final Stream<Update> updates() {
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
		 */
		protected final Stream<Update> updates(Object result) {
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