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
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.OutOfGasError;

/**
 * The creator of a non-initial transaction. Non-initial transactions consume gas.
 */
public abstract class NonInitialTransactionBuilder<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractTransactionBuilder<Request, Response> {

	/**
	 * The gas initially provided for the transaction.
	 */
	private final BigInteger initialGas;

	/**
	 * The coins payed for each unit of gas consumed by the transaction.
	 */
	private final BigInteger gasPrice;

	/**
	 * The remaining amount of gas for the current transaction, not yet consumed.
	 */
	private BigInteger gas;

	/**
	 * The amount of gas consumed for CPU execution.
	 */
	private BigInteger gasConsumedForCPU = BigInteger.ZERO;

	/**
	 * The amount of gas consumed for RAM allocation.
	 */
	private BigInteger gasConsumedForRAM = BigInteger.ZERO;

	/**
	 * The amount of gas consumed for storage consumption.
	 */
	private BigInteger gasConsumedForStorage = BigInteger.ZERO;

	/**
	 * A stack of available gas. When a sub-computation is started
	 * with a subset of the available gas, the latter is taken away from
	 * the current available gas and pushed on top of this stack.
	 */
	private final LinkedList<BigInteger> oldGas = new LinkedList<>();

	/**
	 * Builds a non-initial transaction creator.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used to refer to the created transaction
	 * @param node the node that is creating the transaction
	 * @throws TransactionException if the creator cannot be built
	 */
	protected NonInitialTransactionBuilder(Request request, TransactionReference current, Node node) throws TransactionException {
		super(current, node);

		this.gas = this.initialGas = request.gas;
		this.gasPrice = request.gasPrice;
	}

