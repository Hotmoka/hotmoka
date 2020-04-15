package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.verification.Dummy;
import io.takamaka.code.whitelisting.ResolvingClassLoader;
import io.takamaka.code.whitelisting.WhiteListingPredicate;
import io.takamaka.code.whitelisting.WhiteListingProofObligation;

/**
 * The creator of a response for a non-initial transaction that executes a method or constructor of Takamaka code.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class CodeCallResponseBuilder<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends NonInitialResponseBuilder<Request, Response> {

	/**
	 * The class loader used for the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The deserialized caller.
	 */
	protected Object deserializedCaller;

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected CodeCallResponseBuilder(Request request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			this.classLoader = node.getCachedClassLoader(request.classpath);
			chargeGasForClassLoader();
			this.deserializedCaller = deserializer.deserialize(request.caller);
			callerMustBeExternallyOwnedAccount();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorageOfRequest();
			remainingGasMustBeEnoughForStoringFailedResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	protected final Object getDeserializedCaller() {
		return deserializedCaller;
	}

	/**
	 * Checks that the caller of the transaction is a red/green externally owned account or subclass.
	 * 
	 * @throws IllegalArgumentException if the object is not a red/green externally owned account
	 */
	protected final void callerMustBeRedGreenExternallyOwnedAccount() {
		if (!getClassLoader().getRedGreenExternallyOwnedAccount().isAssignableFrom(getDeserializedCaller().getClass()))
			throw new IllegalArgumentException("only a red/green externally owned contract can start a transaction for a @RedPayable method or constructor");
	}

	/**
	 * Determines if the given method or constructor is annotated with an annotation with the given name.
	 * 
	 * @param executable the method or constructor
	 * @param annotationName the name of the annotation
	 * @return true if and only if that condition holds
	 */
	protected final static boolean hasAnnotation(Executable executable, String annotationName) {
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
				if (line >= 0 && !cursor.getClassName().startsWith(Constants.TAKAMAKA_LANG_PACKAGE))
					try {
						Class<?> clazz = getClassLoader().loadClass(cursor.getClassName());
						if (clazz.getClassLoader() instanceof ResolvingClassLoader)
							return cursor.getFileName() + ":" + line;
					}
					catch (Exception e) {}
			}

		return null;
	}

	protected abstract class ResponseCreator {
		protected ResponseCreator() {
		}

		/**
		 * Yields the actual arguments of the call.
		 * 
		 * @return the actual arguments
		 */
		protected abstract Stream<Object> getDeserializedActuals();

		/**
		 * Checks that the number of formal and actual arguments is the same.
		 * 
		 * @throws IllegalArgumentException if the numbers do not match
		 */
		protected final void formalsAndActualsMustMatch() {
			if (request.getStaticTarget().formals().count() != request.actuals().count())
				throw new IllegalArgumentException("argument count mismatch between formals and actuals");
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

			return classes.toArray(new Class<?>[classes.size()]);
		}

		/**
		 * Yields the classes of the formal arguments of the method or constructor, assuming that it is
		 * an {@link io.takamaka.code.lang.Entry}. Entries are instrumented with the addition of a
		 * trailing contract formal argument (the caller) and of a dummy type.
		 * 
		 * @return the array of classes, in the same order as the formals
		 * @throws ClassNotFoundException if some class cannot be found
		 */
		protected final Class<?>[] formalsAsClassForEntry() throws ClassNotFoundException {
			List<Class<?>> classes = new ArrayList<>();
			for (StorageType type: request.getStaticTarget().formals().collect(Collectors.toList()))
				classes.add(storageTypeToClass.toClass(type));

			classes.add(getClassLoader().getContract());
			classes.add(Dummy.class);

			return classes.toArray(new Class<?>[classes.size()]);
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
				.filter(predicate -> !predicate.test(value, getClassLoader().getWhiteListingWizard()))
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
		 * Yields the same exception, if it is checked and the executable is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
		 * Otherwise, yields its cause.
		 * 
		 * @param e the exception
		 * @param executable the method or constructor whose execution has thrown the exception
		 * @return the same exception, or its cause
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
		 * Scans the objects of the caller that might have been affected during the execution of the
		 * transaction, and consumes each of them. Such objects do not include the returned value of
		 * a method or the object created by a constructor.
		 * 
		 * @param consumer the consumer
		 */
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			consumer.accept(getDeserializedCaller());
		
			Class<?> storage = getClassLoader().getStorage();
			getDeserializedActuals()
				.filter(actual -> actual != null && storage.isAssignableFrom(actual.getClass()))
				.forEach(consumer);
		
			// events are accessible from outside, hence they count as side-effects
			events().forEach(consumer);
		}

		/**
		 * Collects all updates that can be seen from the context of the caller of the method or constructor.
		 * 
		 * @return the updates, sorted
		 */
		protected final Stream<Update> updates() {
			List<Object> potentiallyAffectedObjects = new ArrayList<>();
			scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
			return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream()).collect(Collectors.toCollection(TreeSet::new)).stream();
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

			Class<?> storage = getClassLoader().getStorage();
			if (result != null && storage.isAssignableFrom(result.getClass()))
				potentiallyAffectedObjects.add(result);

			scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
			return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream()).collect(Collectors.toCollection(TreeSet::new)).stream();
		}

		/**
		 * Yields the storage references of the events generated so far.
		 * 
		 * @return the storage references
		 */
		protected final Stream<StorageReference> storageReferencesOfEvents() {
			return events().map(getClassLoader()::getStorageReferenceOf);
		}
	}
}