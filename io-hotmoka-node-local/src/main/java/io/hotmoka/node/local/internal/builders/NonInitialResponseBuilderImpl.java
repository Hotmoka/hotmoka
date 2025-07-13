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
import java.util.ArrayList;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.ClassLoaderCreationException;
import io.hotmoka.node.api.DeserializationException;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.api.OutOfGasException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.UncheckedStoreException;
import io.hotmoka.whitelisting.api.WhiteListingClassLoader;

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
	protected EngineClassLoader mkClassLoader() throws ClassLoaderCreationException {
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
		 * The set of updates if the transaction fails. They are the update to the nonce
		 * and to the balance of the caller, if any.
		 */
		private final SortedSet<Update> updatesInCaseOfFailure = new TreeSet<>();

		/**
		 * The amount of coins that have been deduced at the beginning for paying the gas in full.
		 */
		private BigInteger coinsInitiallyPaidForGas;

		protected ResponseCreator() throws TransactionRejectedException {
			this.gas = request.getGasLimit();
			this.gasCostModel = consensus.getGasCostModel();
		}

		protected void checkConsistency() throws TransactionRejectedException {
			callerMustBeExternallyOwnedAccount();
			gasLimitIsInsideBounds();
			requestPromisesEnoughGas();
			gasPriceIsLargeEnough();
			requestMustHaveCorrectChainId();
			signatureMustBeValid();
			callerAndRequestMustAgreeOnNonce();
			callerCanPayForAllPromisedGas();
		}

		protected final void init() throws DeserializationException, OutOfGasException {
			this.deserializedCaller = deserializer.deserialize(request.getCaller());
			BigInteger initialBalance = classLoader.getBalanceOf(deserializedCaller);
			increaseNonceOfCaller();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorage(BigInteger.valueOf(request.size()));
			chargeGasForClassLoader();	
			this.coinsInitiallyPaidForGas = chargePayerForAllGasPromised();
			BigInteger balanceOfCallerInCaseOfFailure = classLoader.getBalanceOf(deserializedCaller);
			if (!balanceOfCallerInCaseOfFailure.equals(initialBalance))
				updatesInCaseOfFailure.add(Updates.ofBigInteger(request.getCaller(), FieldSignatures.BALANCE_FIELD, balanceOfCallerInCaseOfFailure));
		}

		/**
		 * Yields a safe message for an exception thrown during the execution of a Hotmoka transaction.
		 * This message will be included in a transaction failed response and included in the store of
		 * the node, therefore we do not want to store messages that might machine-dependent, or otherwise
		 * consensus might be lost. The idea is that we only trust exceptions not coming from the Java runtime,
		 * since the latter might contain non-deterministic messages (for instance, the address of a module or object)
		 * 
		 * @param throwable the exception
		 * @return the safe message of {@code throwable}
		 */
		protected final String getMessageForResponse(Throwable throwable) {
			var clazz = throwable.getClass();
			return HotmokaException.class.isAssignableFrom(clazz) || clazz.getClassLoader() instanceof WhiteListingClassLoader ? throwable.getMessage() : "";
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
			return !isView() && !isCallToFaucet() && request instanceof SignedTransactionRequest;
		}

		/**
		 * Determines if this is a call to the unsigned faucet of the node.
		 * This always returns false if the network has no open unsigned faucet.
		 * 
		 * @return true if and only if the faucet is open and this is a call to the faucet
		 */
		protected boolean isCallToFaucet() {
			return false;
		}

		/**
		 * Collects all updates that can be seen from the context of the caller of the method or constructor.
		 * 
		 * @return the updates, sorted
		 */
		protected final Stream<Update> updates() throws IllegalAssignmentToFieldInStorageException {
			var potentiallyAffectedObjects = new ArrayList<Object>();
			scanPotentiallyAffectedObjects(potentiallyAffectedObjects::add);
			return updatesExtractor.extractUpdatesFrom(potentiallyAffectedObjects);
		}

		/**
		 * Scans the objects reachable from the context of the caller of the transaction
		 * that might have been affected during the execution of the transaction
		 * and consumes each of them. Such objects do not include the returned value of
		 * a method or the object created by a constructor, if any.
		 * 
		 * @param consumer the consumer
		 */
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			consumer.accept(getDeserializedCaller());
		}

		/**
		 * Checks if the caller is an externally owned account or subclass.
		 *
		 * @throws TransactionRejectedException if the caller is not an externally owned account
		 */
		private void callerMustBeExternallyOwnedAccount() throws TransactionRejectedException {
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
		private void requestMustHaveCorrectChainId() throws TransactionRejectedException {
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
		 * @throws TransactionRejectedException if the signature of the request could not be checked
		 */
		private void signatureMustBeValid() throws TransactionRejectedException {
			// if the node is not initialized yet, the signature is not checked
			if (transactionIsSigned() && environment.getManifest().isPresent()) {
				try {
					if (!environment.signatureIsValid((SignedTransactionRequest<?>) request, determineSignatureAlgorithm()))
						throw new TransactionRejectedException("Invalid request signature", consensus);
				}
				catch (UnknownReferenceException | FieldNotFoundException e) {
					// we have already verified that the caller exists and is an externally owned account:
					// hence these exceptions can only mean that the store is corrupted
					throw new UncheckedStoreException(e);
				}
			}
		}

		/**
		 * Checks if the caller has the same nonce as the request.
		 * 
		 * @throws TransactionRejectedException if the nonce of the caller is not equal to that in {@code request}
		 */
		private void callerAndRequestMustAgreeOnNonce() throws TransactionRejectedException {
			// calls to @View methods do not check the nonce
			if (!isView() && !isCallToFaucet()) {
				BigInteger expected;

				try {
					expected = environment.getNonce(request.getCaller());
				}
				catch (UnknownReferenceException | FieldNotFoundException e) {
					// we have already checked that the caller is an account, hence this should not happen
					throw new UncheckedStoreException(e);
				}

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
		private void callerCanPayForAllPromisedGas() throws TransactionRejectedException {
			BigInteger cost = costOf(request.getGasLimit());
			BigInteger totalBalance;

			try {
				totalBalance = environment.getBalance(request.getCaller());
			}
			catch (UnknownReferenceException | FieldNotFoundException e) {
				// we already checked that the caller is an account, therefore this should not happen
				throw new UncheckedStoreException(e);
			}
		
			if (totalBalance.subtract(cost).signum() < 0)
				throw new TransactionRejectedException("The payer has not enough funds to buy " + request.getGasLimit() + " units of gas", consensus);
		}

		/**
		 * Determine the signature algorithm that must have been used for signing the request.
		 * This depends on the run-time class of the caller of the request.
		 * 
		 * @return the signature algorithm
		 * @throws TransactionRejectedException if the signature for signing the request cannot be determined 
		 */
		private SignatureAlgorithm determineSignatureAlgorithm() throws TransactionRejectedException {
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
				// this is a limit of the Java installation, is not the fault of the user of Hotmoka
				throw new UncheckedStoreException(e);
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
		 * @throws OutOfGasException 
		 */
		private void charge(BigInteger amount, Consumer<BigInteger> forWhat) throws OutOfGasException {
			if (amount.signum() < 0)
				throw new IllegalArgumentException("The gas cannot increase"); // TODO

			// gas can only be negative if it was initialized so; this special case is
			// used for the creation of the gamete, when gas should not be counted
			if (gas.signum() < 0) // TODO: check
				return;

			if (gas.compareTo(amount) < 0)
				throw new OutOfGasException("Not enough gas to complete the operation");
		
			gas = gas.subtract(amount);
			forWhat.accept(amount);
		}

		/**
		 * Decreases the available gas by the given amount, for storage allocation.
		 * 
		 * @param amount the amount of gas to consume
		 * @throws OutOfGasException 
		 */
		private void chargeGasForStorage(BigInteger amount) throws OutOfGasException {
			charge(amount, x -> gasConsumedForStorage = gasConsumedForStorage.add(x));
		}

		/**
		 * Decreases the available gas for the given response, for storage allocation.
		 * 
		 * @param response the response
		 * @throws OutOfGasException 
		 */
		protected final void chargeGasForStorageOf(Response response) throws OutOfGasException {
			chargeGasForStorage(BigInteger.valueOf(response.size()));
		}

		@Override
		public final void chargeGasForCPU(BigInteger amount) throws OutOfGasException {
			charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
		}

		@Override
		public final void chargeGasForRAM(BigInteger amount) throws OutOfGasException {
			charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
		}

		/**
		 * Charges gas proportional to the complexity of the class loader that has been created.
		 */
		protected final void chargeGasForClassLoader() throws OutOfGasException {
			int[] lengthsOfJars = classLoader.getLengthsOfJars().toArray();
			for (int length: lengthsOfJars) {
				chargeGasForCPU(gasCostModel.cpuCostForLoadingJar(length));
				chargeGasForRAM(gasCostModel.ramCostForLoadingJar(length));
			}
		}

		/**
		 * Collects the updates to the balance or nonce of the caller of the transaction,
		 * if the latter fails.
		 * 
		 * @return the updates
		 */
		protected final Stream<Update> updatesInCaseOfFailure() {
			return updatesInCaseOfFailure.stream();
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
		 */
		private BigInteger chargePayerForAllGasPromised() {
			BigInteger cost = costOf(request.getGasLimit());
			BigInteger balance = classLoader.getBalanceOf(deserializedCaller);
			classLoader.setBalanceOf(deserializedCaller, balance.subtract(cost));

			return cost;
		}

		/**
		 * Pays back the remaining gas to the caller of the transaction.
		 */
		protected final void refundCallerForAllRemainingGas() {
			BigInteger refund = costOf(gas);
			BigInteger balance = classLoader.getBalanceOf(deserializedCaller);

			if (refund.subtract(coinsInitiallyPaidForGas).signum() <= 0)
				classLoader.setBalanceOf(deserializedCaller, balance.add(refund));
			else
				classLoader.setBalanceOf(deserializedCaller, balance.add(coinsInitiallyPaidForGas));
		}

		/**
		 * Sets the nonce to the value successive to that in the request.
		 */
		private void increaseNonceOfCaller() {
			if (!isView() && !isCallToFaucet()) {
				BigInteger increasedNonce = request.getNonce().add(ONE);
				classLoader.setNonceOf(deserializedCaller, increasedNonce);
				updatesInCaseOfFailure.add(Updates.ofBigInteger(request.getCaller(), FieldSignatures.EOA_NONCE_FIELD, increasedNonce));
			}
		}
	}
}