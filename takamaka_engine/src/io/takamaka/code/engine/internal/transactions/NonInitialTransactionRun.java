package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
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
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.OutOfGasError;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class NonInitialTransactionRun<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractTransactionRun<Request, Response> {

	protected NonInitialTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);
	}

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

	@Override
	public final void chargeForCPU(BigInteger amount) {
		charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
	}

	@Override
	public final void chargeForRAM(BigInteger amount) {
		charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
	}

	@Override
	public final void chargeForStorage(BigInteger amount) {
		charge(amount, x -> gasConsumedForStorage = gasConsumedForStorage.add(x));
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
	 * Checks if the caller of a transaction has enough money at least for paying the promised gas and the addition of a
	 * failed transaction response to blockchain.
	 * 
	 * @param request the request
	 * @param deserializedCaller the caller
	 * @return the update to the balance that would follow if the failed transaction request is added to the blockchain
	 * @throws IllegalTransactionRequestException if the caller has not enough money to buy the promised gas and the addition
	 *                                            of a failed transaction response to blockchain
	 */
	public final UpdateOfBalance checkMinimalGas(NonInitialTransactionRequest<?> request, Object deserializedCaller) throws IllegalTransactionRequestException {
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
	 * Buys back the remaining gas to the given externally owned account.
	 * 
	 * @param eoa the externally owned account
	 * @return the balance of the contract after buying back the remaining gas
	 */
	protected final BigInteger increaseBalance(Object eoa) {
		BigInteger result = classLoader.getBalanceOf(eoa).add(node.getGasCostModel().toCoin(gas));
		classLoader.setBalanceOf(eoa, result);
		return result;
	}
}