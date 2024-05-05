/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local.internal;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.OutOfGasError;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.StoreTransaction;
import io.hotmoka.node.local.internal.transactions.AbstractResponseBuilder;

/**
 * Implementation of the creator of the response for a non-initial transaction. Non-initial transactions consume gas,
 * have a payer a nonce and a chain identifier and are signed. The constructor of this class checks
 * the validity of all these elements.
 */
public abstract class NonInitialResponseBuilderImpl<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {
	private final static Logger LOGGER = Logger.getLogger(NonInitialResponseBuilderImpl.class.getName());

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
	 * @param maxGasAllowedForTransaction the maximal gas allowed for this transaction. If the gas limit of the request is larger
	 *                                    than this, then the transaction will be rejected
	 * @throws TransactionRejectedException if the builder cannot be built
	 */
	protected NonInitialResponseBuilderImpl(TransactionReference reference, Request request, StoreTransaction<?> storeTransaction, ConsensusConfig<?,?> consensus, BigInteger maxGasAllowedForTransaction, AbstractLocalNodeImpl<?,?> node) throws TransactionRejectedException {
		super(reference, request, storeTransaction, consensus, node);

		try {
			this.gasCostModel = node.getGasCostModel();
			callerMustBeExternallyOwnedAccount();
			gasLimitIsInsideBounds(maxGasAllowedForTransaction);
			requestPromisesEnoughGas();
			gasPriceIsLargeEnough();
			requestMustHaveCorrectChainId();
			signatureMustBeValid();
			callerAndRequestMustAgreeOnNonce();
			payerCanPayForAllPromisedGas();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	/**
	 * Determines if the transaction is signed.
	 * 
	 * @return true if and only if the request is signed and the transaction is not a view transaction
	 */
	protected boolean transactionIsSigned() {
		return !isView() && request instanceof SignedTransactionRequest;
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws NodeException, TransactionRejectedException {
		try {
			return storeTransaction.getClassLoader(request.getClasspath(), consensus);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Computes a minimal threshold of gas that is required for the transaction.
	 * Below this threshold, the response builder cannot be created.
	 * 
	 * @return the minimal threshold
	 */
	protected BigInteger minimalGasRequiredForTransaction() {
		BigInteger result = gasCostModel.cpuBaseTransactionCost();
		result = result.add(BigInteger.valueOf(request.size()));
		result = result.add(BigInteger.valueOf(gasForStoringFailedResponse()));
		result = result.add(classLoader.getLengthsOfJars().mapToObj(gasCostModel::cpuCostForLoadingJar).reduce(ZERO, BigInteger::add));
		result = result.add(classLoader.getLengthsOfJars().mapToObj(gasCostModel::ramCostForLoadingJar).reduce(ZERO, BigInteger::add));
	
		return result;
	}

	/**
	 * Yields the cost for storage a failed response for the transaction that is being built.
	 * 
	 * @return the cost
	 */
	protected abstract int gasForStoringFailedResponse();

	/**
	 * Determines if the transaction is a view transaction, that is, it has no side-effect on the store.
	 * 
	 * @return true if and only if the transaction is a view transaction
	 */
	protected boolean isView() {
		return false; // subclasses may redefine
	}

	/**
	 * Determine the signature algorithm that must have been used for signing the request.
	 * This depends on the run-time class of the caller of the request.
	 * 
	 * @return the signature algorithm
	 * @throws NodeException 
	 */
	private SignatureAlgorithm determineSignatureAlgorithm() throws NodeException, TransactionRejectedException {
		try {
			ClassTag classTag = storeTransaction.getClassTagUncommitted(request.getCaller());
			Class<?> clazz = classLoader.loadClass(classTag.getClazz().getName());

			if (classLoader.getAccountED25519().isAssignableFrom(clazz))
				return SignatureAlgorithms.ed25519();
			else if (classLoader.getAccountSHA256DSA().isAssignableFrom(clazz))
				return SignatureAlgorithms.sha256dsa();
			else if (classLoader.getAccountQTESLA1().isAssignableFrom(clazz))
				return SignatureAlgorithms.qtesla1();
			else if (classLoader.getAccountQTESLA3().isAssignableFrom(clazz))
				return SignatureAlgorithms.qtesla3();
			else
				return consensus.getSignatureForRequests();
		}
		catch (NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}
		catch (ClassNotFoundException e) {
			throw new TransactionRejectedException(e);
		}
	}

	/**
	 * Checks if the caller is an externally owned account or subclass.
	 *
	 * @throws TransactionRejectedException if the caller is not an externally owned account
	 * @throws ClassNotFoundException if the class of the caller cannot be determined
	 * @throws NodeException 
	 * @throws NoSuchElementException 
	 * @throws UnknownReferenceException 
	 */
	private void callerMustBeExternallyOwnedAccount() throws TransactionRejectedException, ClassNotFoundException, NodeException, UnknownReferenceException {
		try {
			ClassTag classTag = storeTransaction.getClassTagUncommitted(request.getCaller());
			Class<?> clazz = classLoader.loadClass(classTag.getClazz().getName());
			if (!classLoader.getExternallyOwnedAccount().isAssignableFrom(clazz))
				throw new TransactionRejectedException("the caller of a request must be an externally owned account");
		}
		catch (NoSuchElementException e) {
			throw new UnknownReferenceException(e);
		}
	}

	/**
	 * Checks that the request is signed with the private key of its caller.
	 * 
	 * @throws NodeException if the signature of the request could not be checked
	 */
	private void signatureMustBeValid() throws NodeException, TransactionRejectedException {
		try {
			// if the node is not initialized yet, the signature is not checked
			if (transactionIsSigned() && storeTransaction.nodeIsInitializedUncommitted()
					&& !storeTransaction.signatureIsValidUncommitted((SignedTransactionRequest<?>) request, determineSignatureAlgorithm()))
				throw new TransactionRejectedException("invalid request signature");
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Checks if the node has the same chain identifier as the request.
	 * 
	 * @throws TransactionRejectedException if the node and the request have different chain identifiers
	 */
	private void requestMustHaveCorrectChainId() throws TransactionRejectedException {
		// unsigned transactions do not check the chain identifier;
		// if the node is not initialized yet, the chain id is not checked
		try {
			if (transactionIsSigned() && storeTransaction.nodeIsInitializedUncommitted()) {
				String chainIdOfNode = consensus.getChainId();
				String chainId = ((SignedTransactionRequest<?>) request).getChainId();
				if (!chainIdOfNode.equals(chainId))
					throw new TransactionRejectedException("Incorrect chain id: the request reports " + chainId + " but the node requires " + chainIdOfNode);
			}
		}
		catch (StoreException e) {
			LOGGER.log(Level.SEVERE, "", e);
		}
	}

	/**
	 * Checks if the caller has the same nonce as the request.
	 * 
	 * @throws TransactionRejectedException if the nonce of the caller is not equal to that in {@code request}
	 */
	private void callerAndRequestMustAgreeOnNonce() throws TransactionRejectedException {
		// calls to @View methods do not check the nonce
		if (!isView()) {
			BigInteger expected = storeTransaction.getNonceUncommitted(request.getCaller());
			if (!expected.equals(request.getNonce()))
				throw new TransactionRejectedException("Incorrect nonce: the request reports " + request.getNonce()
					+ " but the account " + request.getCaller() + " contains " + expected);
		}
	}

	/**
	 * Checks that the request provides a minimal threshold of gas for starting the transaction.
	 * 
	 * @throws TransactionRejectedException if the request provides too little gas
	 */
	private void requestPromisesEnoughGas() throws TransactionRejectedException {
		BigInteger minimum = minimalGasRequiredForTransaction();
		if (request.getGasLimit().compareTo(minimum) < 0)
			throw new TransactionRejectedException("not enough gas to start the transaction, expected at least " + minimum + " units of gas");
	}

	/**
	 * Checks that the gas of the request is between zero and the maximum in the configuration of the node.
	 * 
	 * @throws TransactionRejectedException if the gas is outside these bounds
	 */
	private void gasLimitIsInsideBounds(BigInteger maxGasAllowedForTransaction) throws TransactionRejectedException {
		if (request.getGasLimit().compareTo(ZERO) < 0)
			throw new TransactionRejectedException("the gas limit cannot be negative");
		else if (request.getGasLimit().compareTo(maxGasAllowedForTransaction) > 0)
			throw new TransactionRejectedException("the gas limit of the request is larger than the maximum allowed (" + request.getGasLimit() + " > " + maxGasAllowedForTransaction + ")");
	}

	/**
	 * Checks that the gas price of the request is at least as large as the current gas price of the node.
	 * 
	 * @throws TransactionRejectedException if the gas price is smaller than the current gas price of the node
	 */
	private void gasPriceIsLargeEnough() throws TransactionRejectedException {
		// before initialization, the gas price is not yet available
		try {
			if (transactionIsSigned() && storeTransaction.nodeIsInitializedUncommitted() && !consensus.ignoresGasPrice()) {
				BigInteger currentGasPrice = node.caches.getGasPrice().get();
				if (request.getGasPrice().compareTo(currentGasPrice) < 0)
					throw new TransactionRejectedException("the gas price of the request is smaller than the current gas price (" + request.getGasPrice() + " < " + currentGasPrice + ")");
			}
		}
		catch (StoreException e) {
			LOGGER.log(Level.SEVERE, "", e);
		}
	}

	/**
	 * Checks if the payer of the request has enough funds for paying for all gas promised
	 * (green and red coins together).
	 * 
	 * @throws TransactionRejectedException if the payer is not rich enough for that
	 */
	private void payerCanPayForAllPromisedGas() throws TransactionRejectedException {
		BigInteger cost = costOf(request.getGasLimit());
		BigInteger totalBalance = storeTransaction.getTotalBalanceUncommitted(request.getCaller());

		if (totalBalance.subtract(cost).signum() < 0)
			throw new TransactionRejectedException("the payer has not enough funds to buy " + request.getGasLimit() + " units of gas");
	}

	/**
	 * Computes the cost of the given units of gas.
	 * 
	 * @param gas the units of gas
	 * @return the cost, as {@code gas} times {@code gasPrice}
	 */
	private BigInteger costOf(BigInteger gas) {
		return gas.multiply(request.getGasPrice());
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
		 * The deserialized validators contract, if the node is already initialized.
		 */
		private Optional<Object> deserializedValidators;

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

		/**
		 * The green balance of the payer with all promised gas paid.
		 * This will be the green balance if the transaction fails.
		 */
		private BigInteger greenBalanceOfPayerInCaseOfTransactionException;

		/**
		 * The red balance of the payer with all promised gas paid.
		 * This will be the red balance if the transaction fails.
		 */
		private BigInteger redBalanceOfPayerInCaseOfTransactionException;

		protected ResponseCreator() throws TransactionRejectedException {
			try {
				this.gas = request.getGasLimit();
			}
			catch (Throwable t) {
				LOGGER.log(Level.WARNING, "response creation rejected", t);
				throw wrapAsTransactionRejectedException(t);
			}
		}

		protected final void init() throws NodeException {
			this.deserializedCaller = deserializer.deserialize(request.getCaller());
			this.deserializedPayer = deserializedPayer();

			try {
				this.deserializedValidators = storeTransaction.getValidatorsUncommitted().map(deserializer::deserialize);
			}
			catch (StoreException e) {
				throw new NodeException(e);
			}

			increaseNonceOfCaller();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorage(BigInteger.valueOf(request.size()));
			chargeGasForClassLoader();	
			this.greenInitiallyPaidForGas = chargePayerForAllGasPromised();
			this.greenBalanceOfPayerInCaseOfTransactionException = classLoader.getBalanceOf(deserializedPayer);
			this.redBalanceOfPayerInCaseOfTransactionException = classLoader.getRedBalanceOf(deserializedPayer);
		}

		/**
		 * Yields the contract that pays for the transaction.
		 * This normally coincides with {@link #getDeserializedCaller()}
		 * but subclasses may redefine.
		 * 
		 * @return the payer for the transaction
		 */
		protected Object deserializedPayer() {
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
		 * Yields the contract that collects the validators of the node.
		 * After each transaction that consumes gas, the price of the gas is sent to this
		 * contract, that can later redistribute the reward to all validators.
		 * 
		 * @return the contract, inside the store of the node, if the node is already initialized
		 */
		protected final Optional<Object> getDeserializedValidators() {
			return deserializedValidators;
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
			return request.getGasLimit().subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
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

			// gas can only be negative if it was initialized so; this special case is
			// used for the creation of the gamete, when gas should not be counted
			if (gas.signum() < 0)
				return;

			if (gas.compareTo(amount) < 0)
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
			chargeGasForStorage(BigInteger.valueOf(response.size()));
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
		}

		/**
		 * Collects all updates to the balance or nonce of the caller of the transaction.
		 * 
		 * @return the updates
		 */
		protected final Stream<Update> updatesToBalanceOrNonceOfCaller() {
			return updatesExtractor.extractUpdatesFrom(Stream.of(deserializedCaller))
				.filter(this::isUpdateToBalanceOrNonceOfCaller);
		}

		/**
		 * Determines if the given update affects the balance or the nonce of the caller
		 * of the transaction. Those are the only updates that are allowed during the execution of a view method.
		 * 
		 * @param update the update
		 * @return true if and only if that condition holds
		 */
		protected final boolean isUpdateToBalanceOrNonceOfCaller(Update update) {
			if (update instanceof UpdateOfField uof && update.getObject().equals(request.getCaller())) {
				FieldSignature field = uof.getField();
				return FieldSignatures.BALANCE_FIELD.equals(field) || FieldSignatures.RED_BALANCE_FIELD.equals(field) || FieldSignatures.EOA_NONCE_FIELD.equals(field);
			}

			return false;
		}

		/**
		 * Charge to the payer of the transaction all gas promised for the transaction.
		 * 
		 * @return the amount that has been subtracted from the green balance
		 */
		private BigInteger chargePayerForAllGasPromised() {
			BigInteger cost = costOf(request.getGasLimit());
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

		/**
		 * Pays back the remaining gas to the payer of the transaction.
		 */
		protected final void refundPayerForAllRemainingGas() {
			BigInteger refund = costOf(gas);
			BigInteger greenBalance = classLoader.getBalanceOf(deserializedPayer);

			// we pay back the green before
			if (refund.subtract(greenInitiallyPaidForGas).signum() <= 0)
				classLoader.setBalanceOf(deserializedPayer, greenBalance.add(refund));
			else {
				BigInteger redBalance = classLoader.getRedBalanceOf(deserializedPayer);
				classLoader.setBalanceOf(deserializedPayer, greenBalance.add(greenInitiallyPaidForGas));
				classLoader.setRedBalanceOf(deserializedPayer, redBalance.add(refund.subtract(greenInitiallyPaidForGas)));
			}
		}

		protected final void resetBalanceOfPayerToInitialValueMinusAllPromisedGas() {
			classLoader.setBalanceOf(deserializedPayer, greenBalanceOfPayerInCaseOfTransactionException);
			classLoader.setRedBalanceOf(deserializedPayer, redBalanceOfPayerInCaseOfTransactionException);
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
			if (!isView())
				classLoader.setNonceOf(deserializedCaller, request.getNonce().add(ONE));
		}
	}
}