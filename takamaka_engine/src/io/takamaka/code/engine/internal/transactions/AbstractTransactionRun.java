package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
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
import io.takamaka.code.engine.runtime.Runtime;

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
			this.response = computeResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	protected abstract EngineClassLoaderImpl mkClassLoader() throws Exception;

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