	/**
	 * Reduces the remaining amount of gas. It performs a task at the end.
	 * 
	 * @param amount the amount of gas to consume
	 * @param forWhat the task performed at the end, for the amount of gas to consume
	 */
	private void charge(BigInteger amount, Consumer<BigInteger> forWhat) {
		if (amount.signum() < 0)
			throw new IllegalArgumentException("gas cannot increase");

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

	/**
	 * Checks if the given object is an externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalArgumentException if the object is not an externally owned account
	 */
	protected final void checkIsExternallyOwned(Object object) {
		Class<? extends Object> clazz = object.getClass();
		if (!getClassLoader().getExternallyOwnedAccount().isAssignableFrom(clazz)
				&& !getClassLoader().getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new IllegalArgumentException("only an externally owned account can start a transaction");
	}

	@Override
	public final void chargeForCPU(BigInteger amount) {
		charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
	}

	@Override
	public final void chargeForRAM(BigInteger amount) {
		charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
	}

	@Override
	public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		chargeForCPU(amount);
		oldGas.addFirst(gas);
		amount.hashCode();
		gas = amount;
	
		try {
			return what.call();
		}
		finally {
			gas = gas.add(oldGas.removeFirst());
		}
	}

	/**
	 * Decreases the available gas by the given amount, for storage allocation.
	 * 
	 * @param amount the amount of gas to consume
	 */
	private void chargeForStorage(BigInteger amount) {
		charge(amount, x -> gasConsumedForStorage = gasConsumedForStorage.add(x));
	}

	/**
	 * Decreases the available gas for the given request, for storage allocation.
	 * 
	 * @param request the request
	 */
	protected final void chargeForStorage(Request request) {
		chargeForStorage(sizeCalculator.sizeOfRequest(request));
	}

	/**
	 * Decreases the available gas for the given response, for storage allocation.
	 * 
	 * @param response the response
	 */
	protected final void chargeForStorage(Response response) {
		chargeForStorage(sizeCalculator.sizeOfResponse(response));
	}

	/**
	 * Computes the cost of the given units of gas.
	 * 
	 * @param gas the units of gas
	 * @return the cost, as {@code gas} times {@code gasPrice}
	 */
	private BigInteger costOf(BigInteger gas) {
		return gas.multiply(gasPrice);
	}

	/**
	 * Checks if the caller of a transaction has enough money at least for paying the promised gas and the addition of a
	 * failed transaction response to blockchain.
	 * 
	 * @param request the request
	 * @param deserializedCaller the caller
	 * @return the update to the balance that would follow if the failed transaction would be stored in the node
	 * @throws IllegalStateException if the caller has not enough money to buy the promised gas and the addition
	 *                               of a failed transaction response to the node
	 */
	protected final UpdateOfBalance checkMinimalGas(NonInitialTransactionRequest<?> request, Object deserializedCaller) {
		BigInteger decreasedBalanceOfCaller = decreaseBalance(deserializedCaller, request.gas);
		UpdateOfBalance balanceUpdateInCaseOfFailure = new UpdateOfBalance(getClassLoader().getStorageReferenceOf(deserializedCaller), decreasedBalanceOfCaller);

		if (gas.compareTo(minimalGasForRunning(request, balanceUpdateInCaseOfFailure)) < 0)
			throw new IllegalStateException("not enough gas to start the transaction");

		return balanceUpdateInCaseOfFailure;
	}

	/**
	 * Computes the minimal gas needed to run a given request. It accounts for storing in the node the failed transaction response.
	 * 
	 * @param request the request
	 * @param balanceUpdateInCaseOfFailure the update that must be stored in the node to account for the consumption of the all the gas
	 * @return the minimal gas
	 */
	private BigInteger minimalGasForRunning(NonInitialTransactionRequest<?> request, UpdateOfBalance balanceUpdateInCaseOfFailure) {
		// we create a response whose size over-approximates that of a response in case of failure of this request
		BigInteger result = node.getGasCostModel().cpuBaseTransactionCost().add(sizeCalculator.sizeOfRequest(request));
		if (request instanceof ConstructorCallTransactionRequest)
			return result.add(sizeCalculator.sizeOfResponse(new ConstructorCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof InstanceMethodCallTransactionRequest)
			return result.add(sizeCalculator.sizeOfResponse(new MethodCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof StaticMethodCallTransactionRequest)
			return result.add(sizeCalculator.sizeOfResponse(new MethodCallTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else if (request instanceof JarStoreTransactionRequest)
			return result.add(sizeCalculator.sizeOfResponse(new JarStoreTransactionFailedResponse(null, balanceUpdateInCaseOfFailure, gas, gas, gas, gas)));
		else
			throw new IllegalStateException("unexpected transaction request");
	}

	/**
	 * Sells the given amount of gas to the given externally owned account.
	 * 
	 * @param eoa the reference to the externally owned account
	 * @param gas the gas to sell
	 * @return the balance of the contract after paying the given amount of gas
	 * @throws IllegalStateException if the externally owned account does not have funds for buying the given amount of gas
	 */
	private BigInteger decreaseBalance(Object eoa, BigInteger gas) {
		BigInteger result = getClassLoader().getBalanceOf(eoa).subtract(costOf(gas));
		if (result.signum() < 0)
			throw new IllegalStateException("caller has not enough funds to buy " + gas + " units of gas");

		getClassLoader().setBalanceOf(eoa, result);
		return result;
	}

	/**
	 * Buys back the remaining gas to the caller of the given externally owned account.
	 * 
	 * @param eoa the externally owned account
	 * @return the balance of the contract after buying back the remaining gas
	 */
	protected final BigInteger increaseBalance(Object eoa) {
		BigInteger result = getClassLoader().getBalanceOf(eoa).add(costOf(gas));
		getClassLoader().setBalanceOf(eoa, result);
		return result;
	}

	/**
	 * Yields the amount of gas consumed for CPU execution.
	 * 
	 * @return the amount of gas
	 */
	protected final BigInteger gasConsumedForCPU() {
		return gasConsumedForCPU;
	}

	/**
	 * Yields the amount of gas consumed for RAM allocation.
	 * 
	 * @return the amount of gas
	 */
	protected final BigInteger gasConsumedForRAM() {
		return gasConsumedForRAM;
	}

	/**
	 * Yields the amount of gas consumed for storage consumption.
	 * 
	 * @return the amount of gas
	 */
	protected final BigInteger gasConsumedForStorage() {
		return gasConsumedForStorage;
	}

	/**
	 * Yields the gas that would be paid if the transaction fails.
	 * 
	 * @return the gas for penalty, computed as the total initial gas minus
	 *         the gas already consumed for PCU, for RAM and for storage
	 */
	protected final BigInteger gasConsumedForPenalty() {
		return initialGas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
	}
}