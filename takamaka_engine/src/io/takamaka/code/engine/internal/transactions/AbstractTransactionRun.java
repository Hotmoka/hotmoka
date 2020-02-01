package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
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
	 * The HotMoka node that is running the transaction.
	 */
	public final Node node;

	/**
	 * The object that knows about the size of data once stored in blockchain.
	 */
	public final SizeCalculator sizeCalculator = new SizeCalculator(this);

	/**
	 * The object that serializes RAM values into storage objects.
	 */
	public final Serializer serializer = new Serializer(this);

	/**
	 * The object that deserializes storage objects into RAM values.
	 */
	public final Deserializer deserializer = new Deserializer(this);

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
	 * The events accumulated during the transaction.
	 */
	private final List<Object> events = new ArrayList<>();

	/**
	 * The reference to the transaction where this must be executed.
	 */
	private final TransactionReference current;

	/**
	 * The time of execution of this transaction.
	 */
	private final long now;

	protected AbstractTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		Runtime.init(this);
		ClassType.clearCache();
		FieldSignature.clearCache();
		this.node = node;
		this.current = current;

		try {
			this.now = node.getNow();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	public abstract EngineClassLoaderImpl getClassLoader();

	public abstract Response getResponse();

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
	 * Yields the events generated so far.
	 * 
	 * @return the events
	 */
	protected final Stream<Object> events() {
		return events.stream();
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