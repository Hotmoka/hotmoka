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

package io.hotmoka.node.local.internal.builders;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.OutOfGasError;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.local.DeserializationException;
import io.hotmoka.node.local.api.ClassLoaderCreationException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.StoreException;

/**
 * Implementation of the creator of the response for a non-initial transaction. Non-initial transactions consume gas,
 * have a payer a nonce and a chain identifier and are signed. The constructor of this class checks
 * the validity of all these elements.
 */
public abstract class NonInitialResponseBuilderImpl<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {

	/**
	 * Creates a the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	protected NonInitialResponseBuilderImpl(TransactionReference reference, Request request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws StoreException, ClassLoaderCreationException {
		return environment.getClassLoader(request.getClasspath(), consensus);
	}

	/**
	 * Determines if the transaction is a view transaction, that is, it has no side-effect on the store.
	 * 
	 * @return true if and only if the transaction is a view transaction
	 */
	protected boolean isView() {
		return false; // subclasses may redefine
	}

	protected abstract class ResponseCreator extends AbstractResponseBuilder<Request, Response>.ResponseCreator {

		/**
		 * The cost model of the node for which the transaction is being built.
		 */
		protected final GasCostModel gasCostModel;

		/**
		 * The deserialized caller.
		 */
		private Object deserializedCaller;

		/**
		 * The deserialized validators contract, if the node is already initialized.
		 */
		private Optional<Object> deserializedValidators;

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
		 * The amount of coins that have been deduced at the beginning for paying the gas in full.
		 */
		private BigInteger coinsInitiallyPaidForGas;

		/**
		 * The balance of the payer with all promised gas paid.
		 * This will be its balance if the transaction fails.
		 */
		private BigInteger balanceOfPayerInCaseOfTransactionException;

		protected ResponseCreator() throws TransactionRejectedException, StoreException {
			this.gas = request.getGasLimit();
			this.gasCostModel = consensus.getGasCostModel();
		}

		protected void checkConsistency() throws TransactionRejectedException {
			try {
				callerMustBeExternallyOwnedAccount();
				gasLimitIsInsideBounds();
				requestPromisesEnoughGas();
				gasPriceIsLargeEnough();
				requestMustHaveCorrectChainId();
				signatureMustBeValid();
				callerAndRequestMustAgreeOnNonce();
				callerCanPayForAllPromisedGas();
			}
			catch (StoreException e) {
				throw new RuntimeException(e);
			}
		}

