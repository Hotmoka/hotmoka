package io.takamaka.code.engine.internal.executors;

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
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.EngineClassLoader;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.NonWhiteListedCallException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;
import io.takamaka.code.engine.runtime.Runtime;
import io.takamaka.code.verification.Dummy;
import io.takamaka.code.whitelisting.WhiteListingProofObligation;

/**
 * The thread that executes a constructor or method of a Takamaka object. It creates the class loader
 * from the class path and deserializes receiver and actuals (if any). It then calls the code and serializes
 * the resulting value back (if any).
 */
public abstract class CodeExecutor<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends TransactionExecutor<Request, Response> {
	public final UpdateOfBalance balanceUpdateInCaseOfFailure;

	/**
	 * The class loader of the transaction being executed.
	 */
	protected final EngineClassLoaderImpl classLoader;

	/**
	 * The exception resulting from the execution of the method or constructor, if any.
	 * This is {@code null} if the execution completed without exception.
	 */
	public Throwable exception;

	/**
	 * The resulting value for methods or the created object for constructors.
	 * This is {@code null} if the execution completed with an exception or
	 * if the method actually returned {@code null}.
	 */
	public Object result;

	/**
	 * The deserialized caller.
	 */
	public final Object deserializedCaller;

	/**
	 * The method or constructor that is being called.
	 */
	public final CodeSignature methodOrConstructor;

	/**
	 * True if the method has been called correctly and it is declared as {@code void},
	 */
	public boolean isVoidMethod;

	/**
	 * True if the method has been called correctly and it is annotated as {@link io.takamaka.code.lang.View}.
	 */
	public boolean isViewMethod;

	/**
	 * The deserialized receiver of a method call. This is {@code null} for static methods and constructors.
	 */
	protected final Object deserializedReceiver; // it might be null

	/**
	 * The deserialized actual arguments of the call.
	 */
	protected final Object[] deserializedActuals;

	/**
	 * The events accumulated during the execution.
	 */
	private final List<Object> events = new ArrayList<>();

	/**
	 * The time of execution of this transaction.
	 */
	private final long now;

	/**
	 * Builds the executor of a method or constructor.
	 * 
	 * @param run the engine for which code is being executed
	 * @param classLoader the class loader that must be used to find the classes during the execution of the method or constructor
	 * @param methodOrConstructor the method or constructor to call
	 * @param receiver the receiver of the call, if any. This is {@code null} for constructors and static methods
	 * @param actuals the actuals provided to the method or constructor
	 * @throws TransactionException 
	 */
	protected CodeExecutor(AbstractTransactionRun<Request, Response> run, CodeSignature methodOrConstructor, StorageReference receiver, Stream<StorageValue> actuals) throws IllegalTransactionRequestException, TransactionException {
		super(run);

		try {
			deserializedCaller = run.deserializer.deserialize(run.request.caller);
			run.checkIsExternallyOwned(deserializedCaller);

			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			balanceUpdateInCaseOfFailure = run.checkMinimalGas(run.request, deserializedCaller);
			run.chargeForCPU(run.node.getGasCostModel().cpuBaseTransactionCost());
			run.chargeForStorage(run.sizeCalculator.sizeOf(run.request));
			Runtime.init(this);
			this.now = run.node.getNow();
			this.classLoader = run.classLoader;
			this.methodOrConstructor = methodOrConstructor;
			this.deserializedReceiver = receiver != null ? run.deserializer.deserialize(receiver) : null;
			this.deserializedActuals = actuals.map(run.deserializer::deserialize).toArray(Object[]::new);
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new IllegalTransactionRequestException(t);
		}
	}

	/**
	 * Yields the UTC time when the transaction is being run.
	 * This might be for instance the time of creation of the block where the transaction
	 * occurs, but the detail is left to the implementation.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	public final long now() {
		return now;
	}

	/**
	 * Takes note of the given event, emitted during this execution.
	 * 
	 * @param event the event
	 */
	public final void event(Object event) {
		if (event == null)
			throw new IllegalArgumentException("an event cannot be null");

		events.add(event);
	}

	/**
	 * Yields the class loader used by this executor.
	 * 
	 * @return the class loader
	 */
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Yields the storage references of the events generated so far.
	 * 
	 * @return the storage references
	 */
	public final Stream<StorageReference> events() {
		return events.stream().map(classLoader::getStorageReferenceOf);
	}

	private SortedSet<Update> updates;

