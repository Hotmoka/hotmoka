package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.NonWhiteListedCallException;
import io.takamaka.code.engine.runtime.Runtime;
import io.takamaka.code.verification.Dummy;
import io.takamaka.code.whitelisting.WhiteListingProofObligation;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class CodeCallTransactionRun<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends NonInitialTransactionRun<Request, Response> {

	protected CodeCallTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);
	}

	protected final void checkWhiteListingProofObligations(String methodName, Object value, Annotation[] annotations) {
		Stream.of(annotations)
		.map(Annotation::annotationType)
		.map(CodeCallTransactionRun::getWhiteListingCheckFor)
		.filter(Optional::isPresent)
		.map(Optional::get)
		.forEachOrdered(checkMethod -> {
			try {
				// white-listing check methods are static
				checkMethod.invoke(null, value, methodName);
			}
			catch (InvocationTargetException e) {
				throw (NonWhiteListedCallException) e.getCause();
			}
			catch (IllegalAccessException | IllegalArgumentException e) {
				throw new IllegalStateException("could not check white-listing proof-obligations for " + methodName, e);
			}
		});
	}

	private static Optional<Method> getWhiteListingCheckFor(Class<? extends Annotation> annotationType) {
		if (annotationType.isAnnotationPresent(WhiteListingProofObligation.class)) {
			String checkName = lowerInitial(annotationType.getSimpleName());
			Optional<Method> checkMethod = Stream.of(Runtime.class.getDeclaredMethods())
				.filter(method -> method.getName().equals(checkName)).findFirst();

			if (!checkMethod.isPresent())
				throw new IllegalStateException("unexpected white-list annotation " + annotationType.getSimpleName());

			return checkMethod;
		}

		return Optional.empty();
	}

	private static String lowerInitial(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	/**
	 * Checks if the given object is a red/green externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalArgumentException if the object is not a red/green externally owned account
	 */
	protected final void checkIsRedGreenExternallyOwned(Object object) {
		if (!getClassLoader().getRedGreenExternallyOwnedAccount().isAssignableFrom(object.getClass()))
			throw new IllegalArgumentException("only a red/green externally owned contract can start a transaction for a @RedPayable method or constructor");
	}

	/**
	 * Yields the classes of the formal arguments of the method or constructor.
	 * 
	 * @return the array of classes, in the same order as the formals
	 * @throws ClassNotFoundException if some class cannot be found
	 */
	protected final Class<?>[] formalsAsClass() throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: getMethodOrConstructor().formals().collect(Collectors.toList()))
			classes.add(storageTypeToClass.toClass(type));

		return classes.toArray(new Class<?>[classes.size()]);
	}

	/**
	 * Yields the classes of the formal arguments of the method or constructor, assuming that it is
	 * and {@link io.takamaka.code.lang.Entry}. Entries are instrumented with the addition of
	 * trailing contract formal (the caller) and of a dummy type.
	 * 
	 * @return the array of classes, in the same order as the formals
	 * @throws ClassNotFoundException if some class cannot be found
	 */
	protected final Class<?>[] formalsAsClassForEntry() throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: getMethodOrConstructor().formals().collect(Collectors.toList()))
			classes.add(storageTypeToClass.toClass(type));

		classes.add(getClassLoader().getContract());
		classes.add(Dummy.class);

		return classes.toArray(new Class<?>[classes.size()]);
	}

	/**
	 * Collects all updates that can be seen from environment of who calls the transaction.
	 * 
	 * @return the updates, sorted
	 */
	protected final Stream<Update> updates() {
		List<Object> potentiallyAffectedObjects = new ArrayList<>();
		scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
		return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream()).collect(Collectors.toCollection(TreeSet::new)).stream();
	}

	/**
	 * Collects all updates that can be seen from environment of who calls the transaction,
	 * including the returned value of a method or created object of a constructor.
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
	 * Yields the same exception, if it is checked and the executable is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
	 * Otherwise, yields its cause.
	 * 
	 * @param e the exception
	 * @param executable the method or constructor whose execution has thrown the exception
	 * @return the same exception, or its cause
	 */
	protected final static Throwable unwrapInvocationTargetException(InvocationTargetException e, Executable executable) {
		return isChecked(e.getCause()) && hasAnnotation(executable, Constants.THROWS_EXCEPTIONS_NAME) ? e : e.getCause();
	}

	/**
	 * Yields the same exception, if it is checked and the executable is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
	 * Otherwise, yields its cause.
	 * 
	 * @param e the exception
	 * @param executable the method or constructor whose execution has thrown the exception
	 * @return the same exception, or its cause
	 */
	protected final static boolean isCheckedForThrowsExceptions(InvocationTargetException e, Executable executable) {
		return isChecked(e.getCause()) && hasAnnotation(executable, Constants.THROWS_EXCEPTIONS_NAME);
	}

	private static boolean isChecked(Throwable t) {
		return !(t instanceof RuntimeException || t instanceof Error);
	}

	protected final static boolean hasAnnotation(Executable executable, String annotationName) {
		return Stream.of(executable.getAnnotations())
			.anyMatch(annotation -> annotation.annotationType().getName().equals(annotationName));
	}

	/**
	 * Yields the storage references of the events generated so far.
	 * 
	 * @return the storage references
	 */
	protected final Stream<StorageReference> storageReferencesOfEvents() {
		return events().map(getClassLoader()::getStorageReferenceOf);
	}

	/**
	 * Scans the objects of the caller that might have been affected during the execution of the
	 * transaction, and consumes each of them. Such objects do not include the returned value of
	 * a method or created object of a constructor.
	 * 
	 * @param add the consumer
	 */
	protected void scanPotentiallyAffectedObjects(Consumer<Object> add) {
		add.accept(getDeserializedCaller());
	
		Class<?> storage = getClassLoader().getStorage();
		getDeserializedActuals()
			.filter(actual -> actual != null && storage.isAssignableFrom(actual.getClass()))
			.forEach(add);
	
		// events are accessible from outside, hence they count as side-effects
		events().forEach(add);
	}

	/**
	 * Yields the method or constructor that is being called.
	 * 
	 * @return the method or constructor that is being called
	 */
	protected abstract CodeSignature getMethodOrConstructor();

	/**
	 * Yields the caller of this transaction.
	 * 
	 * @return the caller
	 */
	protected abstract Object getDeserializedCaller();

	/**
	 * Yields the actual arguments of the call.
	 * 
	 * @return the actual arguments
	 */
	protected abstract Stream<Object> getDeserializedActuals();
}