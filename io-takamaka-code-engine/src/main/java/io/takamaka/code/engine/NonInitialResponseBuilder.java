package io.takamaka.code.engine;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.OutOfGasError;
import io.takamaka.code.engine.internal.transactions.AbstractResponseBuilder;

/**
 * The creator of the response for a non-initial transaction. Non-initial transactions consume gas,
 * have a payer a nonce and a chain identifier and are signed. The constructor of this class checks
 * the validity of all these elements.
 */
public abstract class NonInitialResponseBuilder<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {
	private final static Logger logger = LoggerFactory.getLogger(NonInitialResponseBuilder.class);

	/**
	 * True if and only if the caller of the request is a red/green externally owned account.
	 * Otherwise it is a normal externally owned account.
	 */
	private final boolean callerIsRedGreen;

	/**
	 * True if and only if the payer of the request is a red/green contract.
	 * Otherwise it is a normal contract. Normally, caller and payer coincide,
	 * hence this field has the same value as {@link #callerIsRedGreen}.
	 * However, there might be future non-standard requests for which caller
	 * and payer differ.
	 */
	private final boolean payerIsRedGreen;

	/**
	 * True if and only if the request is a view request.
	 */
	private final boolean requestIsView;

	/**
	 * The cost model of the node for which the transaction is being built.
	 */
	protected final GasCostModel gasCostModel;

	/**
	 * Creates a the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be built
	 */
	protected NonInitialResponseBuilder(TransactionReference reference, Request request, AbstractLocalNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);

		try {
			this.requestIsView = NonInitialResponseBuilder.this instanceof ViewResponseBuilder;
			this.gasCostModel = node.getGasCostModel();
			this.callerIsRedGreen = callerMustBeExternallyOwnedAccount();
			this.payerIsRedGreen = payerMustBeContract();
			requestPromisesEnoughGas();
			requestMustHaveCorrectChainId();
			signatureMustBeValid();
			callerAndRequestMustAgreeOnNonce();
			payerCanPayForAllPromisedGas();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		return node.getCachedClassLoader(request.classpath);
	}

	/**
	 * Computes a minimal threshold of gas that is required for the transaction.
	 * Below this threshold, the response builder cannot be created.
	 * 
	 * @return the minimal threshold
	 */
	protected BigInteger minimalGasRequiredForTransaction() {
		BigInteger result = gasCostModel.cpuBaseTransactionCost();
		result = result.add(request.size(gasCostModel));
		result = result.add(gasForStoringFailedResponse());
		result = result.add(classLoader.getLengthsOfJars().mapToObj(gasCostModel::cpuCostForLoadingJar).reduce(ZERO, BigInteger::add));
		result = result.add(classLoader.getLengthsOfJars().mapToObj(gasCostModel::ramCostForLoadingJar).reduce(ZERO, BigInteger::add));
		result = result.add(classLoader.getTransactionsOfJars().map(gasCostModel::cpuCostForGettingResponseAt).reduce(ZERO, BigInteger::add));
	
		return result;
	}

	/**
	 * Extracts the payer from the request. Normally, this is its caller,
	 * but subclasses might redefine.
	 * 
	 * @return the payer
	 */
	protected StorageReference getPayerFromRequest() {
		return request.caller;
	}

	/**
	 * Yields the cost for storage a failed response for the transaction that is being built.
	 * 
	 * @return the cost
	 */
	protected abstract BigInteger gasForStoringFailedResponse();

	/**
	 * Determines if the node allows the use of the @SelfCharged annotation.
	 * 
	 * @return true if only if that condition holds
	 */
	protected final boolean nodeAdmitsSelfCharged() {
		return node.admitsSelfCharged();
	}

