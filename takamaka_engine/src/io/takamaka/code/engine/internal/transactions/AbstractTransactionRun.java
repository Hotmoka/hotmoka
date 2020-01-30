package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
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
	public final Response response;

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
	public final EngineClassLoaderImpl classLoader;

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

		try (EngineClassLoaderImpl classLoader = mkClassLoader()) {
			this.classLoader = classLoader;
			this.now = node.getNow();
			this.response = computeResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	protected abstract EngineClassLoaderImpl mkClassLoader() throws Exception;

	@Override
	public final long now() {
		return now;
	}

	@Override
	public final TransactionReference getCurrentTransaction() {
		return current;
	}

	protected abstract Response computeResponse() throws Exception;

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
		MethodSignature method = (MethodSignature) executor.methodOrConstructor;
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
		MethodSignature method = (MethodSignature) executor.methodOrConstructor;
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

		return classLoader.resolveConstructor(executor.methodOrConstructor.definingClass.name, argTypes)
			.orElseThrow(() -> new NoSuchMethodException(executor.methodOrConstructor.toString()));
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

		return classLoader.resolveConstructor(executor.methodOrConstructor.definingClass.name, argTypes)
			.orElseThrow(() -> new NoSuchMethodException(executor.methodOrConstructor.toString()));
	}

	/**
	 * Yields the classes of the formal arguments of the method or constructor.
	 * 
	 * @return the array of classes, in the same order as the formals
	 * @throws ClassNotFoundException if some class cannot be found
	 */
	public final Class<?>[] formalsAsClass(CodeExecutor<?,?> executor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: executor.methodOrConstructor.formals().collect(Collectors.toList()))
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
		for (StorageType type: executor.methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(storageTypeToClass.toClass(type));

		classes.add(classLoader.getContract());
		classes.add(Dummy.class);

		return classes.toArray(new Class<?>[classes.size()]);
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
}