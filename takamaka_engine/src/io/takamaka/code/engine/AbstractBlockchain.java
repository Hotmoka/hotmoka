package io.takamaka.code.engine;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.internal.CodeExecutor;
import io.takamaka.code.engine.internal.ConstructorExecutor;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.InstanceMethodExecutor;
import io.takamaka.code.engine.internal.Serializer;
import io.takamaka.code.engine.internal.SizeCalculator;
import io.takamaka.code.engine.internal.StaticMethodExecutor;
import io.takamaka.code.engine.internal.StorageTypeToClass;
import io.takamaka.code.engine.internal.TempJarFile;
import io.takamaka.code.engine.internal.UpdatesExtractor;
import io.takamaka.code.engine.runtime.Runtime;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class AbstractBlockchain implements Engine {

	/**
	 * The events accumulated during the current transaction. This is reset at each transaction.
	 */
	private final List<Object> events = new ArrayList<>();

	/**
	 * The gas cost model of this blockchain.
	 */
	public final GasCostModel gasCostModel = getGasCostModel(); //TODO: too visible

	/**
	 * The object that knows about the size of data once stored in blockchain.
	 */
	private final SizeCalculator sizeCalculator = new SizeCalculator(gasCostModel);

	/**
	 * The object that serializes RAM values into storage objects.
	 */
	private final Serializer serializer = new Serializer(this);

	/**
	 * The object that deserializes storage objects into RAM values.
	 */
	public final Deserializer deserializer = new Deserializer(this, this::getLastEagerUpdatesFor);

	/**
	 * The object that translates storage types into their run-time class tag.
	 */
	public final StorageTypeToClass storageTypeToClass = new StorageTypeToClass(this);

	/**
	 * The remaining amount of gas for the current transaction, not yet consumed.
	 */
	private BigInteger gas;

	/**
	 * The amount of gas consumed for CPU execution.
	 */
	private BigInteger gasConsumedForCPU;

	/**
	 * The amount of gas consumed for RAM allocation.
	 */
	private BigInteger gasConsumedForRAM;

	/**
	 * The amount of gas consumed for storage consumption.
	 */
	private BigInteger gasConsumedForStorage;

	/**
	 * A stack of available gas. When a sub-computation is started
	 * with a subset of the available gas, the latter is taken away from
	 * the current available gas and pushed on top of this stack.
	 */
	private final LinkedList<BigInteger> oldGas = new LinkedList<>();

	/**
	 * The class loader for the transaction currently being executed.
	 */
	public EngineClassLoader classLoader;

	/**
	 * The reference to the transaction where this must be executed.
	 */
	private TransactionReference current;

	// ABSTRACT TEMPLATE METHODS
	// Any implementation of a blockchain must implement the following and leave the rest unchanged
	
	/**
	 * Yields a transaction reference whose {@code toString()} is the given string.
	 * 
	 * @param toString the result of {@code toString()} on the desired transaction reference
	 * @return the transaction reference
	 */
	protected abstract TransactionReference getTransactionReferenceFor(String toString);

	/**
	 * Yields the request that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the request
	 * @throws Exception if the request could not be found
	 */
	protected abstract TransactionRequest getRequestAt(TransactionReference transaction) throws Exception;

	/**
	 * Yields the response that was generated by the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	protected abstract TransactionResponse getResponseAt(TransactionReference transaction) throws Exception;

	/**
	 * Yields the most recent eager updates for the given storage reference.
	 * 
	 * @param reference the storage reference
	 * @return the updates. These must include the class tag update for the reference
	 * @throws Exception if the updates cannot be found
	 */
	protected abstract Stream<Update> getLastEagerUpdatesFor(StorageReference reference) throws Exception;

	/**
	 * Yields the most recent update for the given non-{@code final} field,
	 * of lazy type, of the object at given storage reference.
	 * Conceptually, this amounts to scanning backwards the blockchain, from its tip,
	 * looking for the latest update.
	 * 
	 * @param object the storage reference
	 * @param field the field whose update is being looked for
	 * @return the update, if any
	 * @throws Exception if the update could not be found
	 */
	protected abstract UpdateOfField getLastLazyUpdateToNonFinalFieldOf(StorageReference object, FieldSignature field) throws Exception;

	/**
	 * Yields the most recent update for the given {@code final} field,
	 * of lazy type, of the object at given storage reference.
	 * Conceptually, this amounts to accessing the storage reference when the object was
	 * created and reading the value of the field there. Its implementation can be identical to
	 * that of {@link #getLastLazyUpdateToNonFinalFieldOf(StorageReference, FieldSignature)},
	 * or exploit the fact that the field is {@code final}, for an optimized look-up.
	 * 
	 * @param object the storage reference
	 * @param field the field whose update is being looked for
	 * @return the update, if any
	 * @throws Exception if the update could not be found
	 */
	protected abstract UpdateOfField getLastLazyUpdateToFinalFieldOf(StorageReference object, FieldSignature field) throws Exception;

	/**
	 * Yields the UTC time when the currently executing transaction is being run.
	 * This might be for instance the time of creation of the block where the transaction
	 * occurs, but the detail is left to the implementation. In any case, this
	 * time must be the same for a given transaction, if it gets executed more times.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	public abstract long getNow();

	/**
	 * Yields the gas cost model of this blockchain.
	 * 
	 * @return the standard gas cost model. Subclasses may redefine
	 */
	protected GasCostModel getGasCostModel() {
		return GasCostModel.standard();
	}

	/**
	 * Initializes the state at the beginning of the execution of a new transaction
	 * 
	 * @param gas the amount of gas available for the transaction
	 * @param previous the transaction reference after which the transaction is being executed.
	 *                 If this is the first transaction, then {@code previous} will be {@code null}
	 * @param current the reference to the transaction where this must be executed
	 * @throws Exception if the transaction could not be initialized
	 */
	protected void initTransaction(BigInteger gas, TransactionReference current) throws Exception {
		Runtime.init(AbstractBlockchain.this); // this blockchain will be used during the execution of the code
		events.clear();
		deserializer.init();
		ClassType.clearCache();
		FieldSignature.clearCache();
		this.gas = gas;
		this.gasConsumedForCPU = BigInteger.ZERO;
		this.gasConsumedForRAM = BigInteger.ZERO;
		this.gasConsumedForStorage = BigInteger.ZERO;
		oldGas.clear();
		this.current = current;
	}

	/**
	 * Yields the reference to the transaction being executed.
	 * 
	 * @return the reference
	 */
	public final TransactionReference getCurrentTransaction() {
		return current;
	}

	// BLOCKCHAIN-AGNOSTIC IMPLEMENTATION

	/**
	 * Yields the request that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the request
	 * @throws Exception if the request could not be found
	 */
	//TODO: too visible
	public TransactionRequest getRequestAtAndCharge(TransactionReference transaction) throws Exception {
		chargeForCPU(gasCostModel.cpuCostForGettingRequestAt(transaction));
		return getRequestAt(transaction);
	}

	/**
	 * Yields the response that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	//TODO: too visible
	public final TransactionResponse getResponseAtAndCharge(TransactionReference transaction) throws Exception {
		chargeForCPU(gasCostModel.cpuCostForGettingResponseAt(transaction));
		return getResponseAt(transaction);
	}

	/**
	 * Yields the transaction reference that installed the jar
	 * where the given class is defined.
	 * 
	 * @param clazz the class
	 * @return the transaction reference
	 * @throws IllegalStateException if the transaction reference cannot be determined
	 */
	public final TransactionReference transactionThatInstalledJarFor(Class<?> clazz) {
		String className = clazz.getName();
		CodeSource src = clazz.getProtectionDomain().getCodeSource();
		if (src == null)
			throw new IllegalStateException("Cannot determine the jar of class "+ className);
		String classpath = src.getLocation().getPath();
		if (!classpath.endsWith(".jar"))
			throw new IllegalStateException("Unexpected class path " + classpath + " for class " + className);
		int start = classpath.lastIndexOf('@');
		if (start < 0)
			throw new IllegalStateException("Class path " + classpath + " misses @ separator");
		return getTransactionReferenceFor(classpath.substring(start + 1, classpath.length() - 4));
	}

	/**
	 * Decreases the available gas by the given amount, for CPU execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public final void chargeForCPU(BigInteger amount) {
		if (amount.signum() < 0)
			throw new IllegalArgumentException("Gas cannot increase");

		// gas can be negative only if it was initialized so; this special case is
		// used for the creation of the gamete, when gas should not be counted
		if (gas.signum() < 0)
			return;

		if (gas.compareTo(amount) < 0)
			// we report how much gas is missing
			throw new OutOfGasError();
	
		gas = gas.subtract(amount);
		gasConsumedForCPU = gasConsumedForCPU.add(amount);
	}

	/**
	 * Decreases the available gas by the given amount, for CPU execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	private void chargeForCPU(int amount) {
		chargeForCPU(BigInteger.valueOf(amount));
	}

	/**
	 * Decreases the available gas by the given amount, for RAM execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public final void chargeForRAM(BigInteger amount) {
		if (amount.signum() < 0)
			throw new IllegalArgumentException("Gas cannot increase");

		// gas can be negative only if it was initialized so; this special case is
		// used for the creation of the gamete, when gas should not be counted
		if (gas.signum() < 0)
			return;

		if (gas.compareTo(amount) < 0)
			// we report how much gas is missing
			throw new OutOfGasError();
	
		gas = gas.subtract(amount);
		gasConsumedForRAM = gasConsumedForRAM.add(amount);
	}

	/**
	 * Decreases the available gas by the given amount, for storage allocation.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public final void chargeForStorage(BigInteger amount) {
		if (amount.signum() < 0)
			throw new IllegalArgumentException("Gas cannot increase");

		// gas can be negative only if it was initialized so; this special case is
		// used for the creation of the gamete, when gas should not be counted
		if (gas.signum() < 0)
			return;

		if (gas.compareTo(amount) < 0)
			// we report how much gas is missing
			throw new OutOfGasError();
	
		gas = gas.subtract(amount);
		gasConsumedForStorage = gasConsumedForStorage.add(amount);
	}

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
	public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		chargeForCPU(amount);
		oldGas.addFirst(gas);
		gas = amount;
	
		try {
			return what.call();
		}
		finally {
			gas = gas.add(oldGas.removeFirst());
		}
	}

	/**
	 * Adds an event to those occurred during the execution of the current transaction.
	 * 
	 * @param event the event
	 * @throws IllegalArgumentException if the event is {@code null}
	 */
	public final void event(Object event) {
		if (event == null)
			throw new IllegalArgumentException("Events cannot be null");

		events.add(event);
	}

	@Override
	public final JarStoreInitialTransactionResponse runJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			// we do not count gas for this transaction
			initTransaction(BigInteger.valueOf(-1L), current);

			// we transform the array of bytes into a real jar file
			try (TempJarFile original = new TempJarFile(request.getJar());
				 EngineClassLoader jarClassLoader = new EngineClassLoader(original.toPath(), request.getDependencies(), this)) {
				VerifiedJar verifiedJar = VerifiedJar.of(original.toPath(), jarClassLoader, true);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasModelAsForInstrumentation());
				return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes());
			}
		});
	}

	@Override
	public final GameteCreationTransactionResponse runGameteCreationTransaction(GameteCreationTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			// we do not count gas for this creation
			initTransaction(BigInteger.valueOf(-1L), current);

			if (request.initialAmount.signum() < 0)
				throw new IllegalTransactionRequestException("The gamete must be initialized with a non-negative amount of coins");

			try (TakamakaClassLoader classLoader = this.classLoader = new EngineClassLoader(request.classpath, this)) {
				// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
				Object gamete = classLoader.getExternallyOwnedAccount().getDeclaredConstructor().newInstance();
				// we set the balance field of the gamete
				Field balanceField = classLoader.getContract().getDeclaredField("balance");
				balanceField.setAccessible(true); // since the field is private
				balanceField.set(gamete, request.initialAmount);

				return new GameteCreationTransactionResponse(collectUpdates(null, null, null, gamete).stream(), getStorageReferenceOf(gamete));
			}
		});
	}

	@Override
	public final GameteCreationTransactionResponse runRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			// we do not count gas for this creation
			initTransaction(BigInteger.valueOf(-1L), current);

			if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
				throw new IllegalTransactionRequestException("The gamete must be initialized with a non-negative amount of coins");

			try (TakamakaClassLoader classLoader = this.classLoader = new EngineClassLoader(request.classpath, this)) {
				// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
				Object gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
				// we set the balance field of the gamete
				Field balanceField = classLoader.getContract().getDeclaredField("balance");
				balanceField.setAccessible(true); // since the field is private
				balanceField.set(gamete, request.initialAmount);

				// we set the red balance field of the gamete
				Field redBalanceField = classLoader.getRedGreenContract().getDeclaredField("balanceRed");
				redBalanceField.setAccessible(true); // since the field is private
				redBalanceField.set(gamete, request.redInitialAmount);
	
				return new GameteCreationTransactionResponse(collectUpdates(null, null, null, gamete).stream(), getStorageReferenceOf(gamete));
			}
		});
	}

	@Override
	public final JarStoreTransactionResponse runJarStoreTransaction(JarStoreTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, current);

			try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
				this.classLoader = classLoader;
				Object deserializedCaller = deserializer.deserialize(request.caller);
				checkIsExternallyOwned(deserializedCaller);

				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction will be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(gasCostModel.cpuBaseTransactionCost());
					chargeForStorage(sizeCalculator.sizeOf(request));

					byte[] jar = request.getJar();
					chargeForCPU(gasCostModel.cpuCostForInstallingJar(jar.length));
					chargeForRAM(gasCostModel.ramCostForInstalling(jar.length));

					byte[] instrumentedBytes;
					// we transform the array of bytes into a real jar file
					try (TempJarFile original = new TempJarFile(jar);
						 EngineClassLoader jarClassLoader = new EngineClassLoader(original.toPath(), request.getDependencies(), this)) {
						VerifiedJar verifiedJar = VerifiedJar.of(original.toPath(), jarClassLoader, false);
						InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasModelAsForInstrumentation());
						instrumentedBytes = instrumentedJar.toBytes();
					}

					BigInteger balanceOfCaller = getBalanceOf(deserializedCaller);
					StorageReference storageReferenceOfDeserializedCaller = getStorageReferenceOf(deserializedCaller);
					UpdateOfBalance balanceUpdate = new UpdateOfBalance(storageReferenceOfDeserializedCaller, balanceOfCaller);
					JarStoreTransactionResponse response = new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(sizeCalculator.sizeOf(response));
					balanceOfCaller = increaseBalance(deserializedCaller);
					balanceUpdate = new UpdateOfBalance(storageReferenceOfDeserializedCaller, balanceOfCaller);
					return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				}
				catch (Throwable t) {
					// we do not pay back the gas
					BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForStorage);
					return new JarStoreTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
				}
			}
		});
	}

	/**
	 * Yields an adapter of the gas cost model for instrumentation.
	 * 
	 * @return the adapted gas model
	 */
	private io.takamaka.code.instrumentation.GasCostModel gasModelAsForInstrumentation() {
		return new io.takamaka.code.instrumentation.GasCostModel() {

			@Override
			public int cpuCostOfArithmeticInstruction() {
				return gasCostModel.cpuCostOfArithmeticInstruction();
			}

			@Override
			public int cpuCostOfArrayAccessInstruction() {
				return gasCostModel.cpuCostOfArrayAccessInstruction();
			}

			@Override
			public int cpuCostOfFieldAccessInstruction() {
				return gasCostModel.cpuCostOfFieldAccessInstruction();
			}

			@Override
			public int cpuCostOfInstruction() {
				return gasCostModel.cpuCostOfInstruction();
			}

			@Override
			public int cpuCostOfInvokeInstruction() {
				return gasCostModel.cpuCostOfInvokeInstruction();
			}

			@Override
			public int cpuCostOfMemoryAllocationInstruction() {
				return gasCostModel.cpuCostOfMemoryAllocationInstruction();
			}

			@Override
			public int cpuCostOfSelectInstruction() {
				return gasCostModel.cpuCostOfSelectInstruction();
			}

			@Override
			public int ramCostOfActivationRecord() {
				return gasCostModel.ramCostOfActivationRecord();
			}

			@Override
			public int ramCostOfActivationSlot() {
				return gasCostModel.ramCostOfActivationSlot();
			}

			@Override
			public int ramCostOfArray() {
				return gasCostModel.ramCostOfArray();
			}

			@Override
			public int ramCostOfArraySlot() {
				return gasCostModel.ramCostOfArraySlot();
			}

			@Override
			public int ramCostOfField() {
				return gasCostModel.ramCostOfField();
			}

			@Override
			public int ramCostOfObject() {
				return gasCostModel.ramCostOfObject();
			}
		};
	}

	@Override
	public final ConstructorCallTransactionResponse runConstructorCallTransaction(ConstructorCallTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, current);

			try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
				this.classLoader = classLoader;
				Object deserializedCaller = deserializer.deserialize(request.caller);
				checkIsExternallyOwned(deserializedCaller);
				
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction can be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(gasCostModel.cpuBaseTransactionCost());
					chargeForStorage(sizeCalculator.sizeOf(request));

					CodeExecutor executor = new ConstructorExecutor(this, request.constructor, deserializedCaller, request.actuals());
					executor.start();
					executor.join();

					if (executor.exception instanceof InvocationTargetException) {
						ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}

					if (executor.exception != null)
						throw executor.exception;

					ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
						((StorageReference) serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					return new ConstructorCallTransactionSuccessfulResponse
						((StorageReference) serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				}
				catch (Throwable t) {
					// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
					BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
					return new ConstructorCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
				}
			}
		});
	}

	@Override
	public final MethodCallTransactionResponse runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, current);

			try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
				this.classLoader = classLoader;
				Object deserializedCaller = deserializer.deserialize(request.caller);
				checkIsExternallyOwned(deserializedCaller);
				
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction can be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(gasCostModel.cpuBaseTransactionCost());
					chargeForStorage(sizeCalculator.sizeOf(request));

					InstanceMethodExecutor executor = new InstanceMethodExecutor(this, request.method, deserializedCaller, request.receiver, request.getActuals());
					executor.start();
					executor.join();

					if (executor.exception instanceof InvocationTargetException) {
						MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}

					if (executor.exception != null)
						throw executor.exception;

					if (executor.isViewMethod && !executor.onlyAffectedBalanceOf(deserializedCaller))
						throw new SideEffectsInViewMethodException((MethodSignature) executor.methodOrConstructor);

					if (executor.isVoidMethod) {
						MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}
					else {
						MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
							(serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new MethodCallTransactionSuccessfulResponse
							(serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}
				}
				catch (Throwable t) {
					// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
					BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
					return new MethodCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
				}
			}
		});
	}

	@Override
	public final MethodCallTransactionResponse runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference current) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, current);

			try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
				this.classLoader = classLoader;
				Object deserializedCaller = deserializer.deserialize(request.caller);
				checkIsExternallyOwned(deserializedCaller);
				
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction can be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(gasCostModel.cpuBaseTransactionCost());
					chargeForStorage(sizeCalculator.sizeOf(request));

					StaticMethodExecutor executor = new StaticMethodExecutor(this, request.method, deserializedCaller, request.getActuals());
					executor.start();
					executor.join();

					if (executor.exception instanceof InvocationTargetException) {
						MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}

					if (executor.exception != null)
						throw executor.exception;

					if (executor.isViewMethod && !executor.onlyAffectedBalanceOf(deserializedCaller))
						throw new SideEffectsInViewMethodException((MethodSignature) executor.methodOrConstructor);

					if (executor.isVoidMethod) {
						MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}
					else {
						MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
							(serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(sizeCalculator.sizeOf(response));
						increaseBalance(deserializedCaller);
						return new MethodCallTransactionSuccessfulResponse
							(serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}
				}
				catch (Throwable t) {
					// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
					BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
					return new MethodCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
				}
			}
		});
	}

	/**
	 * Yields the run-time class of the given object.
	 * 
	 * @param object the object
	 * @return the name of the class
	 * @throws DeserializationError if the class of the object cannot be found
	 */
	public final String getClassNameOf(StorageReference object) {
		try {
			TransactionResponse response = getResponseAt(object.transaction);
			if (response instanceof TransactionResponseWithUpdates) {
				Optional<ClassTag> classTag = ((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst();

				if (classTag.isPresent())
					return classTag.get().className;
			}
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}

		throw new DeserializationError("Did not find the class tag for " + object);
	}

	// BLOCKCHAIN-AGNOSTIC IMPLEMENTATION
	
	/**
	 * Yields the latest value for the given field, of lazy type, of the given storage reference.
	 * The field is not {@code final}.
	 * Conceptually, this method goes backwards from the tip of the blockchain, looking for the latest
	 * update of the given field.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	public final Object deserializeLastLazyUpdateFor(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(getLastLazyUpdateToNonFinalFieldOf(reference, field).getValue());
	}

	/**
	 * Yields the latest value for the given field, of lazy type, of the given storage reference.
	 * The field is {@code final}. Conceptually, this method looks for the value of the field
	 * in the transaction where the reference was created.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	public final Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(getLastLazyUpdateToFinalFieldOf(reference, field).getValue());
	}

	public StorageReference getStorageReferenceOf(Object object) {
		try {
			return (StorageReference) classLoader.storageReference.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the storage reference of a storage object of class " + object.getClass().getName());
		}
	}

	public boolean getInStorageOf(Object object) {
		try {
			return (boolean) classLoader.inStorage.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the inStorage tag of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Checks if the caller of a transaction has enough money at least for paying the promised gas and the addition of a
	 * failed transaction response to blockchain.
	 * 
	 * @param request the request
	 * @param deserializedCaller the caller
	 * @return the update to the balance that would follow if the failed transaction request is added to the blockchain
	 * @throws IllegalTransactionRequestException if the caller has not enough money to buy the promised gas and the addition
	 *                                            of a failed transaction response to blockchain
	 * @throws ClassNotFoundException if the balance of the account cannot be correctly modified
	 * @throws NoSuchFieldException if the balance of the account cannot be correctly modified
	 * @throws SecurityException if the balance of the account cannot be correctly modified
	 * @throws IllegalArgumentException if the balance of the account cannot be correctly modified
	 * @throws IllegalAccessException if the balance of the account cannot be correctly modified
	 */
	private UpdateOfBalance checkMinimalGas(NonInitialTransactionRequest request, Object deserializedCaller) throws IllegalTransactionRequestException, ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
		UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(getStorageReferenceOf(deserializedCaller), decreasedBalanceOfCaller);

		if (gas.compareTo(minimalGasForRunning(request, balanceUpdateInCaseOfFailure)) < 0)
			throw new IllegalTransactionRequestException("Not enough gas to start the transaction");

		return balanceUpdateInCaseOfFailure;
	}

	private BigInteger minimalGasForRunning(NonInitialTransactionRequest request, UpdateOfBalance balanceUpdateInCaseOfFailure) throws IllegalTransactionRequestException {
		// we create a response whose size over-approximates that of a response in case of failure of this request
		if (request instanceof ConstructorCallTransactionRequest)
			return BigInteger.valueOf(gasCostModel.cpuBaseTransactionCost())
				.add(sizeCalculator.sizeOf(request))
				.add(sizeCalculator.sizeOf(new ConstructorCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof InstanceMethodCallTransactionRequest)
			return BigInteger.valueOf(gasCostModel.cpuBaseTransactionCost())
				.add(sizeCalculator.sizeOf(request))
				.add(sizeCalculator.sizeOf(new MethodCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof StaticMethodCallTransactionRequest)
			return BigInteger.valueOf(gasCostModel.cpuBaseTransactionCost())
				.add(sizeCalculator.sizeOf(request))
				.add(sizeCalculator.sizeOf(new MethodCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof JarStoreTransactionRequest)
			return BigInteger.valueOf(gasCostModel.cpuBaseTransactionCost())
				.add(sizeCalculator.sizeOf(request))
				.add(sizeCalculator.sizeOf(new JarStoreTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else
			throw new IllegalTransactionRequestException("unexpected transaction request");
	}

	/**
	 * Calls the given callable. If if throws an exception, it wraps into into a {@link io.hotmoka.beans.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws TransactionException the wrapped exception
	 */
	protected static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
		try {
			return what.call();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * Sells the given amount of gas to the given externally owned account.
	 * 
	 * @param eoa the reference to the externally owned account
	 * @param gas the gas to sell
	 * @return the balance of the contract after paying the given amount of gas
	 * @throws IllegalTransactionRequestException if the externally owned account does not have funds
	 *                                            for buying the given amount of gas
	 * @throws ClassNotFoundException if the balance of the account cannot be correctly modified
	 * @throws NoSuchFieldException if the balance of the account cannot be correctly modified
	 * @throws SecurityException if the balance of the account cannot be correctly modified
	 * @throws IllegalArgumentException if the balance of the account cannot be correctly modified
	 * @throws IllegalAccessException if the balance of the account cannot be correctly modified
	 */
	private BigInteger decreaseBalance(Object eoa, BigInteger gas)
			throws IllegalTransactionRequestException, ClassNotFoundException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
	
		BigInteger delta = gasCostModel.toCoin(gas);
		Field balanceField = classLoader.getContract().getDeclaredField("balance");
		balanceField.setAccessible(true); // since the field is private
		BigInteger previousBalance = (BigInteger) balanceField.get(eoa);
		if (previousBalance.compareTo(delta) < 0)
			throw new IllegalTransactionRequestException("Caller has not enough funds to buy " + gas + " units of gas");

		BigInteger result = previousBalance.subtract(delta);
		balanceField.set(eoa, result);
		return result;
	}

	/**
	 * Buys back the given amount of gas from the given externally owned account.
	 * 
	 * @param eoa the reference to the externally owned account
	 * @param gas the gas to buy back
	 * @return the balance of the contract after buying back the given amount of gas
	 * @throws NoSuchFieldException if the balance of the account cannot be correctly modified
	 * @throws SecurityException if the balance of the account cannot be correctly modified
	 * @throws IllegalArgumentException if the balance of the account cannot be correctly modified
	 * @throws IllegalAccessException if the balance of the account cannot be correctly modified
	 */
	private BigInteger increaseBalance(Object eoa) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		BigInteger delta = gasCostModel.toCoin(gas);
		Field balanceField = classLoader.getContract().getDeclaredField("balance");
		balanceField.setAccessible(true); // since the field is private
		BigInteger previousBalance = (BigInteger) balanceField.get(eoa);
		BigInteger result = previousBalance.add(delta);
		balanceField.set(eoa, result);
		return result;
	}

	/**
	 * Yields the balance of the given externally owned account.
	 * 
	 * @param eoa the reference to the externally owned account
	 * @return the balance of {@code eoa}
	 * @throws NoSuchFieldException if the balance of the account cannot be correctly modified
	 * @throws SecurityException if the balance of the account cannot be correctly modified
	 * @throws IllegalArgumentException if the balance of the account cannot be correctly modified
	 * @throws IllegalAccessException if the balance of the account cannot be correctly modified
	 */
	private BigInteger getBalanceOf(Object eoa) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field balanceField = classLoader.getContract().getDeclaredField("balance");
		balanceField.setAccessible(true); // since the field is private
		return (BigInteger) balanceField.get(eoa);
	}

	/**
	 * Checks if the given object is an externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not an externally owned account
	 */
	private void checkIsExternallyOwned(Object object) throws ClassNotFoundException, IllegalTransactionRequestException {
		Class<? extends Object> clazz = object.getClass();
		if (!classLoader.getExternallyOwnedAccount().isAssignableFrom(clazz)
				&& !classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new IllegalTransactionRequestException("Only an externally owned account can start a transaction");
	}

	/**
	 * Collects all updates reachable from the actuals or from the caller, receiver or result of a method call.
	 * 
	 * @param actuals the actuals; only {@code Storage} are relevant; this might be {@code null}
	 * @param caller the caller of an {@code @@Entry} method; this might be {@code null}
	 * @param receiver the receiver of the call; this might be {@code null}
	 * @param result the result; relevant only if {@code Storage}
	 * @return the ordered updates
	 */
	public SortedSet<Update> collectUpdates(Object[] actuals, Object caller, Object receiver, Object result) {
		List<Object> potentiallyAffectedObjects = new ArrayList<>();
		if (caller != null)
			potentiallyAffectedObjects.add(caller);
		if (receiver != null)
			potentiallyAffectedObjects.add(receiver);
		Class<?> storage = classLoader.getStorage();
		if (result != null && storage.isAssignableFrom(result.getClass()))
			potentiallyAffectedObjects.add(result);

		if (actuals != null)
			for (Object actual: actuals)
				if (actual != null && storage.isAssignableFrom(actual.getClass()))
					potentiallyAffectedObjects.add(actual);

		// events are accessible from outside, hence they count as side-effects
		events.forEach(potentiallyAffectedObjects::add);

		return new UpdatesExtractor(this, potentiallyAffectedObjects.stream()).getUpdates();
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message used for the {@link io.hotmoka.beans.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	protected static TransactionException wrapAsTransactionException(Throwable t, String message) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(message, t);
	}
}