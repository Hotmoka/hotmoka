package takamaka.blockchain;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

import takamaka.blockchain.request.AbstractJarStoreTransactionRequest;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.request.TransactionRequest;
import takamaka.blockchain.response.ConstructorCallTransactionExceptionResponse;
import takamaka.blockchain.response.ConstructorCallTransactionFailedResponse;
import takamaka.blockchain.response.ConstructorCallTransactionResponse;
import takamaka.blockchain.response.ConstructorCallTransactionSuccessfulResponse;
import takamaka.blockchain.response.GameteCreationTransactionResponse;
import takamaka.blockchain.response.JarStoreInitialTransactionResponse;
import takamaka.blockchain.response.JarStoreTransactionFailedResponse;
import takamaka.blockchain.response.JarStoreTransactionResponse;
import takamaka.blockchain.response.JarStoreTransactionSuccessfulResponse;
import takamaka.blockchain.response.MethodCallTransactionExceptionResponse;
import takamaka.blockchain.response.MethodCallTransactionFailedResponse;
import takamaka.blockchain.response.MethodCallTransactionResponse;
import takamaka.blockchain.response.MethodCallTransactionSuccessfulResponse;
import takamaka.blockchain.response.TransactionResponse;
import takamaka.blockchain.response.TransactionResponseWithInstrumentedJar;
import takamaka.blockchain.response.TransactionResponseWithUpdates;
import takamaka.blockchain.response.VoidMethodCallTransactionSuccessfulResponse;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageReferenceAlreadyInBlockchain;
import takamaka.blockchain.values.StorageValue;
import takamaka.instrumentation.Dummy;
import takamaka.instrumentation.JarInstrumentation;
import takamaka.instrumentation.TakamakaClassLoader;
import takamaka.instrumentation.VerificationException;
import takamaka.lang.Event;
import takamaka.lang.InsufficientFundsError;
import takamaka.lang.NonWhiteListedCallException;
import takamaka.lang.OutOfGasError;
import takamaka.lang.Storage;
import takamaka.lang.Takamaka;
import takamaka.lang.ThrowsExceptions;
import takamaka.lang.View;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class AbstractBlockchain implements Blockchain {

	/**
	 * The events accumulated during the current transaction. This is reset at each transaction.
	 */
	private final List<Storage> events = new ArrayList<>();

	/**
	 * A map from each storage reference to its deserialized object. This is needed in order to guarantee that
	 * repeated deserialization of the same storage reference yields the same object and can also
	 * work as an efficiency measure. This is reset at each transaction since each transaction uses
	 * a distinct class loader and each storage object keeps a reference to its class loader, as
	 * always in Java.
	 */
	private final Map<StorageReferenceAlreadyInBlockchain, Storage> cache = new HashMap<>();

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
	private BlockchainClassLoader classLoader;

	/**
	 * The transaction reference after which the current transaction is being executed.
	 * This is {@code null} for the first transaction.
	 */
	protected TransactionReference previous;

	// ABSTRACT TEMPLATE METHODS
	// Any implementation of a blockchain must implement the following and leave the rest unchanged
	
	/**
	 * Yields the reference to the transaction on top of the blockchain.
	 * If there are more chains, this refers to the transaction in the longest chain.
	 * 
	 * @return the reference to the topmost transaction, if any. Yields {@code null} if
	 *         the blockchain is empty
	 */
	protected abstract TransactionReference getTopmostTransactionReference();

	/**
	 * Yields a transaction reference whose {@code toString()} is the given string.
	 * 
	 * @param toString the result of {@code toString()} on the desired transaction reference
	 * @return the transaction reference
	 */
	protected abstract TransactionReference getTransactionReferenceFor(String toString);

	/**
	 * Expands the blockchain with a new topmost transaction. If there are more chains, this
	 * method expands the longest chain.
	 * 
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @return the reference to the transaction that has been added
	 * @throws Exception if the expansion cannot be completed
	 */
	protected abstract TransactionReference expandBlockchainWith(TransactionRequest request, TransactionResponse response) throws Exception;

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
	 * Initializes the state at the beginning of the execution of a new transaction
	 * 
	 * @param gas the amount of gas available for the transaction
	 * @param previous the transaction reference after which the transaction is being executed.
	 *                 If this is the first transaction, then {@code previous} will be {@code null}
	 * @throws Exception if the transaction could not be initialized
	 */
	protected void initTransaction(BigInteger gas, TransactionReference previous) throws Exception {
		Takamaka.init(AbstractBlockchain.this); // this blockchain will be used during the execution of the code
		events.clear();
		cache.clear();
		ClassType.clearCache();
		FieldSignature.clearCache();
		this.gas = gas;
		this.gasConsumedForCPU = BigInteger.ZERO;
		this.gasConsumedForRAM = BigInteger.ZERO;
		this.gasConsumedForStorage = BigInteger.ZERO;
		oldGas.clear();
		this.previous = previous;
	}

	/**
	 * A comparator that puts updates in the order required for the parameter
	 * of the deserialization constructor of storage objects: fields of superclasses first;
	 * then the fields for the same class, ordered by name and then by the
	 * {@code toString()} of their type.
	 */
	private final Comparator<Update> updateComparator = new Comparator<Update>() {

		@Override
		public int compare(Update update1, Update update2) {
			if (update1 instanceof UpdateOfField && update2 instanceof UpdateOfField) {
				FieldSignature field1 = ((UpdateOfField) update1).getField();
				FieldSignature field2 = ((UpdateOfField) update2).getField();

				try {
					String className1 = field1.definingClass.name;
					String className2 = field2.definingClass.name;

					if (className1.equals(className2)) {
						int diff = field1.name.compareTo(field2.name);
						if (diff != 0)
							return diff;
						else
							return field1.type.toString().compareTo(field2.type.toString());
					}

					Class<?> clazz1 = classLoader.loadClass(className1);
					Class<?> clazz2 = classLoader.loadClass(className2);
					if (clazz1.isAssignableFrom(clazz2)) // clazz1 superclass of clazz2
						return -1;
					else if (clazz2.isAssignableFrom(clazz1)) // clazz2 superclass of clazz1
						return 1;
					else
						throw new IllegalStateException("Updates are not on the same supeclass chain");
				}
				catch (ClassNotFoundException e) {
					throw new DeserializationError(e);
				}
			}
			else
				return update1.compareTo(update2);
		}
	};
	
	/**
	 * Yields the UTC time when the currently executing transaction is being run.
	 * This might be for instance the time of creation of the block where the transaction
	 * occurs, but the detail is left to the implementation. In any case, this
	 * time must be the same for a given transaction, if it gets executed more times.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	public abstract long getNow();

	// BLOCKCHAIN-AGNOSTIC IMPLEMENTATION

	/**
	 * Yields the request that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the request
	 * @throws Exception if the request could not be found
	 */
	private TransactionRequest getRequestAtAndCharge(TransactionReference transaction) throws Exception {
		chargeForCPU(GasCosts.cpuCostForGettingRequestAt(transaction));
		return getRequestAt(transaction);
	}

	/**
	 * Yields the response that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	private TransactionResponse getResponseAtAndCharge(TransactionReference transaction) throws Exception {
		chargeForCPU(GasCosts.cpuCostForGettingResponseAt(transaction));
		return getResponseAt(transaction);
	}

	/**
	 * Yields the transaction reference that installed the jar from which
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
			throw new IllegalArgumentException("Gas cannot inrease");

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
	public final void event(Event event) {
		if (event == null)
			throw new IllegalArgumentException("Events cannot be null");

		events.add(event);
	}

	@Override
	public final JarStoreInitialTransactionResponse runJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference previous) throws TransactionException {
		return wrapInCaseOfException(() -> {
			// we do not count gas for this transaction
			initTransaction(BigInteger.valueOf(-1L), previous);

			if (previous == null) {
				// this is the first transaction of this blockchain
				if (request.getNumberOfDependencies() > 0)
					throw new IllegalTransactionRequestException("A jar file can only depend on jars installed by older transactions");
			}
			else {
				TransactionRequest previousRequest = getRequestAt(previous);
				if (!(previousRequest instanceof InitialTransactionRequest))
					throw new IllegalTransactionRequestException("This blockchain is already initialized");
				else if (request.getDependencies().map(dependency -> dependency.transaction).anyMatch(previous::isOlderThan))
					throw new IllegalTransactionRequestException("A jar file can only depend on jars installed by older transactions");
			}

			// we transform the array of bytes into a real jar file
			Path original = Files.createTempFile("original", ".jar");
			Files.write(original, request.getJar());

			// we create a temporary file to hold the instrumented jar
			Path instrumented = Files.createTempFile("instrumented", ".jar");
			// we keep the BCEL repository to a minimum
			String appendedClassPath = original.toString();
			Repository.setRepository(SyntheticRepository.getInstance(new ClassPath(appendedClassPath)));
			try (BlockchainClassLoader jarClassLoader = new BlockchainClassLoader(original, request.getDependencies(), this)) {
				JarInstrumentation instrumentation = new JarInstrumentation(original, instrumented, jarClassLoader, true);
				if (instrumentation.hasErrors())
					throw new VerificationException(instrumentation.getFirstError().get());
			}

			byte[] instrumentedBytes = Files.readAllBytes(instrumented);
			Files.delete(instrumented);
		
			return new JarStoreInitialTransactionResponse(instrumentedBytes);
		});
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			return expandBlockchainWith(request, runJarStoreInitialTransaction(request, getTopmostTransactionReference()));
		});
	}

	@Override
	public final GameteCreationTransactionResponse runGameteCreationTransaction(GameteCreationTransactionRequest request, TransactionReference previous) throws TransactionException {
		return wrapInCaseOfException(() -> {
			// we do not count gas for this creation
			initTransaction(BigInteger.valueOf(-1L), previous);

			TransactionRequest previousRequest = getRequestAt(previous);
			if (!(previousRequest instanceof InitialTransactionRequest))
				throw new IllegalTransactionRequestException("This blockchain is already initialized");

			if (request.initialAmount.signum() < 0)
				throw new IllegalTransactionRequestException("The gamete must be initialized with a non-negative amount of coins");

			try (BlockchainClassLoader classLoader = this.classLoader = new BlockchainClassLoader(request.classpath, this)) {
				// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
				Storage gamete = (Storage) classLoader.externallyOwnedAccount.newInstance();
				// we set the balance field of the gamete
				Field balanceField = classLoader.contractClass.getDeclaredField("balance");
				balanceField.setAccessible(true); // since the field is private
				balanceField.set(gamete, request.initialAmount);

				return new GameteCreationTransactionResponse(collectUpdates(null, null, null, gamete).stream(), gamete.storageReference);
			}
		});
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			return runGameteCreationTransaction(request, getTopmostTransactionReference()).gamete
				.contextualizeAt(expandBlockchainWith(request, runGameteCreationTransaction(request, getTopmostTransactionReference())));
		});
	}

	@Override
	public final JarStoreTransactionResponse runJarStoreTransaction(JarStoreTransactionRequest request, TransactionReference previous) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, previous);

			try (BlockchainClassLoader classLoader = this.classLoader = new BlockchainClassLoader(request.classpath, this)) {
				Storage deserializedCaller = request.caller.deserialize(this);
				checkIsExternallyOwned(deserializedCaller);

				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
				UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(deserializedCaller.storageReference, decreasedBalanceOfCaller);
				checkMinimalGas(request, balanceUpdateInCaseOfFailure);

				if (request.getDependencies().map(dependency -> dependency.transaction).anyMatch(previous::isOlderThan))
					throw new IllegalTransactionRequestException("A jar file can only depend on jars installed by older transactions");

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction will be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(GasCosts.BASE_CPU_TRANSACTION_COST);
					chargeForStorage(request.size());

					byte[] jar = request.getJar();
					chargeForCPU(GasCosts.cpuCostForInstalling(jar));
					chargeForRAM(GasCosts.ramCostForInstalling(jar));

					// we transform the array of bytes into a real jar file
					Path original = Files.createTempFile("original", ".jar");
					Files.write(original, jar);

					// we create a temporary file to hold the instrumented jar
					Path instrumented = Files.createTempFile("instrumented", ".jar");

					// we set the BCEL repository so that it matches the class path made up of the jar to
					// instrument and its dependencies. This is important since class instrumentation will use
					// the repository to infer least common supertypes during type inference, hence the
					// whole hierarchy of classes must be available to BCEL through its repository
					String appendedClassPath = Stream.of(classLoader.getURLs()).map(URL::getFile).collect(Collectors.joining(":", original.toString() + ":", ""));
					Repository.setRepository(SyntheticRepository.getInstance(new ClassPath(appendedClassPath)));
					try (BlockchainClassLoader jarClassLoader = new BlockchainClassLoader(original, request.getDependencies(), this)) {
						JarInstrumentation instrumentation = new JarInstrumentation(original, instrumented, jarClassLoader, false);
						if (instrumentation.hasErrors())
							throw new VerificationException(instrumentation.getFirstError().get());
					}

					byte[] instrumentedBytes = Files.readAllBytes(instrumented);
					Files.delete(instrumented);

					BigInteger balanceOfCaller = increaseBalance(deserializedCaller, BigInteger.ZERO);
					UpdateOfBalance balanceUpdate = new UpdateOfBalance(deserializedCaller.storageReference, balanceOfCaller);
					JarStoreTransactionResponse response = new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(response.size());
					balanceOfCaller = increaseBalance(deserializedCaller, remainingGas());
					balanceUpdate = new UpdateOfBalance(deserializedCaller.storageReference, balanceOfCaller);
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

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			JarStoreTransactionResponse response = runJarStoreTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof JarStoreTransactionFailedResponse)
				throw ((JarStoreTransactionFailedResponse) response).cause;
			else
				return transaction;
		});
	}

	@Override
	public final ConstructorCallTransactionResponse runConstructorCallTransaction(ConstructorCallTransactionRequest request, TransactionReference previous) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, previous);

			try (BlockchainClassLoader classLoader = this.classLoader = new BlockchainClassLoader(request.classpath, this)) {
				Storage deserializedCaller = request.caller.deserialize(this);
				checkIsExternallyOwned(deserializedCaller);
				
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
				UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(deserializedCaller.storageReference, decreasedBalanceOfCaller);
				checkMinimalGas(request, balanceUpdateInCaseOfFailure);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction can be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(GasCosts.BASE_CPU_TRANSACTION_COST);
					chargeForStorage(request.size());

					CodeExecutor executor = new ConstructorExecutor(request.constructor, deserializedCaller, request.getActuals());
					executor.start();
					executor.join();

					if (executor.exception instanceof InvocationTargetException) {
						ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}

					if (executor.exception != null)
						throw executor.exception;

					ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
						((StorageReference) StorageValue.serialize(executor.result), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(response.size());
					increaseBalance(deserializedCaller, remainingGas());
					return new ConstructorCallTransactionSuccessfulResponse
						((StorageReference) StorageValue.serialize(executor.result), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			ConstructorCallTransactionResponse response = runConstructorCallTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof ConstructorCallTransactionFailedResponse)
				throw ((ConstructorCallTransactionFailedResponse) response).cause;
			else if (response instanceof ConstructorCallTransactionExceptionResponse)
				throw new CodeExecutionException("Constructor threw exception", ((ConstructorCallTransactionExceptionResponse) response).exception);
			else
				return ((ConstructorCallTransactionSuccessfulResponse) response).newObject.contextualizeAt(transaction);
		});
	}

	@Override
	public final MethodCallTransactionResponse runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference previous) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, previous);

			try (BlockchainClassLoader classLoader = this.classLoader = new BlockchainClassLoader(request.classpath, this)) {
				Storage deserializedCaller = request.caller.deserialize(this);
				checkIsExternallyOwned(deserializedCaller);
				
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
				UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(deserializedCaller.storageReference, decreasedBalanceOfCaller);
				checkMinimalGas(request, balanceUpdateInCaseOfFailure);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction can be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(GasCosts.BASE_CPU_TRANSACTION_COST);
					chargeForStorage(request.size());

					InstanceMethodExecutor executor = new InstanceMethodExecutor(request.method, deserializedCaller, request.receiver, request.getActuals());
					executor.start();
					executor.join();

					if (executor.exception instanceof InvocationTargetException) {
						MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}

					if (executor.exception != null)
						throw executor.exception;

					if (executor.isViewMethod && !executor.onlyAffectedBalanceOf(deserializedCaller))
						throw new SideEffectsInViewMethodException((MethodSignature) executor.methodOrConstructor);

					if (executor.isVoidMethod) {
						MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}
					else {
						MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
							(StorageValue.serialize(executor.result), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new MethodCallTransactionSuccessfulResponse
							(StorageValue.serialize(executor.result), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			MethodCallTransactionResponse response = runInstanceMethodCallTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof MethodCallTransactionFailedResponse)
				throw ((MethodCallTransactionFailedResponse) response).cause;
			else if (response instanceof MethodCallTransactionExceptionResponse)
				throw new CodeExecutionException("Method threw exception", ((MethodCallTransactionExceptionResponse) response).exception);
			else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
				return null;
			else {
				StorageValue result = ((MethodCallTransactionSuccessfulResponse) response).result;
				return result instanceof StorageReference ?
					((StorageReference) result).contextualizeAt(transaction) : result;
			}
		});
	}

	@Override
	public final MethodCallTransactionResponse runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference previous) throws TransactionException {
		return wrapInCaseOfException(() -> {
			initTransaction(request.gas, previous);

			try (BlockchainClassLoader classLoader = this.classLoader = new BlockchainClassLoader(request.classpath, this)) {
				Storage deserializedCaller = request.caller.deserialize(this);
				checkIsExternallyOwned(deserializedCaller);
				
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
				UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(deserializedCaller.storageReference, decreasedBalanceOfCaller);
				checkMinimalGas(request, balanceUpdateInCaseOfFailure);

				// before this line, an exception will abort the transaction and leave the blockchain unchanged;
				// after this line, the transaction can be added to the blockchain, possibly as a failed one

				try {
					chargeForCPU(GasCosts.BASE_CPU_TRANSACTION_COST);
					chargeForStorage(request.size());

					StaticMethodExecutor executor = new StaticMethodExecutor(request.method, deserializedCaller, request.getActuals());
					executor.start();
					executor.join();

					if (executor.exception instanceof InvocationTargetException) {
						MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}

					if (executor.exception != null)
						throw executor.exception;

					if (executor.isViewMethod && !executor.onlyAffectedBalanceOf(deserializedCaller))
						throw new SideEffectsInViewMethodException((MethodSignature) executor.methodOrConstructor);

					if (executor.isVoidMethod) {
						MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					}
					else {
						MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
								(StorageValue.serialize(executor.result), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						chargeForStorage(response.size());
						increaseBalance(deserializedCaller, remainingGas());
						return new MethodCallTransactionSuccessfulResponse
							(StorageValue.serialize(executor.result), executor.updates(), events.stream().map(event -> event.storageReference), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			MethodCallTransactionResponse response = runStaticMethodCallTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof MethodCallTransactionFailedResponse)
				throw ((MethodCallTransactionFailedResponse) response).cause;
			else if (response instanceof MethodCallTransactionExceptionResponse)
				throw new CodeExecutionException("Method threw exception", ((MethodCallTransactionExceptionResponse) response).exception);
			else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
				return null;
			else {
				StorageValue result = ((MethodCallTransactionSuccessfulResponse) response).result;
				return result instanceof StorageReference ?
					((StorageReference) result).contextualizeAt(transaction) : result;
			}
		});
	}

	/**
	 * Deserializes the given storage reference from the blockchain. It first checks in a cache if the
	 * same reference has been already deserialized during the current transaction and in such a case yeilds
	 * the same object. Otherwise, it calls method {@link takamaka.blockchain.AbstractBlockchain#deserializeAnew(StorageReferenceAlreadyInBlockchain)}
	 * and yields the resulting object.
	 * 
	 * @param object the storage reference to deserialize
	 * @return the resulting storage object
	 */
	public final Storage deserialize(StorageReferenceAlreadyInBlockchain object) {
		return cache.computeIfAbsent(object, this::deserializeAnew);
	}

	/**
	 * Yields the run-time class of the given object.
	 * 
	 * @param object the object
	 * @return the name of the class
	 * @throws DeserializationError if the class of the object cannot be found
	 */
	public final String getClassNameOf(StorageReferenceAlreadyInBlockchain object) {
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
	public final Object deserializeLastLazyUpdateFor(StorageReferenceAlreadyInBlockchain reference, FieldSignature field) throws Exception {
		return getLastLazyUpdateFor(reference, field).getValue().deserialize(this);
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
	public final Object deserializeLastLazyUpdateForFinal(StorageReferenceAlreadyInBlockchain reference, FieldSignature field) throws Exception {
		return getLastLazyUpdateForFinal(reference, field).getValue().deserialize(this);
	}

	/**
	 * Yields the class with the given name for the current transaction.
	 * 
	 * @param name the name of the class
	 * @return the class, if any
	 * @throws ClassNotFoundException if the class cannot be found for the current transaction
	 */
	public final Class<?> loadClass(String name) throws ClassNotFoundException {
		return classLoader.loadClass(name);
	}

	/**
	 * Puts in the given set all the latest updates for the fields of eager type of the
	 * object at the given storage reference.
	 * 
	 * @param object the storage reference
	 * @param updates the set where the latest updates must be added
	 * @param eagerFields the number of eager fields whose latest update needs to be found
	 * @throws Exception if the operation fails
	 */
	private void collectEagerUpdatesFor(StorageReferenceAlreadyInBlockchain object, Set<Update> updates, int eagerFields) throws Exception {
		// goes back from the transaction that precedes that being executed;
		// there is no reason to look before the transaction that created the object;
		// moreover, there is no reason to look beyond the total number of fields
		// whose update was expected to be found
		for (TransactionReference cursor = previous; updates.size() < eagerFields && !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious())
			// adds the eager updates from the cursor, if any and if they are the latest
			addEagerUpdatesFor(object, cursor, updates);
	}

	/**
	 * Adds, to the given set, the updates of eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the transaction
	 * @param updates the set where they must be added
	 * @throws IOException if there is an error while accessing the disk
	 */
	private void addEagerUpdatesFor(StorageReferenceAlreadyInBlockchain object, TransactionReference transaction, Set<Update> updates) throws Exception {
		TransactionResponse response = getResponseAtAndCharge(transaction);
		if (response instanceof TransactionResponseWithUpdates)
			((TransactionResponseWithUpdates) response).getUpdates()
				.map(update -> update.contextualizeAt(transaction))
				.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && update.isEager() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	/**
	 * Determines if the given set of updates contains an update for the
	 * same object and field as the given update.
	 * 
	 * @param update the given update
	 * @param updates the set
	 * @return true if and only if that condition holds
	 */
	private static boolean isAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().anyMatch(update::isForSamePropertyAs);
	}

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
	private UpdateOfField getLastLazyUpdateFor(StorageReferenceAlreadyInBlockchain object, FieldSignature field) throws Exception {
		// goes back from the previous transaction;
		// there is no reason to look before the transaction that created the object
		for (TransactionReference cursor = previous; !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious()) {
			UpdateOfField update = getLastUpdateFor(object, field, cursor);
			if (update != null)
				return update;
		}
	
		throw new DeserializationError("Did not find the last update for " + field + " of " + object);
	}

	/**
	 * Yields the update to the given field of the object at the given reference,
	 * generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param transaction the block where the update is being looked for
	 * @return the update, if any. If the field of {@code reference} was not modified during
	 *         the {@code transaction}, this method returns {@code null}
	 */
	private UpdateOfField getLastUpdateFor(StorageReferenceAlreadyInBlockchain object, FieldSignature field, TransactionReference transaction) throws Exception {
		TransactionResponse response = getResponseAtAndCharge(transaction);
		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> update.contextualizeAt(transaction))
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findAny()
				.orElse(null);
	
		return null;
	}

	/**
	 * Yields the most recent update for the given {@code final} field,
	 * of lazy type, of the object at given storage reference.
	 * Conceptually, this amounts to accessing the storage reference when the object was
	 * created and reading the value of the field there.
	 * 
	 * @param object the storage reference
	 * @param field the field whose update is being looked for
	 * @return the update, if any
	 * @throws Exception if the update could not be found
	 */
	private UpdateOfField getLastLazyUpdateForFinal(StorageReferenceAlreadyInBlockchain object, FieldSignature field) throws Exception {
		// goes directly to the transaction that created the object
		UpdateOfField update = getLastUpdateFor(object, field, object.transaction);
		if (update != null)
			return update;
	
		throw new DeserializationError("Did not find the last update for " + field + " of " + object);
	}

	private static void checkMinimalGas(TransactionRequest request, UpdateOfBalance balanceUpdateInCaseOfFailure) throws IllegalTransactionRequestException {
		if (!request.hasMinimalGas(balanceUpdateInCaseOfFailure))
			throw new IllegalTransactionRequestException("Not enough gas to start the transaction");
	}

	/**
	 * Calls the given callable. If if throws an exception, it wraps into into a {@link takamaka.blockchain.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
		try {
			return what.call();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * Calls the given callable. If if throws a {@link takamaka.blockchain.CodeExecutionException}, if throws it back
	 * unchanged. Otherwise, it wraps the exception into into a {@link takamaka.blockchain.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws CodeExecutionException the unwrapped exception
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapWithCodeInCaseOfException(Callable<T> what) throws TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (CodeExecutionException e) {
			throw e;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * A class loader used to access the definition of the classes
	 * of Takamaka methods or constructors executed during a transaction.
	 */
	private static class BlockchainClassLoader extends TakamakaClassLoader {

		/**
		 * The temporary files that hold the class path for a transaction.
		 */
		private final List<Path> classpathElements = new ArrayList<>();

		/**
		 * Builds the class loader for the given class path and its dependencies.
		 * 
		 * @param classpath the class path
		 * @throws Exception if an error occurs
		 */
		private BlockchainClassLoader(Classpath classpath, AbstractBlockchain blockchain) throws Exception {
			super(collectURLs(Stream.of(classpath), blockchain, null));

			for (URL url: getURLs())
				classpathElements.add(Paths.get(url.toURI()));
		}

		/**
		 * Builds the class loader for the given jar and its dependencies.
		 * 
		 * @param jar the jar
		 * @param dependencies the dependencies
		 * @throws Exception if an error occurs
		 */
		private BlockchainClassLoader(Path jar, Stream<Classpath> dependencies, AbstractBlockchain blockchain) throws Exception {
			super(collectURLs(dependencies, blockchain, jar.toUri().toURL()));

			for (URL url: getURLs())
				classpathElements.add(Paths.get(url.toURI()));
		}

		private static URL[] collectURLs(Stream<Classpath> classpaths, AbstractBlockchain blockchain, URL start) throws Exception {
			List<URL> urls = new ArrayList<>();
			if (start != null)
				urls.add(start);

			for (Classpath classpath: classpaths.toArray(Classpath[]::new))
				urls = addURLs(classpath, blockchain, urls);

			return urls.toArray(new URL[urls.size()]);
		}

		private static List<URL> addURLs(Classpath classpath, AbstractBlockchain blockchain, List<URL> bag) throws Exception {
			// if the class path is recursive, we consider its dependencies as well, recursively
			if (classpath.recursive) {
				TransactionRequest request = blockchain.getRequestAtAndCharge(classpath.transaction);
				if (!(request instanceof AbstractJarStoreTransactionRequest))
					throw new IllegalTransactionRequestException("classpath does not refer to a jar store transaction");

				Stream<Classpath> dependencies = ((AbstractJarStoreTransactionRequest) request).getDependencies();
				for (Classpath dependency: dependencies.toArray(Classpath[]::new))
					addURLs(dependency, blockchain, bag);
			}

			TransactionResponse response = blockchain.getResponseAtAndCharge(classpath.transaction);
			if (!(response instanceof TransactionResponseWithInstrumentedJar))
				throw new IllegalTransactionRequestException("classpath does not refer to a successful jar store transaction");

			byte[] instrumentedJarBytes = ((TransactionResponseWithInstrumentedJar) response).getInstrumentedJar();
			blockchain.chargeForCPU(GasCosts.cpuCostForLoading(instrumentedJarBytes));
			blockchain.chargeForRAM(GasCosts.ramCostForLoading(instrumentedJarBytes));

			try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(instrumentedJarBytes))) {
				Path classpathElement = Files.createTempFile(null, "@" + classpath.transaction + ".jar");
				Files.copy(is, classpathElement, StandardCopyOption.REPLACE_EXISTING);

				// we add, for class loading, the jar containing the instrumented code
				bag.add(classpathElement.toFile().toURI().toURL());
			}

			return bag;
		}

		@Override
		public void close() throws IOException {
			// we delete all paths elements that were used to build this class loader
			for (Path classpathElement: classpathElements)
				Files.delete(classpathElement);

			super.close();
		}
	}

	/**
	 * Deserializes the given storage reference from the blockchain.
	 * 
	 * @param reference the storage reference to deserialize
	 * @return the resulting storage object
	 */
	private Storage deserializeAnew(StorageReferenceAlreadyInBlockchain reference) {
		try {
			SortedSet<Update> updates;
			TransactionReference transaction = reference.transaction;

			TransactionResponse response = getResponseAtAndCharge(transaction);
			if (response instanceof TransactionResponseWithUpdates) {
				updates = ((TransactionResponseWithUpdates) response).getUpdates()
					.map(update -> update.contextualizeAt(transaction))
					.filter(update -> update.object.equals(reference) && update.isEager())
					.collect(Collectors.toCollection(() -> new TreeSet<>(updateComparator)));
			}
			else
				throw new DeserializationError("Storage reference " + reference + " does not contain updates");

			Optional<ClassTag> classTag = updates.stream()
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findAny();
	
			if (!classTag.isPresent())
				throw new DeserializationError("No class tag found for " + reference);

			// we drop the class tag
			updates.remove(classTag.get());

			// we drop updates to non-final fields
			Set<Field> eagerFields = collectEagerFieldsOf(classTag.get().className);
			Iterator<Update> it = updates.iterator();
			while (it.hasNext())
				if (!updatesFinalField(it.next(), eagerFields))
					it.remove();

			// the updates set contains the updates to eager final fields now:
			// we must still collect the latest updates to the non-final fields
			collectEagerUpdatesFor(reference, updates, eagerFields.size());

			String className = classTag.get().className;
			List<Class<?>> formals = new ArrayList<>();
			List<Object> actuals = new ArrayList<>();
			// the constructor for deserialization has a first parameter
			// that receives the storage reference of the object
			formals.add(StorageReferenceAlreadyInBlockchain.class);
			actuals.add(reference);
	
			for (Update update: updates) {
				UpdateOfField updateOF = (UpdateOfField) update;
				formals.add(updateOF.getField().type.toClass(this));
				actuals.add(updateOF.getValue().deserialize(this));
			}
	
			Class<?> clazz = classLoader.loadClass(className);
			TransactionReference actual = transactionThatInstalledJarFor(clazz);
			TransactionReference expected = classTag.get().jar;
			if (!actual.equals(expected))
				throw new DeserializationError("Class " + className + " was instantiated from jar at " + expected + " not from jar at " + actual);

			Constructor<?> constructor = clazz.getConstructor(formals.toArray(new Class<?>[formals.size()]));

			// the instrumented constructor is public, but the class might well be non-public;
			// hence we must force accessibility
			constructor.setAccessible(true);

			return (Storage) constructor.newInstance(actuals.toArray(new Object[actuals.size()]));
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}
	}

	/**
	 * Determines if the given update affects a {@code final} eager field contained in the given set.
	 * 
	 * @param update the update
	 * @param eagerFields the set of all possible eager fields
	 * @return true if and only if that condition holds
	 */
	private boolean updatesFinalField(Update update, Set<Field> eagerFields) throws ClassNotFoundException {
		if (update instanceof AbstractUpdateOfField) {
			FieldSignature sig = ((AbstractUpdateOfField) update).field;
			Class<?> type = sig.type.toClass(this);
			String name = sig.name;
			return eagerFields.stream()
				.anyMatch(field -> Modifier.isFinal(field.getModifiers()) && field.getType() == type && field.getName().equals(name));
		}

		return false;
	}

	/**
	 * Collects all eager fields of the given storage class, including those of its superclasses,
	 * up to and excluding {@link takamaka.lang.Storage}.
	 * 
	 * @param className the name of the storage class
	 * @return the eager fields
	 */
	private Set<Field> collectEagerFieldsOf(String className) throws ClassNotFoundException {
		Set<Field> bag = new HashSet<>();

		// fields added by instrumentation by Takamaka itself are not considered, since they are transient
		for (Class<?> clazz = loadClass(className); clazz != Storage.class; clazz = clazz.getSuperclass())
			Stream.of(clazz.getDeclaredFields())
			.filter(field -> !Modifier.isTransient(field.getModifiers())
					&& !Modifier.isStatic(field.getModifiers())
					&& classLoader.isEagerlyLoaded(field.getType()))
			.forEach(bag::add);

		return bag;
	}

	/**
	 * Sells the given amount of gas to the given externally owned account.
	 * 
	 * @param eoa the reference to the externally owned account
	 * @param gas the gas to sell
	 * @return the balance of the contract after paying the given amount of gas
	 * @throws IllegalTransactionRequestException if the externally owned account does not have funds
	 *                                            for buying the given amount of gas
	 * @throws InsufficientFundsError if the account has not enough money to pay for the gas
	 * @throws ClassNotFoundException if the balance of the account cannot be correctly modified
	 * @throws NoSuchFieldException if the balance of the account cannot be correctly modified
	 * @throws SecurityException if the balance of the account cannot be correctly modified
	 * @throws IllegalArgumentException if the balance of the account cannot be correctly modified
	 * @throws IllegalAccessException if the balance of the account cannot be correctly modified
	 */
	private BigInteger decreaseBalance(Storage eoa, BigInteger gas)
			throws IllegalTransactionRequestException, ClassNotFoundException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
	
		BigInteger delta = GasCosts.toCoin(gas);
		Field balanceField = classLoader.contractClass.getDeclaredField("balance");
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
	 * @throws ClassNotFoundException if the balance of the account cannot be correctly modified
	 * @throws NoSuchFieldException if the balance of the account cannot be correctly modified
	 * @throws SecurityException if the balance of the account cannot be correctly modified
	 * @throws IllegalArgumentException if the balance of the account cannot be correctly modified
	 * @throws IllegalAccessException if the balance of the account cannot be correctly modified
	 */
	private BigInteger increaseBalance(Storage eoa, BigInteger gas)
			throws ClassNotFoundException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
	
		BigInteger delta = GasCosts.toCoin(gas);
		Field balanceField = classLoader.contractClass.getDeclaredField("balance");
		balanceField.setAccessible(true); // since the field is private
		BigInteger previousBalance = (BigInteger) balanceField.get(eoa);
		BigInteger result = previousBalance.add(delta);
		balanceField.set(eoa, result);
		return result;
	}

	/**
	 * Checks if the given object is an externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not an externally owned account
	 * @throws ClassNotFoundException if the {@link takamaka.lang.ExternallyOwnedAccount} class cannot be found
	 *                                in the class path of the transaction
	 */
	private void checkIsExternallyOwned(Storage object) throws ClassNotFoundException, IllegalTransactionRequestException {
		if (!classLoader.externallyOwnedAccount.isAssignableFrom(object.getClass()))
			throw new IllegalTransactionRequestException("Only an externally owned contract can start a transaction");
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
	private SortedSet<Update> collectUpdates(Object[] actuals, Storage caller, Storage receiver, Object result) {
		List<Storage> potentiallyAffectedObjects = new ArrayList<>();
		if (caller != null)
			potentiallyAffectedObjects.add(caller);
		if (receiver != null)
			potentiallyAffectedObjects.add(receiver);
		if (result instanceof Storage)
			potentiallyAffectedObjects.add((Storage) result);

		if (actuals != null)
			for (Object actual: actuals)
				if (actual instanceof Storage)
					potentiallyAffectedObjects.add((Storage) actual);

		// events are accessible from outside, hence they count as side-effects
		events.forEach(potentiallyAffectedObjects::add);

		Set<StorageReference> seen = new HashSet<>();
		SortedSet<Update> updates = new TreeSet<>();
		potentiallyAffectedObjects.forEach(storage -> storage.updates(updates, seen));

		return updates;
	}

	/**
	 * The thread that executes a constructor or method of a storage object. It creates the class loader
	 * from the class path and deserializes receiver and actuals. Then calls the code and serializes
	 * the resulting value back.
	 */
	private abstract class CodeExecutor extends Thread {

		/**
		 * The exception resulting from the execution of the method or constructor, if any.
		 * This is {@code null} if the execution completed without exception.
		 */
		protected Throwable exception;

		/**
		 * The resulting value for methods or the created object for constructors.
		 * This is {@code null} if the execution completed with an exception or
		 * if the method actually returned {@code null}.
		 */
		protected Object result;

		/**
		 * The deserialized caller.
		 */
		private final Storage deserializedCaller;

		/**
		 * The method or constructor that is being called.
		 */
		protected final CodeSignature methodOrConstructor;
		
		/**
		 * True if the method has been called correctly and it is declared as {@code void},
		 */
		protected boolean isVoidMethod;

		/**
		 * True if the method has been called correctly and it is annotated as {@link takamaka.lang.View}.
		 */
		protected boolean isViewMethod;

		/**
		 * The deserialized receiver of a method call. This is {@code null} for static methods and constructors.
		 */
		protected final Storage deserializedReceiver; // it might be null

		/**
		 * The deserialized actual arguments of the call.
		 */
		protected final Object[] deserializedActuals;

		/**
		 * Builds the executor of a method or constructor.
		 * 
		 * @param classLoader the class loader that must be used to find the classes during the execution of the method or constructor
		 * @param deseralizedCaller the deserialized caller
		 * @param methodOrConstructor the method or constructor to call
		 * @param receiver the receiver of the call, if any. This is {@code null} for constructors and static methods
		 * @param actuals the actuals provided to the method or constructor
		 */
		private CodeExecutor(Storage deseralizedCaller, CodeSignature methodOrConstructor, StorageReference receiver, Stream<StorageValue> actuals) {
			setContextClassLoader(new ClassLoader(classLoader.getParent()) {

				@Override
				public Class<?> loadClass(String name) throws ClassNotFoundException {
					return classLoader.loadClass(name);
				}
			});

			this.deserializedCaller = deseralizedCaller;
			this.methodOrConstructor = methodOrConstructor;
			this.deserializedReceiver = receiver != null ? receiver.deserialize(AbstractBlockchain.this) : null;
			this.deserializedActuals = actuals.map(actual -> actual.deserialize(AbstractBlockchain.this)).toArray(Object[]::new);
		}

		/**
		 * A cache for {@link takamaka.blockchain.AbstractBlockchain.CodeExecutor#updates()}.
		 */
		private SortedSet<Update> updates;

		/**
		 * Yields the updates resulting from the execution of the method or constructor.
		 * 
		 * @return the updates
		 */
		protected final Stream<Update> updates() {
			if (updates != null)
				return updates.stream();

			return (updates = collectUpdates(deserializedActuals, deserializedCaller, deserializedReceiver, result)).stream();
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
			Class<?> returnType = method instanceof NonVoidMethodSignature ? ((NonVoidMethodSignature) method).returnType.toClass(AbstractBlockchain.this) : void.class;
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
			Class<?> returnType = method instanceof NonVoidMethodSignature ? ((NonVoidMethodSignature) method).returnType.toClass(AbstractBlockchain.this) : void.class;
			Class<?>[] argTypes = formalsAsClassForEntry();

			return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
				.orElseThrow(() -> new NoSuchMethodException(method.toString()));
		}

		/**
		 * Determines if the execution only affected the balance of the caller contract.
		 *
		 * @param deserializedCaller the caller contract
		 * @return true  if and only if that condition holds
		 */
		protected final boolean onlyAffectedBalanceOf(Storage deserializedCaller) {
			return updates().allMatch
				(update -> update.object.equals(deserializedCaller.storageReference)
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
				classes.add(type.toClass(AbstractBlockchain.this));

			return classes.toArray(new Class<?>[classes.size()]);
		}

		/**
		 * Yields the classes of the formal arguments of the method or constructor, assuming that it is
		 * and {@link takamaka.lang.Entry}. Entries are instrumented with the addition of
		 * trailing contract formal (the caller) and of a dummy type.
		 * 
		 * @return the array of classes, in the same order as the formals
		 * @throws ClassNotFoundException if some class cannot be found
		 */
		protected final Class<?>[] formalsAsClassForEntry() throws ClassNotFoundException {
			List<Class<?>> classes = new ArrayList<>();
			for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
				classes.add(type.toClass(AbstractBlockchain.this));

			classes.add(classLoader.contractClass);
			classes.add(Dummy.class);

			return classes.toArray(new Class<?>[classes.size()]);
		}

		/**
		 * Adds to the actual parameters the implicit actuals that are passed
		 * to {@link takamaka.lang.Entry} methods or constructors. They are the caller of
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
		 * Yields the same exception, if it is checked and the executable is annotated as {@link takamaka.lang.ThrowsExceptions}.
		 * Otherwise, yields its cause.
		 * 
		 * @param e the exception
		 * @param executable the method or constructor whose execution has thrown the exception
		 * @return the same exception, or its cause
		 */
		protected final Throwable unwrapInvocationException(InvocationTargetException e, Executable executable) {
			if (isChecked(e.getCause()) && executable.isAnnotationPresent(ThrowsExceptions.class))
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
				.map(Takamaka::getWhiteListingCheckFor)
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
	}

	/**
	 * The thread that executes a constructor of a storage object. It creates the class loader
	 * from the class path and deserializes the actuals. Then calls the code and serializes
	 * the resulting value back.
	 */
	private class ConstructorExecutor extends CodeExecutor {

		/**
		 * Builds the executor of a constructor.
		 * 
		 * @param constructor the constructor to call
		 * @param deseralizedCaller the deserialized caller
		 * @param actuals the actuals provided to the constructor
		 */
		private ConstructorExecutor(ConstructorSignature constructor, Storage deserializedCaller, Stream<StorageValue> actuals) {
			super(deserializedCaller, constructor, null, actuals);
		}

		/**
		 * Resolves the constructor that must be called.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws SecurityException if the constructor could not be accessed
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClass();

			return classLoader.resolveConstructor(methodOrConstructor.definingClass.name, argTypes)
				.orElseThrow(() -> new NoSuchMethodException(methodOrConstructor.toString()));
		}

		/**
		 * Resolves the constructor that must be called, assuming that it is an entry.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws SecurityException if the constructor could not be accessed
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getEntryConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClassForEntry();

			return classLoader.resolveConstructor(methodOrConstructor.definingClass.name, argTypes)
				.orElseThrow(() -> new NoSuchMethodException(methodOrConstructor.toString()));
		}

		@Override
		public void run() {
			try {
				Constructor<?> constructorJVM;
				Object[] deserializedActuals;

				try {
					// we first try to call the constructor with exactly the parameter types explicitly provided
					constructorJVM = getConstructor();
					deserializedActuals = this.deserializedActuals;
				}
				catch (NoSuchMethodException e) {
					// if not found, we try to add the trailing types that characterize the @Entry constructors
					try {
						constructorJVM = getEntryConstructor();
						deserializedActuals = addExtraActualsForEntry();
					}
					catch (NoSuchMethodException ee) {
						throw e; // the message must be relative to the constructor as the user sees it
					}
				}

				ensureWhiteListingOf(constructorJVM, deserializedActuals);

				try {
					result = (Storage) constructorJVM.newInstance(deserializedActuals);
				}
				catch (InvocationTargetException e) {
					exception = unwrapInvocationException(e, constructorJVM);
				}
			}
			catch (Throwable t) {
				exception = t;
			}
		}
	}

	/**
	 * The thread that executes an instance method of a storage object. It creates the class loader
	 * from the class path and deserializes receiver and actuals. Then calls the code and serializes
	 * the resulting value back.
	 */
	private class InstanceMethodExecutor extends CodeExecutor {

		/**
		 * Builds the executor of an instance method.
		 * 
		 * @param method the method to call
		 * @param deseralizedCaller the deserialized caller
		 * @param receiver the receiver of the method
		 * @param actuals the actuals provided to the method
		 */
		private InstanceMethodExecutor(MethodSignature method, Storage deserializedCaller, StorageReference receiver, Stream<StorageValue> actuals) {
			super(deserializedCaller, method, receiver, actuals);
		}

		@Override
		public void run() {
			try {
				Method methodJVM;
				Object[] deserializedActuals;

				try {
					// we first try to call the method with exactly the parameter types explicitly provided
					methodJVM = getMethod();
					deserializedActuals = this.deserializedActuals;
				}
				catch (NoSuchMethodException e) {
					// if not found, we try to add the trailing types that characterize the @Entry methods
					try {
						methodJVM = getEntryMethod();
						deserializedActuals = addExtraActualsForEntry();
					}
					catch (NoSuchMethodException ee) {
						throw e; // the message must be relative to the method as the user sees it
					}
				}

				if (Modifier.isStatic(methodJVM.getModifiers()))
					throw new NoSuchMethodException("Cannot call a static method: use addStaticMethodCallTransaction instead");

				ensureWhiteListingOf(methodJVM, deserializedActuals);

				isVoidMethod = methodJVM.getReturnType() == void.class;
				isViewMethod = methodJVM.isAnnotationPresent(View.class);

				try {
					result = methodJVM.invoke(deserializedReceiver, deserializedActuals);
				}
				catch (InvocationTargetException e) {
					exception = unwrapInvocationException(e, methodJVM);
				}
			}
			catch (Throwable t) {
				exception = t;
			}
		}
	}

	/**
	 * The thread that executes a static method of a storage object. It creates the class loader
	 * from the class path and deserializes the actuals. Then calls the code and serializes
	 * the resulting value back.
	 */
	private class StaticMethodExecutor extends CodeExecutor {

		/**
		 * Builds the executor of a static method.
		 * 
		 * @param method the method to call
		 * @param caller the caller, that pays for the execution
		 * @param deseralizedCaller the deserialized caller
		 * @param actuals the actuals provided to the method
		 */
		private StaticMethodExecutor(MethodSignature method, Storage deserializedCaller, Stream<StorageValue> actuals) {
			super(deserializedCaller, method, null, actuals);
		}

		@Override
		public void run() {
			try {
				Method methodJVM = getMethod();

				if (!Modifier.isStatic(methodJVM.getModifiers()))
					throw new NoSuchMethodException("Cannot call an instance method: use addInstanceMethodCallTransaction instead");

				ensureWhiteListingOf(methodJVM, deserializedActuals);

				isVoidMethod = methodJVM.getReturnType() == void.class;
				isViewMethod = methodJVM.isAnnotationPresent(View.class);

				try {
					result = methodJVM.invoke(null, deserializedActuals);
				}
				catch (InvocationTargetException e) {
					exception = unwrapInvocationException(e, methodJVM);
				}
			}
			catch (Throwable t) {
				exception = t;
			}
		}
	}

	/**
	 * Yields the amount of gas still available to the
	 * currently executing transaction.
	 * 
	 * @return the remaining gas
	 */
	private BigInteger remainingGas() {
		return gas;
	}

	/**
	 * Wraps the given throwable in a {@link takamaka.blockchain.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message added to the {@link takamaka.blockchain.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	private static TransactionException wrapAsTransactionException(Throwable t, String message) {
		if (t instanceof TransactionException)
			return (TransactionException) t;
		else
			return new TransactionException(message, t);
	}
}