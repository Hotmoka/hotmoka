package io.takamaka.code.engine.internal.transactions;

import static io.takamaka.code.engine.internal.runtime.Runtime.responseCreators;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.OutOfGasError;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.ResponseBuilder;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.SizeCalculator;
import io.takamaka.code.engine.internal.StorageTypeToClass;
import io.takamaka.code.engine.internal.UpdatesExtractor;

/**
 * A generic implementation of the creator of a response.
 *
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class AbstractResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> implements ResponseBuilder<Request, Response> {

	/**
	 * The HotMoka node that is creating the response.
	 */
	public final AbstractNode node;

	/**
	 * The object that translates storage types into their run-time class tag.
	 */
	public final StorageTypeToClass storageTypeToClass = new StorageTypeToClass(this);

	/**
	 * The class loader used for the transaction.
	 */
	public final EngineClassLoader classLoader;

	/**
	 * The request of the transaction.
	 */
	protected final Request request;

	/**
	 * The object that knows about the size of data once serialized.
	 */
	protected final SizeCalculator sizeCalculator = new SizeCalculator(this);

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the response
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected AbstractResponseBuilder(Request request, AbstractNode node) throws TransactionRejectedException {
		try {
			this.request = request;
			this.node = node;
			this.classLoader = mkClassLoader();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final Request getRequest() {
		return request;
	}

	/**
	 * Creates the class loader for computing the response.
	 * 
	 * @return the class loader
	 */
	protected abstract EngineClassLoader mkClassLoader() throws Exception;

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message used for the {@link io.hotmoka.beans.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	protected final static TransactionRejectedException wrapAsTransactionRejectedException(Throwable t) {
		return t instanceof TransactionRejectedException ? (TransactionRejectedException) t : new TransactionRejectedException(t.getMessage());
	}

	/**
	 * The creator of a response. Its body runs in a thread, so that the
	 * {@linkplain io.takamaka.code.engine.internal.runtime.Runtime} class
	 * can recover it from its thread-local table.
	 */
	public abstract class ResponseCreator {

		/**
		 * The object that deserializes storage objects into RAM values.
		 */
		protected final Deserializer deserializer;

		/**
		 * The object that can be used to extract the updates to a set of storage objects,
		 * induced by the run of the transaction.
		 */
		protected final UpdatesExtractor updatesExtractor;

		/**
		 * The reference that must be used to refer to the created transaction.
		 */
		private final TransactionReference current;

		/**
		 * The time of execution of the transaction.
		 */
		private final long now;

		/**
		 * The counter for the next storage object created during the transaction.
		 */
		private BigInteger nextProgressive = BigInteger.ZERO;

		protected ResponseCreator(TransactionReference current) throws TransactionRejectedException {
			try {
				this.current = current;
				this.deserializer = new Deserializer(AbstractResponseBuilder.this, this::chargeGasForCPU);
				this.updatesExtractor = new UpdatesExtractor(AbstractResponseBuilder.this);
				this.now = node.getNow();
			}
			catch (Throwable t) {
				throw new TransactionRejectedException(t);
			}
		}

		protected final Response create() throws TransactionRejectedException {
			try {
				return node.submit(new TakamakaCallable(this::body)).get();
			}
			catch (ExecutionException e) {
				throw wrapAsTransactionRejectedException(e.getCause());
			}
			catch (Throwable t) {
				throw wrapAsTransactionRejectedException(t);
			}
		}

		/**
		 * The body of the creation of the response.
		 * 
		 * @return the response
		 * @throws Exception if the response could not be created
		 */
		protected abstract Response body() throws Exception;

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
		public abstract void event(Object event);

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
		 * Yields the next storage reference for the current transaction.
		 * This can be used to associate a storage reference to each new
		 * storage object created during a transaction.
		 * 
		 * @return the next storage reference
		 */
		public final StorageReference getNextStorageReference() {
			BigInteger result = nextProgressive;
			nextProgressive = nextProgressive.add(BigInteger.ONE);
			return new StorageReference(current, result);
		}

		/**
		 * Yields the class loader used for the transaction being created.
		 * 
		 * @return the class loader
		 */
		public final EngineClassLoader getClassLoader() {
			return classLoader;
		}

		/**
		 * A task that executes Takamaka code as part of this transaction.
		 * It sets the response creator in the thread-local of the runtime.
		 */
		private final class TakamakaCallable implements Callable<Response> {
			private final Callable<Response> body;

			private TakamakaCallable(Callable<Response> body) {
				this.body = body;
			}

			@Override
			public Response call() throws Exception {
				try {
					responseCreators.set(ResponseCreator.this);
					return body.call();
				}
				finally {
					responseCreators.remove();
				}
			}
		}
	}
}