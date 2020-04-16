package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;

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
public abstract class AbstractResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> implements ResponseBuilder<Response> {

	/**
	 * The HotMoka node that is creating the response.
	 */
	public final AbstractNode node;

	/**
	 * The request of the transaction.
	 */
	public final Request request;

	/**
	 * The object that knows about the size of data once serialized.
	 */
	public final SizeCalculator sizeCalculator = new SizeCalculator(this);

	/**
	 * The object that translates storage types into their run-time class tag.
	 */
	public final StorageTypeToClass storageTypeToClass = new StorageTypeToClass(this);

	/**
	 * The class loader used for the transaction.
	 */
	private final EngineClassLoader classLoader;

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

	/**
	 * Creates the class loader for computing the response.
	 * 
	 * @return the class loader
	 */
	protected abstract EngineClassLoader mkClassLoader() throws Exception;

	/**
	 * Yields the class loader used for the transaction being created.
	 * 
	 * @return the class loader
	 */
	public final EngineClassLoader getClassLoader() {
		return classLoader;
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

		protected ResponseCreator() throws TransactionRejectedException {
			try {
				deserializer = new Deserializer(this);
				updatesExtractor = new UpdatesExtractor(AbstractResponseBuilder.this);
			}
			catch (Throwable t) {
				throw new TransactionRejectedException(t);
			}
		}

		/**
		 * Yields the builder for which the creator works.
		 * 
		 * @return the builder
		 */
		public final AbstractResponseBuilder<?,?> getBuilder() {
			return AbstractResponseBuilder.this;
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
		 * A thread that executes Takamaka code as part of this transaction.
		 */
		public abstract class TakamakaThread extends Thread {
		
			/**
			 * The reference that must be used to refer to the created transaction.
			 */
			private final TransactionReference current;
		
			/**
			 * The time of execution of the transaction.
			 */
			private final long now;
		
			/**
			 * The exception that occurred during the transaction, if any.
			 */
			private Throwable exception;
		
			/**
			 * The counter for the next storage object created during the transaction.
			 */
			private BigInteger nextProgressive = BigInteger.ZERO;
		
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
			 * Builds the thread.
			 * 
			 * @param current the reference of the transaction that is creating the response
			 * @throws Exception if the thread cannot be created
			 */
			protected TakamakaThread(TransactionReference current) throws Exception {
				this.current = current;
				this.now = node.getNow();
			}
		
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
			 * Yields the builder for which the thread works.
			 * 
			 * @return the builder
			 */
			public final AbstractResponseBuilder<?,?> getBuilder() {
				return AbstractResponseBuilder.this;
			}

			/**
			 * Yields the response creator for which the thread works.
			 * 
			 * @return the response creator
			 */
			public final ResponseCreator getResponseCreator() {
				return ResponseCreator.this;
			}
		}
	}
}