	/**
	 * Checks if the caller is an externally owned account or subclass.
	 *
	 * @return true if the caller is a red/green externally owned account, false if it is
	 *         a normal account
	 * @throws TransactionRejectedException if the caller is not an externally owned account
	 * @throws ClassNotFoundException if the class of the caller cannot be determined
	 */
	private boolean callerMustBeExternallyOwnedAccount() throws TransactionRejectedException, ClassNotFoundException {
		ClassTag classTag = node.getClassTag(request.caller);
		Class<?> clazz = classLoader.loadClass(classTag.className);
		if (classLoader.getExternallyOwnedAccount().isAssignableFrom(clazz))
			return false;
		else if (classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			return true;
		else
			throw new TransactionRejectedException("the caller of a request must be an externally owned account");
	}

	/**
	 * Checks if the payer is a contract or subclass.
	 *
	 * @return true if the payer is a red/green contract, false if it is a normal contract
	 * @throws TransactionRejectedException if the payer is not a contract
	 * @throws ClassNotFoundException if the class of the payer cannot be determined
	 */
	private boolean payerMustBeContract() throws TransactionRejectedException, ClassNotFoundException {
		StorageReference payer = getPayerFromRequest();
	
		if (payer.equals(request.caller))
			// if the payer coincides with the caller, as it is normally the case,
			// then there is nothing to check, since we know that the caller
			// is an externally owned account, hence a contract
			return callerIsRedGreen;
	
		// otherwise we check
		ClassTag classTag = node.getClassTag(payer);
		Class<?> clazz = classLoader.loadClass(classTag.className);
		if (classLoader.getRedGreenContract().isAssignableFrom(clazz))
			return true;
		else if (classLoader.getContract().isAssignableFrom(clazz))
			return false;
		else
			throw new TransactionRejectedException("the payer of a request must be a contract");
	}

	/**
	 * Checks that the request is signed with the private key of its caller.
	 * 
	 * @throws Exception if the signature of the request could not be checked
	 */
	private void signatureMustBeValid() throws Exception {
		if (!node.signatureIsValid(request))
			throw new TransactionRejectedException("invalid request signature");
	}

	/**
	 * Checks if the node has the same chain identifier as the request.
	 * 
	 * @throws TransactionRejectedException if the node and the request have different chain identifiers
	 */
	private void requestMustHaveCorrectChainId() throws TransactionRejectedException {
		// calls to @View methods do not check the chain identifier
		if (!requestIsView) {
			String chainIdOfNode = node.getChainId();

			if (!chainIdOfNode.equals(request.chainId))
				throw new TransactionRejectedException("incorrect chain id: the request reports " + request.chainId + " but the node requires " + chainIdOfNode);
		}
	}

	/**
	 * Checks if the caller has the same nonce as the request.
	 * 
	 * @throws TransactionRejectedException if the nonce of the caller is not equal to that in {@code request}
	 */
	private void callerAndRequestMustAgreeOnNonce() throws TransactionRejectedException {
		// calls to @View methods do not check the nonce
		if (!requestIsView) {
			BigInteger expected = node.getNonce(request.caller, callerIsRedGreen);

			if (!expected.equals(request.nonce))
				throw new TransactionRejectedException("incorrect nonce: the request reports " + request.nonce
					+ " but the account " + request.caller + " contains " + expected);
		}
	}

	/**
	 * Checks that the request provides a minimal threshold of gas for starting the transaction.
	 * 
	 * @throws TransactionRejectedException if the request provides too little gas
	 */
	private void requestPromisesEnoughGas() throws TransactionRejectedException {
		BigInteger minimum = minimalGasRequiredForTransaction();
		if (request.gasLimit.compareTo(minimum) < 0)
			throw new TransactionRejectedException("not enough gas to start the transaction, expected at least " + minimum + " units of gas");
	}

	/**
	 * Checks if the payer of the request has enough funds for paying for all gas promised
	 * (green and red coins together).
	 * 
	 * @throws TransactionRejectedException if the payer is not rich enough for that
	 */
	private void payerCanPayForAllPromisedGas() throws TransactionRejectedException {
		BigInteger cost = costOf(request.gasLimit);
		BigInteger totalBalance = node.getTotalBalance(getPayerFromRequest(), payerIsRedGreen);

		if (totalBalance.subtract(cost).signum() < 0)
			throw new TransactionRejectedException("the payer has not enough funds to buy " + request.gasLimit + " units of gas");
	}

	/**
	 * Computes the cost of the given units of gas.
	 * 
	 * @param gas the units of gas
	 * @return the cost, as {@code gas} times {@code gasPrice}
	 */
	private BigInteger costOf(BigInteger gas) {
		return gas.multiply(request.gasPrice);
	}

	protected abstract class ResponseCreator extends AbstractResponseBuilder<Request, Response>.ResponseCreator {

		/**
		 * The deserialized caller.
		 */
		private Object deserializedCaller;

		/**
		 * The deserialized payer.
		 */
		private Object deserializedPayer;

		/**
		 * A stack of available gas. When a sub-computation is started
		 * with a subset of the available gas, the latter is taken away from
		 * the current available gas and pushed on top of this stack.
		 */
		private final LinkedList<BigInteger> oldGas = new LinkedList<>();

		/**
		 * The remaining amount of gas for the current transaction, not yet consumed.
		 */
		private BigInteger gas;

		/**
		 * The amount of gas consumed for CPU execution.
		 */
		private BigInteger gasConsumedForCPU = ZERO;

		/**
		 * The amount of gas consumed for RAM allocation.
		 */
		private BigInteger gasConsumedForRAM = ZERO;

		/**
		 * The amount of gas consumed for storage consumption.
		 */
		private BigInteger gasConsumedForStorage = ZERO;

		/**
		 * The amount of green coins that have been deduced at the beginning
		 * for paying the gas in full.
		 */
		private BigInteger greenInitiallyPaidForGas;

		protected ResponseCreator() throws TransactionRejectedException {
			try {
				this.gas = request.gasLimit;
			}
			catch (Throwable t) {
				logger.error("response creation rejected", t);
				throw wrapAsTransactionRejectedException(t);
			}
		}

		protected final void init() throws Exception {
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedPayer = deserializePayer();
			increaseNonceOfCaller();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorage(request.size(gasCostModel));
			chargeGasForClassLoader();				
			this.greenInitiallyPaidForGas = chargePayerForAllGasPromised();
		}

		/**
		 * Yields the contract that pays for the transaction.
		 * This normally coincides with {@link #getDeserializedCaller()}
		 * but subclasses may redefine.
		 * 
		 * @return the payer for the transaction
		 */
		protected Object deserializePayer() {
			return deserializedCaller;
		}

		/**
		 * Yields the deserialized caller of the transaction.
		 * 
		 * @return the deserialized caller
		 */
		protected final Object getDeserializedCaller() {
			return deserializedCaller;
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
			return request.gasLimit.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
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
		 * Decreases the available gas by the given amount, for storage allocation.
		 * 
		 * @param amount the amount of gas to consume
		 */
		private void chargeGasForStorage(BigInteger amount) {
			charge(amount, x -> gasConsumedForStorage = gasConsumedForStorage.add(x));
		}

		/**
		 * Decreases the available gas for the given response, for storage allocation.
		 * 
		 * @param response the response
		 */
		protected final void chargeGasForStorageOf(Response response) {
			chargeGasForStorage(response.size(gasCostModel));
		}

		@Override
		public final void chargeGasForCPU(BigInteger amount) {
			charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
		}

		@Override
		public final void chargeGasForRAM(BigInteger amount) {
			charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
		}

		/**
		 * Charges gas proportional to the complexity of the class loader that has been created.
		 */
		protected final void chargeGasForClassLoader() {
			classLoader.getLengthsOfJars().mapToObj(gasCostModel::cpuCostForLoadingJar).forEach(this::chargeGasForCPU);
			classLoader.getLengthsOfJars().mapToObj(gasCostModel::ramCostForLoadingJar).forEach(this::chargeGasForRAM);
			classLoader.getTransactionsOfJars().map(gasCostModel::cpuCostForGettingResponseAt).forEach(this::chargeGasForCPU);
		}

		/**
		 * Collects all updates to the balance or nonce of the caller of the transaction.
		 * 
		 * @return the updates
		 */
		protected final Stream<Update> updatesToBalanceOrNonceOfCaller() {
			return updatesExtractor.extractUpdatesFrom(Stream.of(deserializedCaller))
				.filter(update -> update.object.equals(request.caller))
				.filter(update -> update instanceof UpdateOfField)
				.filter(update -> ((UpdateOfField) update).getField().equals(FieldSignature.BALANCE_FIELD)
						|| ((UpdateOfField) update).getField().equals(FieldSignature.EOA_NONCE_FIELD)
						|| ((UpdateOfField) update).getField().equals(FieldSignature.RGEOA_NONCE_FIELD));
		}

		/**
		 * Charge to the payer of the transaction all gas promised for the transaction.
		 * 
		 * @return the amount that has been subtracted from the green balance
		 * @throws TransactionRejectedException if the payer has not enough money to buy the promised gas
		 */
		private BigInteger chargePayerForAllGasPromised() throws TransactionRejectedException {
			BigInteger cost = costOf(request.gasLimit);

			if (payerIsRedGreen) {
				BigInteger greenBalance = classLoader.getBalanceOf(deserializedPayer);
				BigInteger redBalance = classLoader.getRedBalanceOf(deserializedPayer);

				// we check first if the payer can pay with red coins only
				BigInteger newRedBalance = redBalance.subtract(cost);
				if (newRedBalance.signum() >= 0) {
					classLoader.setRedBalanceOf(deserializedPayer, newRedBalance);
					return ZERO;
				}
				else {
					// otherwise, its red coins are set to 0 and the remainder is paid with green coins
					classLoader.setRedBalanceOf(deserializedPayer, ZERO);
					classLoader.setBalanceOf(deserializedPayer, greenBalance.add(newRedBalance));
					return newRedBalance.negate();
				}
			}
			else {
				BigInteger balance = classLoader.getBalanceOf(deserializedPayer);
				BigInteger newBalance = balance.subtract(cost);
				classLoader.setBalanceOf(deserializedPayer, newBalance);

				return cost;
			}
		}

		/**
		 * Pays back the remaining gas to the payer of the transaction.
		 */
		protected final void refundPayerForAllRemainingGas() {
			BigInteger refund = costOf(gas);
			BigInteger greenBalance = classLoader.getBalanceOf(deserializedPayer);

			if (payerIsRedGreen) {
				// we pay back the green before
				if (refund.subtract(greenInitiallyPaidForGas).signum() <= 0)
					classLoader.setBalanceOf(deserializedPayer, greenBalance.add(refund));
				else {
					BigInteger redBalance = classLoader.getRedBalanceOf(deserializedPayer);
					classLoader.setBalanceOf(deserializedPayer, greenBalance.add(greenInitiallyPaidForGas));
					classLoader.setRedBalanceOf(deserializedPayer, redBalance.add(refund.subtract(greenInitiallyPaidForGas)));
				}
			}
			else
				classLoader.setBalanceOf(deserializedPayer, greenBalance.add(refund));
		}

		@Override
		public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
			chargeGasForCPU(amount);
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
		 * Sets the nonce to the value successive to that in the request.
		 */
		private void increaseNonceOfCaller() {
			if (!requestIsView)
				classLoader.setNonceOf(deserializedCaller, request.nonce.add(ONE));
		}
	}
}