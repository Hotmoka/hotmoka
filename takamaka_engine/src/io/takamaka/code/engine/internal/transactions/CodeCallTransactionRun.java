package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.nodes.Node;
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

	public UpdateOfBalance balanceUpdateInCaseOfFailure;

	/**
	 * The deserialized caller.
	 */
	public Object deserializedCaller;

	/**
	 * The deserialized actual arguments of the call.
	 */
	public Object[] deserializedActuals;

	/**
	 * The resulting value for methods or the created object for constructors.
	 * This is {@code null} if the execution completed with an exception or
	 * if the method actually returned {@code null}.
	 */
	public Object result;

	/**
	 * The exception resulting from the execution of the method or constructor, if any.
	 * This is {@code null} if the execution completed without exception.
	 */
	public Throwable exception;

	private SortedSet<Update> updates;

	protected CodeCallTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);
	}

	/**
	 * Checks that the method or constructor called by this transaction, that is,
	 * is must be white-listed and its white-listing proof-obligations must hold.
	 * 
	 * @param executable the method or constructor
	 * @param actuals the actual arguments passed to {@code executable}, including the
	 *                receiver for instance methods
	 * @throws ClassNotFoundException if some class could not be found during the check
	 */
	protected final void ensureWhiteListingOf(Executable executable, Object[] actuals) throws ClassNotFoundException {
		Optional<? extends Executable> model;
		CodeSignature methodOrConstructor = getMethodOrConstructor();
		if (executable instanceof Constructor<?>) {
			model = classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed constructor of "
					+ methodOrConstructor.definingClass.name);
		}
		else {
			model = classLoader.getWhiteListingWizard().whiteListingModelOf((Method) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed method "
					+ methodOrConstructor.definingClass.name + "." + ((MethodSignature) methodOrConstructor).methodName);
		}

		if (this instanceof InstanceMethodCallTransactionRun && !Modifier.isStatic(executable.getModifiers()))
			checkWhiteListingProofObligations(model.get().getName(), ((InstanceMethodCallTransactionRun) this).deserializedReceiver, model.get().getAnnotations());

		Annotation[][] anns = model.get().getParameterAnnotations();
		for (int pos = 0; pos < anns.length; pos++)
			checkWhiteListingProofObligations(model.get().getName(), actuals[pos], anns[pos]);
	}

	private void checkWhiteListingProofObligations(String methodName, Object value, Annotation[] annotations) {
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

		classes.add(classLoader.getContract());
		classes.add(Dummy.class);

		return classes.toArray(new Class<?>[classes.size()]);
	}

	/**
	 * Adds to the actual parameters the implicit actuals that are passed
	 * to {@link io.takamaka.code.lang.Entry} methods or constructors. They are the caller of
	 * the entry and {@code null} for the dummy argument.
	 * 
	 * @return the resulting actual parameters
	 */
	protected final Object[] addExtraActualsForEntry() {
		int al = deserializedActuals.length;
		Object[] result = new Object[al + 2];
		System.arraycopy(deserializedActuals, 0, result, 0, al);
		result[al] = deserializedCaller;
		result[al + 1] = null; // Dummy is not used

		return result;
	}

	/**
	 * Collects all updates reachable from the actuals or from the caller, receiver or result of a method call.
	 * 
	 * @return the updates, sorted
	 */
	protected final Stream<Update> updates() {
		if (updates != null)
			return updates.stream();

		List<Object> potentiallyAffectedObjects = new ArrayList<>();
		if (deserializedCaller != null)
			potentiallyAffectedObjects.add(deserializedCaller);
		if (this instanceof InstanceMethodCallTransactionRun)
			potentiallyAffectedObjects.add(((InstanceMethodCallTransactionRun) this).deserializedReceiver);
		Class<?> storage = classLoader.getStorage();
		if (result != null && storage.isAssignableFrom(result.getClass()))
			potentiallyAffectedObjects.add(result);

		for (Object actual: deserializedActuals)
			if (actual != null && storage.isAssignableFrom(actual.getClass()))
				potentiallyAffectedObjects.add(actual);

		// events are accessible from outside, hence they count as side-effects
		eventObjects().forEach(potentiallyAffectedObjects::add);

		return (updates = updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream()).collect(Collectors.toCollection(TreeSet::new))).stream();
	}

	/**
	 * Yields the same exception, if it is checked and the executable is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
	 * Otherwise, yields its cause.
	 * 
	 * @param e the exception
	 * @param executable the method or constructor whose execution has thrown the exception
	 * @return the same exception, or its cause
	 */
	protected final static Throwable unwrapInvocationException(InvocationTargetException e, Executable executable) {
		return isChecked(e.getCause()) && hasAnnotation(executable, ClassType.THROWS_EXCEPTIONS.name) ? e : e.getCause();
	}

	private static boolean isChecked(Throwable t) {
		return !(t instanceof RuntimeException || t instanceof Error);
	}

	protected final static boolean hasAnnotation(Executable executable, String annotationName) {
		return Stream.of(executable.getAnnotations())
			.anyMatch(annotation -> annotation.annotationType().getName().equals(annotationName));
	}

	/**
	 * Yields the method or constructor that is being called.
	 * 
	 * @return the method or constructor that is being called
	 */
	public abstract CodeSignature getMethodOrConstructor();
}