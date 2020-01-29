package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.OutOfGasError;
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
	 * The class loader for the transaction currently being executed.
	 */
	public EngineClassLoaderImpl classLoader;

	/**
	 * A stack of available gas. When a sub-computation is started
	 * with a subset of the available gas, the latter is taken away from
	 * the current available gas and pushed on top of this stack.
	 */
	private final LinkedList<BigInteger> oldGas = new LinkedList<>();

	/**
	 * The reference to the transaction where this must be executed.
	 */
	private final TransactionReference current;

	/**
	 * The remaining amount of gas for the current transaction, not yet consumed.
	 */
	private BigInteger gas;

	private final long now;

	protected AbstractTransactionRun(Request request, TransactionReference current, Node node, BigInteger gas) throws TransactionException {
		this.request = request;
		Runtime.init(this);
		ClassType.clearCache();
		FieldSignature.clearCache();
		this.node = node;
		this.now = wrapInCaseOfException(node::getNow);
		this.deserializer = new Deserializer(this);
		this.gas = gas;
		this.sizeCalculator = new SizeCalculator(node.getGasCostModel());
		this.current = current;
		this.response = wrapInCaseOfException(this::computeResponse);
	}

	@Override
	public final TransactionReference getCurrentTransaction() {
		return current;
	}

	@Override
	public final long now() {
		return now;
	}

	private void charge(BigInteger amount, Consumer<BigInteger> forWhat) {
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
		forWhat.accept(amount);
	}

	@Override
	public final void chargeForCPU(BigInteger amount) {
		charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
	}

	@Override
	public final void chargeForRAM(BigInteger amount) {
		charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
	}

	/**
	 * Decreases the available gas by the given amount, for storage allocation.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public final void chargeForStorage(BigInteger amount) {
		charge(amount, x -> gasConsumedForStorage = gasConsumedForStorage.add(x));
	}

	@Override
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

	protected abstract Response computeResponse() throws Exception;

	@Override
	public final Object deserializeLastLazyUpdateFor(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(node.getLastLazyUpdateToNonFinalFieldOf(reference, field, this::chargeForCPU).getValue());
	}

	@Override
	public final Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field) throws Exception {
		return deserializer.deserialize(node.getLastLazyUpdateToFinalFieldOf(reference, field, this::chargeForCPU).getValue());
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
	 */
	protected final UpdateOfBalance checkMinimalGas(NonInitialTransactionRequest<?> request, Object deserializedCaller) throws IllegalTransactionRequestException {
		BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
		UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(classLoader.getStorageReferenceOf(deserializedCaller), decreasedBalanceOfCaller);

		if (gas.compareTo(minimalGasForRunning(request, balanceUpdateInCaseOfFailure)) < 0)
			throw new IllegalTransactionRequestException("Not enough gas to start the transaction");

		return balanceUpdateInCaseOfFailure;
	}

	private BigInteger minimalGasForRunning(NonInitialTransactionRequest<?> request, UpdateOfBalance balanceUpdateInCaseOfFailure) throws IllegalTransactionRequestException {
		// we create a response whose size over-approximates that of a response in case of failure of this request
		BigInteger result = node.getGasCostModel().cpuBaseTransactionCost().add(sizeCalculator.sizeOf(request));
		if (request instanceof ConstructorCallTransactionRequest)
			return result.add(sizeCalculator.sizeOf(new ConstructorCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof InstanceMethodCallTransactionRequest)
			return result.add(sizeCalculator.sizeOf(new MethodCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof StaticMethodCallTransactionRequest)
			return result.add(sizeCalculator.sizeOf(new MethodCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof JarStoreTransactionRequest)
			return result.add(sizeCalculator.sizeOf(new JarStoreTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
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
	private static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
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
	 */
	private BigInteger decreaseBalance(Object eoa, BigInteger gas) throws IllegalTransactionRequestException {
		BigInteger result = classLoader.getBalanceOf(eoa).subtract(node.getGasCostModel().toCoin(gas));
		if (result.signum() < 0)
			throw new IllegalTransactionRequestException("Caller has not enough funds to buy " + gas + " units of gas");

		classLoader.setBalanceOf(eoa, result);
		return result;
	}

	/**
	 * Buys back the given amount of gas from the given externally owned account.
	 * 
	 * @param eoa the reference to the externally owned account
	 * @param gas the gas to buy back
	 * @return the balance of the contract after buying back the given amount of gas
	 */
	protected final BigInteger increaseBalance(Object eoa) {
		BigInteger result = classLoader.getBalanceOf(eoa).add(node.getGasCostModel().toCoin(gas));
		classLoader.setBalanceOf(eoa, result);
		return result;
	}

	/**
	 * Checks if the given object is an externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not an externally owned account
	 */
	protected final void checkIsExternallyOwned(Object object) throws IllegalTransactionRequestException {
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

	@Override
	public EngineClassLoaderImpl getClassLoader() {
		return classLoader;
	}
}