		protected final void init() throws StoreException, DeserializationException {
			this.deserializedCaller = deserializer.deserialize(request.getCaller());
			var validators = environment.getValidators();
			this.deserializedValidators = validators.isPresent() ? Optional.of(deserializer.deserialize(validators.get())) : Optional.empty();
			increaseNonceOfCaller();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorage(BigInteger.valueOf(request.size()));
			chargeGasForClassLoader();	
			this.coinsInitiallyPaidForGas = chargePayerForAllGasPromised();
			this.balanceOfPayerInCaseOfTransactionException = classLoader.getBalanceOf(deserializedCaller, StoreException::new);
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
		 * Determines if the transaction is signed.
		 * 
		 * @return true if and only if the request is signed and the transaction is not a view transaction
		 */
		protected boolean transactionIsSigned() {
			return !isView() && request instanceof SignedTransactionRequest;
		}

		/**
		 * Checks if the caller is an externally owned account or subclass.
		 *
		 * @throws TransactionRejectedException if the caller is not an externally owned account
		 */
		private void callerMustBeExternallyOwnedAccount() throws TransactionRejectedException, StoreException {
			String className;
		
			try {
				className = environment.getClassName(request.getCaller());
			}
			catch (UnknownReferenceException e) {
				throw new TransactionRejectedException("The caller " + request.getCaller() + " cannot be found in store", consensus);
			}
		
			try {
				Class<?> clazz = classLoader.loadClass(className);
				if (!classLoader.getExternallyOwnedAccount().isAssignableFrom(clazz))
					throw new TransactionRejectedException("The caller of a request must be an externally owned account", consensus);
			}
			catch (ClassNotFoundException e) {
				throw new TransactionRejectedException("The class " + className + " of the caller cannot be resolved", consensus);
			}
		}

		/**
		 * Checks that the gas of the request is between zero and the maximum in the configuration of the node.
		 * 
		 * @throws TransactionRejectedException if the gas is outside these bounds
		 */
		private void gasLimitIsInsideBounds() throws TransactionRejectedException {
			if (request.getGasLimit().compareTo(ZERO) < 0)
				throw new TransactionRejectedException("The gas limit cannot be negative", consensus);
			else if (request.getGasLimit().compareTo(consensus.getMaxGasPerTransaction()) > 0)
				throw new TransactionRejectedException("The gas limit of the request is larger than the maximum allowed (" + request.getGasLimit() + " > " + consensus.getMaxGasPerTransaction() + ")", consensus);
		}

		/**
		 * Checks that the request provides a minimal threshold of gas for starting the transaction.
		 * 
		 * @throws TransactionRejectedException if the request provides too little gas
		 */
		private void requestPromisesEnoughGas() throws TransactionRejectedException {
			BigInteger minimum = minimalGasRequiredForTransaction();
			if (request.getGasLimit().compareTo(minimum) < 0)
				throw new TransactionRejectedException("not enough gas to start the transaction, expected at least " + minimum + " units of gas", consensus);
		}

		/**
		 * Checks that the gas price of the request is at least as large as the current gas price of the node.
		 * 
		 * @throws TransactionRejectedException if the gas price is smaller than the current gas price of the node
		 */
		private void gasPriceIsLargeEnough() throws TransactionRejectedException {
			if (transactionIsSigned() && !consensus.ignoresGasPrice()) {
				Optional<BigInteger> maybeGasPrice = environment.getGasPrice();
				// before initialization, the gas price is not yet available
				if (maybeGasPrice.isPresent() && request.getGasPrice().compareTo(maybeGasPrice.get()) < 0)
					throw new TransactionRejectedException("The gas price of the request is smaller than the current gas price (" + request.getGasPrice() + " < " + maybeGasPrice.get() + ")", consensus);
			}
		}

		/**
		 * Checks if the node has the same chain identifier as the request.
		 * 
		 * @throws TransactionRejectedException if the node and the request have different chain identifiers
		 */
		private void requestMustHaveCorrectChainId() throws TransactionRejectedException, StoreException {
			// the chain identifier is not checked for unsigned transactions or if the node is not initialized yet
			if (transactionIsSigned() && environment.getManifest().isPresent()) {
				String chainIdOfNode = consensus.getChainId();
				String chainId = ((SignedTransactionRequest<?>) request).getChainId();
				if (!chainIdOfNode.equals(chainId))
					throw new TransactionRejectedException("Incorrect chain id: the request reports " + chainId + " but the node requires " + chainIdOfNode, consensus);
			}
		}

		/**
		 * Checks that the request is signed with the private key of its caller.
		 * 
		 * @throws NodeException if the signature of the request could not be checked
		 */
		private void signatureMustBeValid() throws TransactionRejectedException, StoreException {
			// if the node is not initialized yet, the signature is not checked
			if (transactionIsSigned() && environment.getManifest().isPresent()) {
				try {
					if (!environment.signatureIsValid((SignedTransactionRequest<?>) request, determineSignatureAlgorithm()))
						throw new TransactionRejectedException("Invalid request signature", consensus);
				}
				catch (UnknownReferenceException | FieldNotFoundException e) {
					// we have already verified that the caller exists and is an externally owned account:
					// hence these exceptions now can only mean that the store is corrupted
					throw new StoreException(e);
				}
			}
		}

		/**
		 * Checks if the caller has the same nonce as the request.
		 * 
		 * @throws TransactionRejectedException if the nonce of the caller is not equal to that in {@code request}
		 */
		private void callerAndRequestMustAgreeOnNonce() throws TransactionRejectedException, StoreException {
			// calls to @View methods do not check the nonce
			if (!isView()) {
				BigInteger expected = environment.getNonce(request.getCaller());
				if (!expected.equals(request.getNonce()))
					throw new TransactionRejectedException("Incorrect nonce: the request reports " + request.getNonce()
						+ " but the account " + request.getCaller() + " contains " + expected, consensus);
			}
		}

		/**
		 * Checks if the payer of the request has enough funds for paying for all gas promised
		 * (green and red coins together).
		 * 
		 * @throws TransactionRejectedException if the payer is not rich enough for that
		 */
		private void callerCanPayForAllPromisedGas() throws TransactionRejectedException, StoreException {
			try {
				BigInteger cost = costOf(request.getGasLimit());
				BigInteger totalBalance = environment.getBalance(request.getCaller());
		
				if (totalBalance.subtract(cost).signum() < 0)
					throw new TransactionRejectedException("The payer has not enough funds to buy " + request.getGasLimit() + " units of gas", consensus);
			}
			catch (UnknownReferenceException | FieldNotFoundException e) {
				// we have verified that the caller was an account, so this can only be a store corruption problem
				throw new StoreException(e);
			}
		}

		/**
		 * Determine the signature algorithm that must have been used for signing the request.
		 * This depends on the run-time class of the caller of the request.
		 * 
		 * @return the signature algorithm
		 * @throws NodeException 
		 */
		private SignatureAlgorithm determineSignatureAlgorithm() throws StoreException, TransactionRejectedException {
			try {
				Class<?> clazz = classLoader.loadClass(environment.getClassName(request.getCaller()));
		
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
			catch (UnknownReferenceException e) {
				throw new TransactionRejectedException("The caller " + request.getCaller() + " is not an object in store", consensus);
			}
			catch (NoSuchAlgorithmException e) {
				throw new StoreException(e);
			}
			catch (ClassNotFoundException e) {
				throw new TransactionRejectedException(e, consensus);
			}
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
		 * @throws DeserializationException 
		 */
		protected final Stream<Update> updatesToBalanceOrNonceOfCaller() throws UpdatesExtractionException, StoreException {
			return updatesExtractor.extractUpdatesFrom(List.of(deserializedCaller))
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
				return FieldSignatures.BALANCE_FIELD.equals(field) || FieldSignatures.EOA_NONCE_FIELD.equals(field);
			}

			return false;
		}

		/**
		 * Charge to the payer of the transaction all gas promised for the transaction.
		 * 
		 * @return the amount that has been subtracted from the balance
		 * @throws StoreException 
		 */
		private BigInteger chargePayerForAllGasPromised() throws StoreException {
			BigInteger cost = costOf(request.getGasLimit());
			BigInteger balance = classLoader.getBalanceOf(deserializedCaller, StoreException::new);
			classLoader.setBalanceOf(deserializedCaller, balance.subtract(cost), StoreException::new);

			return cost;
		}

		/**
		 * Pays back the remaining gas to the payer of the transaction.
		 * @throws StoreException 
		 */
		protected final void refundPayerForAllRemainingGas() throws StoreException {
			BigInteger refund = costOf(gas);
			BigInteger balance = classLoader.getBalanceOf(deserializedCaller, StoreException::new);

			if (refund.subtract(coinsInitiallyPaidForGas).signum() <= 0)
				classLoader.setBalanceOf(deserializedCaller, balance.add(refund), StoreException::new);
			else
				classLoader.setBalanceOf(deserializedCaller, balance.add(coinsInitiallyPaidForGas), StoreException::new);
		}

		protected final void resetBalanceOfPayerToInitialValueMinusAllPromisedGas() throws StoreException {
			classLoader.setBalanceOf(deserializedCaller, balanceOfPayerInCaseOfTransactionException, StoreException::new);
		}

		/**
		 * Sets the nonce to the value successive to that in the request.
		 * 
		 * @throws StoreException if the satore is misbehaving
		 */
		private void increaseNonceOfCaller() throws StoreException {
			if (!isView())
				classLoader.setNonceOf(deserializedCaller, request.getNonce().add(ONE), StoreException::new);
		}
	}
}