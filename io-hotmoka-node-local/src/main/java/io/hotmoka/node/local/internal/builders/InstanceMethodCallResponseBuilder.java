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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.api.StoreException;
import io.takamaka.code.constants.Constants;

/**
 * The builder of the response of a transaction that executes an instance method of Takamaka code.
 */
public class InstanceMethodCallResponseBuilder extends MethodCallResponseBuilder<AbstractInstanceMethodCallTransactionRequest> {
	private final static Logger LOGGER = Logger.getLogger(InstanceMethodCallResponseBuilder.class.getName());

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public InstanceMethodCallResponseBuilder(TransactionReference reference, AbstractInstanceMethodCallTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	public ResponseCreation<MethodCallTransactionResponse> getResponseCreation() throws TransactionRejectedException, StoreException, InterruptedException {
		return new ResponseCreator().create();
	}

	private boolean callerIsGameteOfTheNode() {
		try {
			return environment.getGamete().filter(request.getCaller()::equals).isPresent();
		}
		catch (StoreException e) {
			LOGGER.log(Level.SEVERE, "", e);
			return false;
		}
	}

	private boolean isCallToFaucet() {
		return consensus.allowsUnsignedFaucet() && request.getStaticTarget().getName().startsWith("faucet")
			&& request.getStaticTarget().getDefiningClass().equals(StorageTypes.GAMETE) && request.getCaller().equals(request.getReceiver())
			&& callerIsGameteOfTheNode();
	}

	private class ResponseCreator extends MethodCallResponseBuilder<InstanceMethodCallTransactionRequest>.ResponseCreator {

		/**
		 * The deserialized receiver the call.
		 */
		private Object deserializedReceiver;

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		private ResponseCreator() throws TransactionRejectedException, StoreException {
		}

		@Override
		protected void checkConsistency() throws TransactionRejectedException {
			super.checkConsistency();

			try {
				// calls to @View methods are allowed to receive non-exported values
				if (transactionIsSigned()) 
					receiverIsExported();
			}
			catch (StoreException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected MethodCallTransactionResponse body() throws TransactionRejectedException, StoreException {
			checkConsistency();

			try {
				init();
				this.deserializedReceiver = deserializer.deserialize(request.getReceiver());
				var actuals = request.actuals().toArray(StorageValue[]::new);
				this.deserializedActuals = new Object[actuals.length];
				for (int pos = 0; pos < actuals.length; pos++)
					deserializedActuals[pos] = deserializer.deserialize(actuals[pos]);

				Object[] deserializedActuals;
				Method methodJVM;

				try {
					// we first try to call the method with exactly the parameter types explicitly provided
					methodJVM = getMethod();
					deserializedActuals = this.deserializedActuals;
				}
				catch (NoSuchMethodException e) {
					// if not found, we try to add the trailing types that characterize the @Entry methods
					try {
						methodJVM = getFromContractMethod();
						deserializedActuals = addExtraActualsForFromContract();
					}
					catch (NoSuchMethodException ee) {
						throw e; // the message must be relative to the method as the user sees it
					}
				}

				boolean isView = hasAnnotation(methodJVM, Constants.VIEW_NAME);
				validateCallee(methodJVM, isView);
				ensureWhiteListingOf(methodJVM, deserializedActuals);
				mintCoinsForRewardToValidators();

				Object result;
				try {
					result = methodJVM.invoke(deserializedReceiver, deserializedActuals);
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, methodJVM)) {
						viewMustBeSatisfied(isView, null);
						chargeGasForStorageOf(TransactionResponses.methodCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), cause.getClass().getName(), getMessage(cause), where(cause)));
						refundPayerForAllRemainingGas();
						return TransactionResponses.methodCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), cause.getClass().getName(), getMessage(cause), where(cause));
					}
					else
						throw cause;
				}

				viewMustBeSatisfied(isView, result);

