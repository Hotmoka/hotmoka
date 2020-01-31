package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.NonWhiteListedCallException;
import io.takamaka.code.engine.TransactionRun;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.Serializer;
import io.takamaka.code.engine.internal.SizeCalculator;
import io.takamaka.code.engine.internal.StorageTypeToClass;
import io.takamaka.code.engine.internal.UpdatesExtractor;
import io.takamaka.code.engine.internal.executors.CodeExecutor;
import io.takamaka.code.engine.runtime.Runtime;
import io.takamaka.code.verification.Dummy;
import io.takamaka.code.whitelisting.WhiteListingProofObligation;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class AbstractTransactionRun<Request extends TransactionRequest<Response>, Response extends TransactionResponse> implements TransactionRun {

	/**
	 * The request of the transaction.
	 */
	public final Request request;

	/**
	 * The response computed for the transaction, starting from the request.
	 */
	public Response response;

	/**
	 * The object that knows about the size of data once stored in blockchain.
	 */
	public final SizeCalculator sizeCalculator;

	/**
	 * The object that serializes RAM values into storage objects.
	 */
	public final Serializer serializer = new Serializer(this);

	/**
	 * The object that deserializes storage objects into RAM values.
	 */
	public final Deserializer deserializer;

	/**
	 * The object that translates storage types into their run-time class tag.
	 */
	public final StorageTypeToClass storageTypeToClass = new StorageTypeToClass(this);

	/**
	 * The object that can be used to extract the updates to a set of storage objects
	 * induced by the run of the transaction.
	 */
	public final UpdatesExtractor updatesExtractor = new UpdatesExtractor(this);

	/**
	 * The HotMoka node that is running the transaction.
	 */
	public final Node node;

	/**
	 * The class loader for the transaction currently being executed.
	 */
	public EngineClassLoaderImpl classLoader;

	/**
	 * The events accumulated during the transaction.
	 */
	private final List<Object> events = new ArrayList<>();

	/**
	 * The reference to the transaction where this must be executed.
	 */
	private final TransactionReference current;

	/**
	 * The amount of gas consumed for CPU execution.
	 */
	protected BigInteger gasConsumedForCPU = BigInteger.ZERO;

	/**
	 * The amount of gas consumed for RAM allocation.
	 */
	protected BigInteger gasConsumedForRAM = BigInteger.ZERO;

	/**
	 * The amount of gas consumed for storage consumption.
	 */
	protected BigInteger gasConsumedForStorage = BigInteger.ZERO;

	/**
	 * A stack of available gas. When a sub-computation is started
	 * with a subset of the available gas, the latter is taken away from
	 * the current available gas and pushed on top of this stack.
	 */
	protected final LinkedList<BigInteger> oldGas = new LinkedList<>();

	/**
	 * The remaining amount of gas for the current transaction, not yet consumed.
	 */
	protected BigInteger gas;

	/**
	 * The time of execution of this transaction.
	 */
	private final long now;

	protected AbstractTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		this.request = request;
		this.gas = request instanceof NonInitialTransactionRequest ? ((NonInitialTransactionRequest<?>) request).gas : BigInteger.valueOf(-1);
		Runtime.init(this);
		ClassType.clearCache();
		FieldSignature.clearCache();
		this.node = node;
		this.deserializer = new Deserializer(this);
		this.sizeCalculator = new SizeCalculator(node.getGasCostModel());
		this.current = current;

		try {
			this.now = node.getNow();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	@Override
	public final long now() {
		return now;
	}

	@Override
	public final TransactionReference getCurrentTransaction() {
		return current;
	}

	@Override
	public final void event(Object event) {
		if (event == null)
			throw new IllegalArgumentException("an event cannot be null");

		events.add(event);
	}

	@Override
	public void chargeForCPU(BigInteger amount) {
	}

	@Override
	public void chargeForRAM(BigInteger amount) {
	}

	@Override
	public void chargeForStorage(BigInteger amount) {
	}

	@Override
	public <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		return what.call();
	}

	@Override
	public final Object deserializeLastLazyUpdateFor(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(node.getLastLazyUpdateToNonFinalFieldOf(reference, field, this::chargeForCPU).getValue());
	}

	@Override
	public final Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(node.getLastLazyUpdateToFinalFieldOf(reference, field, this::chargeForCPU).getValue());
	}

	/**
	 * Checks if the given object is an externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not an externally owned account
	 */
	public final void checkIsExternallyOwned(Object object) throws IllegalTransactionRequestException {
		Class<? extends Object> clazz = object.getClass();
		if (!classLoader.getExternallyOwnedAccount().isAssignableFrom(clazz)
				&& !classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new IllegalTransactionRequestException("Only an externally owned account can start a transaction");
	}

	/**
	 * Checks if the given object is a red/green externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not a red/green externally owned account
	 */
	public final void checkIsRedGreenExternallyOwned(Object object) throws ClassNotFoundException, IllegalTransactionRequestException {
		Class<?> clazz = object.getClass();
		if (!classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new IllegalTransactionRequestException("Only a red/green externally owned contract can start a transaction for a @RedPayable method or constructor");
	}

	/**
	 * Resolves the method that must be called.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws SecurityException if the method could not be accessed
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	public final Method getMethod(CodeExecutor<?,?> executor) throws ClassNotFoundException, NoSuchMethodException {
		MethodSignature method = (MethodSignature) ((MethodCallTransactionRun<?>) this).method;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClass(executor);

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
	public final Method getEntryMethod(CodeExecutor<?,?> executor) throws NoSuchMethodException, SecurityException, ClassNotFoundException {
		MethodSignature method = (MethodSignature) ((MethodCallTransactionRun<?>) this).method;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClassForEntry(executor);

		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	/**
	 * Resolves the constructor that must be called.
	 * 
	 * @return the constructor
	 * @throws NoSuchMethodException if the constructor could not be found
	 * @throws SecurityException if the constructor could not be accessed
	 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
	 */
	public final Constructor<?> getConstructor(CodeExecutor<?,?> executor) throws ClassNotFoundException, NoSuchMethodException {
		Class<?>[] argTypes = formalsAsClass(executor);

		return classLoader.resolveConstructor(((ConstructorCallTransactionRun) this).constructor.definingClass.name, argTypes)
			.orElseThrow(() -> new NoSuchMethodException(((ConstructorCallTransactionRun) this).constructor.toString()));
	}

	/**
	 * Resolves the constructor that must be called, assuming that it is an entry.
	 * 
	 * @return the constructor
	 * @throws NoSuchMethodException if the constructor could not be found
	 * @throws SecurityException if the constructor could not be accessed
	 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
	 */
	public final Constructor<?> getEntryConstructor(CodeExecutor<?,?> executor) throws ClassNotFoundException, NoSuchMethodException {
		Class<?>[] argTypes = formalsAsClassForEntry(executor);

		return classLoader.resolveConstructor(((ConstructorCallTransactionRun) this).constructor.definingClass.name, argTypes)
			.orElseThrow(() -> new NoSuchMethodException(((ConstructorCallTransactionRun) this).constructor.toString()));
	}

	/**
	 * Yields the classes of the formal arguments of the method or constructor.
	 * 
	 * @return the array of classes, in the same order as the formals
	 * @throws ClassNotFoundException if some class cannot be found
	 */
	public final Class<?>[] formalsAsClass(CodeExecutor<?,?> executor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: ((CodeCallTransactionRun<?,?>) this).getMethodOrConstructor().formals().collect(Collectors.toList()))
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
	public final Class<?>[] formalsAsClassForEntry(CodeExecutor<?,?> executor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: ((CodeCallTransactionRun<?,?>) this).getMethodOrConstructor().formals().collect(Collectors.toList()))
			classes.add(storageTypeToClass.toClass(type));

		classes.add(classLoader.getContract());
		classes.add(Dummy.class);

		return classes.toArray(new Class<?>[classes.size()]);
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
	public final Stream<Update> updates(CodeExecutor<?,?> executor) {
		if (updates != null)
			return updates.stream();

		List<Object> potentiallyAffectedObjects = new ArrayList<>();
		if (executor.deserializedCaller != null)
			potentiallyAffectedObjects.add(executor.deserializedCaller);
		if (executor.deserializedReceiver != null)
			potentiallyAffectedObjects.add(executor.deserializedReceiver);
		Class<?> storage = classLoader.getStorage();
		if (((CodeCallTransactionRun<?,?>)this).result != null && storage.isAssignableFrom(((CodeCallTransactionRun<?,?>)this).result.getClass()))
			potentiallyAffectedObjects.add(((CodeCallTransactionRun<?,?>)this).result);

		if (executor.deserializedActuals != null)
			for (Object actual: executor.deserializedActuals)
				if (actual != null && storage.isAssignableFrom(actual.getClass()))
					potentiallyAffectedObjects.add(actual);

		// events are accessible from outside, hence they count as side-effects
		events.forEach(potentiallyAffectedObjects::add);

		return (updates = updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects.stream()).collect(Collectors.toCollection(TreeSet::new))).stream();
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
	public final void ensureWhiteListingOf(CodeExecutor<?,?> executor, Executable executable, Object[] actuals) throws ClassNotFoundException {
		Optional<? extends Executable> model;
		if (executable instanceof Constructor<?>) {
			model = classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed constructor of "
						+ ((ConstructorCallTransactionRun) this).constructor.definingClass.name);
		}
		else {
			model = classLoader.getWhiteListingWizard().whiteListingModelOf((Method) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed method "
						+ ((MethodCallTransactionRun<?>) this).method.definingClass.name + "." + ((MethodCallTransactionRun<?>) this).method.methodName);
		}

		if (executable instanceof java.lang.reflect.Method && !Modifier.isStatic(executable.getModifiers()))
			checkWhiteListingProofObligations(model.get().getName(), executor.deserializedReceiver, model.get().getAnnotations());

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

	/**
	 * Adds to the actual parameters the implicit actuals that are passed
	 * to {@link io.takamaka.code.lang.Entry} methods or constructors. They are the caller of
	 * the entry and {@code null} for the dummy argument.
	 * 
	 * @return the resulting actual parameters
	 */
	public final Object[] addExtraActualsForEntry(CodeExecutor<?,?> executor) {
		int al = executor.deserializedActuals.length;
		Object[] result = new Object[al + 2];
		System.arraycopy(executor.deserializedActuals, 0, result, 0, al);
		result[al] = executor.deserializedCaller;
		result[al + 1] = null; // Dummy is not used

		return result;
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message used for the {@link io.hotmoka.beans.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	protected final static TransactionException wrapAsTransactionException(Throwable t, String message) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(message, t);
	}

	private static boolean isChecked(Throwable t) {
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
	public final static Throwable unwrapInvocationException(InvocationTargetException e, Executable executable) {
		return isChecked(e.getCause()) && hasAnnotation(executable, ClassType.THROWS_EXCEPTIONS.name) ? e : e.getCause();
	}

	public final static boolean hasAnnotation(Executable executable, String annotationName) {
		return Stream.of(executable.getAnnotations())
			.anyMatch(annotation -> annotation.annotationType().getName().equals(annotationName));
	}
}