	/**
	 * Collects all updates reachable from the actuals or from the caller, receiver or result of a method call.
	 * 
	 * @return the updates, sorted
	 */
	public final Stream<Update> updates() {
		if (updates != null)
			return updates.stream();

		List<Object> potentiallyAffectedObjects = new ArrayList<>();
		if (deserializedCaller != null)
			potentiallyAffectedObjects.add(deserializedCaller);
		if (deserializedReceiver != null)
			potentiallyAffectedObjects.add(deserializedReceiver);
		Class<?> storage = classLoader.getStorage();
		if (result != null && storage.isAssignableFrom(result.getClass()))
			potentiallyAffectedObjects.add(result);

		if (deserializedActuals != null)
			for (Object actual: deserializedActuals)
				if (actual != null && storage.isAssignableFrom(actual.getClass()))
					potentiallyAffectedObjects.add(actual);

		// events are accessible from outside, hence they count as side-effects
		events.forEach(potentiallyAffectedObjects::add);

		return (updates = run.updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream()).collect(Collectors.toCollection(TreeSet::new))).stream();
	}

	/**
	 * Resolves the method that must be called.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws SecurityException if the method could not be accessed
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	protected final Method getMethod() throws ClassNotFoundException, NoSuchMethodException {
		MethodSignature method = (MethodSignature) methodOrConstructor;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? run.storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClass();

		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	/**
	 * Resolves the method that must be called, assuming that it is an entry.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws SecurityException if the method could not be accessed
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	protected final Method getEntryMethod() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
		MethodSignature method = (MethodSignature) methodOrConstructor;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? run.storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClassForEntry();

		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	/**
	 * Determines if the execution only affected the balance of the caller contract.
	 *
	 * @param deserializedCaller the caller contract
	 * @return true if and only if that condition holds
	 */
	public final boolean onlyAffectedBalanceOf(Object deserializedCaller) {
		return updates().allMatch
			(update -> update.object.equals(classLoader.getStorageReferenceOf(deserializedCaller))
						&& update instanceof UpdateOfField
						&& ((UpdateOfField) update).getField().equals(FieldSignature.BALANCE_FIELD));
	}

	/**
	 * Yields the classes of the formal arguments of the method or constructor.
	 * 
	 * @return the array of classes, in the same order as the formals
	 * @throws ClassNotFoundException if some class cannot be found
	 */
	protected final Class<?>[] formalsAsClass() throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(run.storageTypeToClass.toClass(type));

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
		for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(run.storageTypeToClass.toClass(type));

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

	protected final boolean isChecked(Throwable t) {
		return !(t instanceof RuntimeException || t instanceof Error);
	}

	/**
	 * Yields the same exception, if it is checked and the executable is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
	 * Otherwise, yields its cause.
	 * 
	 * @param e the exception
	 * @param executable the method or constructor whose execution has thrown the exception
	 * @return the same exception, or its cause
	 */
	protected final Throwable unwrapInvocationException(InvocationTargetException e, Executable executable) {
		if (isChecked(e.getCause()) && hasAnnotation(executable, ClassType.THROWS_EXCEPTIONS.name))
			return e;
		else
			return e.getCause();
	}

	/**
	 * Checks that the given method or constructor can be called from Takamaka code, that is,
	 * is white-listed and its white-listing proof-obligations hold.
	 * 
	 * @param executable the method or constructor
	 * @param actuals the actual arguments passed to {@code executable}, including the
	 *                receiver for instance methods
	 * @throws ClassNotFoundException if some class could not be found during the check
	 */
	protected final void ensureWhiteListingOf(Executable executable, Object[] actuals) throws ClassNotFoundException {
		Optional<? extends Executable> model;
		if (executable instanceof Constructor<?>) {
			model = classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed constructor of "
						+ ((ConstructorSignature) methodOrConstructor).definingClass.name);
		}
		else {
			model = classLoader.getWhiteListingWizard().whiteListingModelOf((Method) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed method "
						+ ((MethodSignature) methodOrConstructor).definingClass.name + "." + ((MethodSignature) methodOrConstructor).methodName);
		}

		if (executable instanceof java.lang.reflect.Method && !Modifier.isStatic(executable.getModifiers()))
			checkWhiteListingProofObligations(model.get().getName(), deserializedReceiver, model.get().getAnnotations());

		Annotation[][] anns = model.get().getParameterAnnotations();
		for (int pos = 0; pos < anns.length; pos++)
			checkWhiteListingProofObligations(model.get().getName(), actuals[pos], anns[pos]);
	}

	private void checkWhiteListingProofObligations(String methodName, Object value, Annotation[] annotations) {
		Stream.of(annotations)
		.map(Annotation::annotationType)
		.map(this::getWhiteListingCheckFor)
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

	private Optional<Method> getWhiteListingCheckFor(Class<? extends Annotation> annotationType) {
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

	protected final static boolean hasAnnotation(Executable executable, String annotationName) {
		return Stream.of(executable.getAnnotations())
			.anyMatch(annotation -> annotation.annotationType().getName().equals(annotationName));
	}

	/**
	 * Checks if the given object is a red/green externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not a red/green externally owned account
	 */
	protected void checkIsRedGreenExternallyOwned(Object object) throws ClassNotFoundException, IllegalTransactionRequestException {
		Class<? extends Object> clazz = object.getClass();
		if (!classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new IllegalTransactionRequestException("Only a red/green externally owned contract can start a transaction for a @RedPayable method or constructor");
	}
}