				if (methodJVM.getReturnType() == void.class) {
					chargeGasForStorageOf(TransactionResponses.voidMethodCallSuccessful(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					return TransactionResponses.voidMethodCallSuccessful(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else {
					chargeGasForStorageOf(TransactionResponses.nonVoidMethodCallSuccessful(serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					return TransactionResponses.nonVoidMethodCallSuccessful(serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				var reference = TransactionReferences.of(environment.getHasher().hash(getRequest()));
				LOGGER.warning(reference + ": failed with message: \"" + t.getMessage() + "\"");
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();

				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller or validators
				try {
					return TransactionResponses.methodCallFailed(updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), t.getClass().getName(), getMessage(t), where(t));
				}
				catch (UpdatesExtractionException | StoreException e) {
					throw new RuntimeException(e); // TODO
				}
			}
		}

		/**
		 * Checks that the called method respects the expected constraints.
		 * 
		 * @param methodJVM the method
		 * @param isView true if the method is annotated as view
		 * @throws NoSuchMethodException if the constraints are not satisfied
		 */
		private void validateCallee(Method methodJVM, boolean isView) throws NoSuchMethodException {
			if (Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("cannot call a static method");

			if (!isView && InstanceMethodCallResponseBuilder.this.isView())
				throw new NoSuchMethodException("cannot call a method not annotated as @View");
		}

		/**
		 * Resolves the method that must be called, assuming that it is annotated as {@code @@FromContract}.
		 * 
		 * @return the method
		 * @throws NoSuchMethodException if the method could not be found
		 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
		 */
		private Method getFromContractMethod() throws NoSuchMethodException, ClassNotFoundException {
			MethodSignature method = request.getStaticTarget();
			Class<?> returnType = method instanceof NonVoidMethodSignature nvms ? classLoader.loadClass(nvms.getReturnType()) : void.class;
			Class<?>[] argTypes = formalsAsClassForFromContract();
		
			return classLoader.resolveMethod(method.getDefiningClass().getName(), method.getName(), argTypes, returnType)
				.orElseThrow(() -> new NoSuchMethodException(method.toString()));
		}

		private void receiverIsExported() throws TransactionRejectedException, StoreException {
			enforceExported(request.getReceiver());
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}

		@Override
		protected boolean transactionIsSigned() {
			return super.transactionIsSigned() && !isCallToFaucet();
		}

		@Override
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			super.scanPotentiallyAffectedObjects(consumer);

			// the receiver is accessible from environment of the caller
			consumer.accept(deserializedReceiver);
		}

		/**
		 * For system calls to the rewarding methods of the validators.
		 */
		private void mintCoinsForRewardToValidators() throws StoreException {
			Optional<StorageReference> manifest;
			if (isSystemCall()) {
				var staticTarget = request.getStaticTarget();
				if ((staticTarget.equals(MethodSignatures.VALIDATORS_REWARD) || staticTarget.equals(MethodSignatures.VALIDATORS_REWARD_MOKAMINT_NODE) || staticTarget.equals(MethodSignatures.VALIDATORS_REWARD_MOKAMINT_MINER))
						&& (manifest = environment.getManifest()).isPresent() && request.getCaller().equals(manifest.get())) {

					Optional<StorageValue> firstArg = request.actuals().findFirst();
					if (firstArg.isPresent() && firstArg.get() instanceof BigIntegerValue biv) {
						Object caller = getDeserializedCaller();
						classLoader.setBalanceOf(caller, classLoader.getBalanceOf(caller, StoreException::new).add(biv.getValue()), StoreException::new);
					}
				}
			}
		}

		/**
		 * Adds to the actual parameters the implicit actuals that are passed
		 * to {@link io.takamaka.code.lang.FromContract} methods or constructors. They are the caller of
		 * the entry and {@code null} for the dummy argument.
		 * 
		 * @return the resulting actual parameters
		 */
		private Object[] addExtraActualsForFromContract() {
			int al = deserializedActuals.length;
			var result = new Object[al + 2];
			System.arraycopy(deserializedActuals, 0, result, 0, al);
			result[al] = getDeserializedCaller();
			result[al + 1] = null; // Dummy is not used

			return result;
		}
	}
}