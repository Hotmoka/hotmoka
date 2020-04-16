package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.OutOfGasError;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The creator of the response for a non-initial transaction. Non-initial transactions consume gas.
 */
public abstract class NonInitialResponseBuilder<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {

	/**
	 * The gas initially provided for the transaction.
	 */
	private final BigInteger gasLimit;

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
	 * The cost model of the node for which the transaction is being built.
	 */
	protected final GasCostModel gasCostModel;

	/**
	 * Creates a the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be built
	 */
	protected NonInitialResponseBuilder(Request request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			this.gasCostModel = node.getGasCostModel();
			this.gas = this.gasLimit = request.gasLimit;
			this.gasPrice = request.gasPrice;
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	/**
	 * Charges gas proportional to the complexity of the class loader that has been created.
	 */
	protected final void chargeGasForClassLoader() {
		EngineClassLoader classLoader = getClassLoader();
		classLoader.getLengthsOfJars().mapToObj(gasCostModel::cpuCostForLoadingJar).forEach(this::chargeGasForCPU);
		classLoader.getLengthsOfJars().mapToObj(gasCostModel::ramCostForLoadingJar).forEach(this::chargeGasForRAM);
		classLoader.getTransactionsOfJars().map(gasCostModel::cpuCostForGettingResponseAt).forEach(this::chargeGasForCPU);
	}

	/**
	 * Yields the caller of the transaction.
	 * 
	 * @return the caller
	 */
	protected abstract Object getDeserializedCaller();

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
	 * Checks if the caller is an externally owned account or subclass.
	 * 
	 * @throws IllegalArgumentException if the caller is not an externally owned account
	 */
	protected final void callerMustBeExternallyOwnedAccount() {
		Class<? extends Object> clazz = getDeserializedCaller().getClass();
		if (!getClassLoader().getExternallyOwnedAccount().isAssignableFrom(clazz)
				&& !getClassLoader().getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new IllegalArgumentException("only an externally owned account can start a transaction");
	}

	/**
	 * Checks if the caller has the same nonce as the request.
	 * 
	 * @throws IllegalArgumentException if the nonce of the caller is not equal to that in {@code request}
	 */
	protected void callerAndRequestMustAgreeOnNonce() {
		BigInteger expected = getClassLoader().getNonceOf(getDeserializedCaller());
		if (!expected.equals(request.nonce))
			throw new IllegalArgumentException("incorrect nonce: the request reports " + request.nonce + " but the account contains " + expected);
	}

	@Override
	public final void chargeGasForCPU(BigInteger amount) {
		charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
	}

	@Override
	public final void chargeGasForRAM(BigInteger amount) {
		charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
	}

	@Override
	public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		chargeGasForCPU(amount);
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
	 * Decreases the available gas for the request of this transaction, for storage allocation.
	 */
	protected final void chargeGasForStorageOfRequest() {
		chargeForStorage(sizeCalculator.sizeOfRequest(request));
	}

	/**
	 * Decreases the available gas for the given response, for storage allocation.
	 * 
	 * @param response the response
	 */
	protected final void chargeGasForStorage(Response response) {
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
	 * Sells to the caller of the transaction all gas promised for the transaction.
	 * 
	 * @throws IllegalStateException if the caller has not enough money to buy the promised gas
	 */
	protected final void sellAllGasToCaller() {
		Object eoa = getDeserializedCaller();
		EngineClassLoader classLoader = getClassLoader();
		BigInteger balance = classLoader.getBalanceOf(eoa);
		BigInteger cost = costOf(request.gasLimit);

		if (balance.subtract(cost).signum() < 0)
			throw new IllegalStateException("caller has not enough funds to buy " + request.gasLimit + " units of gas");

		classLoader.setBalanceOf(eoa, balance.subtract(cost));
	}

	/**
	 * Checks if the remaining gas for the transaction is enough to cover the cost of the addition of a
	 * failed transaction response to the store of the node.
	 * 
	 * @throws OutOfGasError if the gas is not enough
	 */
	protected final void remainingGasMustBeEnoughForStoringFailedResponse() throws OutOfGasError {
		if (gas.compareTo(gasForStoringFailedResponse()) < 0)
			throw new OutOfGasError("not enough gas to start the transaction");
	}

	/**
	 * Yields the cost for storage a failed response for the transaction that is being built.
	 * 
	 * @return the cost
	 */
	protected abstract BigInteger gasForStoringFailedResponse();

	/**
	 * Collects all updates to the balance or nonce of the caller of the transaction.
	 * 
	 * @return the updates
	 */
	protected final Stream<Update> updatesToBalanceOrNonceOfCaller() {
		Object caller = getDeserializedCaller();
		return updatesExtractor.extractUpdatesFrom(Stream.of(caller))
			.filter(update -> update.object.equals(request.caller))
			.filter(update -> update instanceof UpdateOfField)
			.filter(update -> ((UpdateOfField) update).getField().equals(FieldSignature.BALANCE_FIELD)
					|| ((UpdateOfField) update).getField().equals(FieldSignature.EOA_NONCE_FIELD)
					|| ((UpdateOfField) update).getField().equals(FieldSignature.RGEOA_NONCE_FIELD));
	}

	/**
	 * Buys back the remaining gas to the caller of the transaction.
	 */
	protected final void payBackAllRemainingGasToCaller() {
		Object caller = getDeserializedCaller();
		getClassLoader().setBalanceOf(caller, getClassLoader().getBalanceOf(caller).add(costOf(gas)));
	}

	/**
	 * Sets the nonce to the value successive to that in the request.
	 */
	protected void increaseNonceOfCaller() {
		getClassLoader().setNonceOf(getDeserializedCaller(), request.nonce.add(BigInteger.ONE));
	}

	/**
	 * Yields the remaining amount of gas for the current transaction, not yet consumed.
	 * 
	 * @return the amount of gas
	 */
	protected final BigInteger gas() {
		return gas;
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
		return gasLimit.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
	}
}