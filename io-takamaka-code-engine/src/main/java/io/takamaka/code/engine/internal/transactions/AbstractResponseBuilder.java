package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.OutOfGasError;
import io.takamaka.code.engine.ResponseBuilder;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.Serializer;
import io.takamaka.code.engine.internal.SizeCalculator;
import io.takamaka.code.engine.internal.StorageTypeToClass;
import io.takamaka.code.engine.internal.UpdatesExtractor;

/**
 * A generic implementation of the creator of a response.
 *
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class AbstractResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> implements ResponseBuilder<Response> {

	/**
	 * The HotMoka node that is creating the response.
	 */
	public final Node node;

	/**
	 * The request of the transaction.
	 */
	public final Request request;

	/**
	 * The object that knows about the size of data once serialized.
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
	 * The object that can be used to extract the updates to a set of storage objects,
	 * induced by the run of the transaction.
	 */
	public final UpdatesExtractor updatesExtractor = new UpdatesExtractor(this);

	/**
	 * The reference that must be used to refer to the created transaction.
	 */
	public final TransactionReference current;

	/**
	 * The events accumulated during the transaction.
	 */
	private final List<Object> events = new ArrayList<>();

	/**
	 * The time of execution of the transaction.
	 */
	private final long now;

	/**
	 * The counter for the next storage object created during the transaction.
	 */
	private BigInteger nextProgressive = BigInteger.ZERO;

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the response
	 * @param current the reference that must be used to refer to the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected AbstractResponseBuilder(Request request, TransactionReference current, Node node) throws TransactionRejectedException {
		try {
			this.request = request;
			this.node = node;
			this.current = current;
			this.now = node.getNow();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	/**
	 * Yields the class loader used for the transaction being created.
	 * 
	 * @return the class loader
	 */
	public abstract EngineClassLoader getClassLoader();

	/**
	 * Decreases the available gas by the given amount, for CPU execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public abstract void chargeGasForCPU(BigInteger amount);

	/**
	 * Decreases the available gas by the given amount, for RAM execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public abstract void chargeGasForRAM(BigInteger amount);

	/**
	 * Runs a given piece of code with a subset of the available gas.
	 * It first charges the given amount of gas. Then runs the code
	 * with the charged gas only. At its end, the remaining gas is added
	 * to the available gas to continue the computation.
	 * 
	 * @param amount the amount of gas provided to the code
	 * @param what the code to run
	 * @return the result of the execution of the code
	 * @throws OutOfGasError if there is not enough gas
	 * @throws Exception if the code runs into this exception
	 */
	public abstract <T> T withGas(BigInteger amount, Callable<T> what) throws Exception;

	/**
	 * Yields the next storage reference for the current transaction.
	 * This can be used to associate a storage reference to each new
	 * storage object created during a transaction.
	 * 
	 * @return the next storage reference
	 */
	public final StorageReference getNextStorageReference() {
		BigInteger result = nextProgressive;
		nextProgressive = nextProgressive.add(BigInteger.ONE);
		return StorageReference.mk(current, result);
	}

	/**
	 * Yields the UTC time when the transaction is being run.
	 * This might be for instance the time of creation of a block where the transaction
	 * will be stored, but the detail is left to the implementation.
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
			throw new NullPointerException("an event cannot be null");

		events.add(event);
	}

	/**
	 * Yields the latest value for the given field, of lazy type, of the object with the given storage reference.
	 * The field is {@code final}. Conceptually, this method looks for the value of the field
	 * in the transaction where the reference was created.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	public final Object deserializeLastLazyUpdateFor(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(node.getLastLazyUpdateToNonFinalFieldOf(reference, field, this::chargeGasForCPU).getValue());
	}

	/**
	 * Yields the latest value for the given field, of lazy type, of the object with the given storage reference.
	 * The field is {@code final}. Conceptually, this method looks for the value of the field
	 * in the transaction where the reference was created.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	public final Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(node.getLastLazyUpdateToFinalFieldOf(reference, field, this::chargeGasForCPU).getValue());
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
	protected final static TransactionRejectedException wrapAsTransactionRejectedException(Throwable t) {
		return t instanceof TransactionRejectedException ? (TransactionRejectedException) t : new TransactionRejectedException(t);
	}

	/**
	 * A thread that executes Takamaka code as part of this transaction.
	 */
	public abstract class TakamakaThread extends Thread {
		protected TakamakaThread() {}

		/**
		 * The exception that occurred during the transaction, if any.
		 */
		private Throwable exception;

		@Override
		public final void run() {
			try {
				body();
			}
			catch (Throwable t) {
				exception = t;
			}
		}

		/**
		 * Starts the thread, waits for its conclusion and throws its exception, if any.
		 * 
		 * @throws Throwable the exception generated during the execution of {@linkplain #body()}, if any
		 */
		public final void go() throws Throwable {
			start();
			join();
			if (exception != null)
				throw exception;
		}

		protected abstract void body() throws Exception;

		/**
		 * Yields the builder for which the transaction is executed.
		 * 
		 * @return the builder
		 */
		public final AbstractResponseBuilder<?,?> getBuilder() {
			return AbstractResponseBuilder.this;
		}
	}
}