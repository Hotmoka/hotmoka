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

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.DeserializationException;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownTypeException;
import io.hotmoka.node.api.UnmatchedTargetException;
import io.hotmoka.node.api.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.StoreException;
import io.takamaka.code.constants.Constants;

/**
 * The builder of the response of a transaction that executes an instance method of Takamaka code.
 */
public class InstanceMethodCallResponseBuilder extends MethodCallResponseBuilder<AbstractInstanceMethodCallTransactionRequest> {

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
	public ResponseCreation<MethodCallTransactionResponse> getResponseCreation() throws TransactionRejectedException, InterruptedException {
		return new ResponseCreator().create();
	}

	private boolean callerIsGameteOfTheNode() {
		Optional<StorageReference> maybeGamete = environment.getGamete();
		return maybeGamete.isPresent() && maybeGamete.get().equals(request.getCaller());
	}

	private class ResponseCreator extends MethodCallResponseBuilder<InstanceMethodCallTransactionRequest>.ResponseCreator {

		/**
		 * The deserialized receiver the call.
		 */
		private Object deserializedReceiver;

		private ResponseCreator() throws TransactionRejectedException {}

		@Override
		protected void checkConsistency() throws TransactionRejectedException {
			super.checkConsistency();

			// calls to @View methods are allowed to receive non-exported values
			if (transactionIsSigned()) 
				receiverIsExported();
		}

		@Override
		protected MethodCallTransactionResponse body() throws TransactionRejectedException {
			checkConsistency();

			try {
				init();
				deserializeReceiver();
				deserializeActuals();

				Object[] deserializedActuals;
				Method methodJVM;

				try {
					// we first try to call the method with exactly the parameter types explicitly provided
					methodJVM = getMethod();
					deserializedActuals = getDeserializedActuals();
				}
				catch (UnmatchedTargetException e) {
					// if not found, we try to add the trailing types that characterize the @FromContract methods
					methodJVM = getFromContractMethod();
					deserializedActuals = getDeserializedActualsForFromContract();
				}

				boolean calleeIsAnnotatedAsView = hasAnnotation(methodJVM, Constants.VIEW_NAME);
				calleeIsConsistent(methodJVM, calleeIsAnnotatedAsView);
				ensureWhiteListingOf(methodJVM, deserializedActuals);
				mintCoinsForRewardToValidators();

				Object result;
				try {
					// deserializedReceiver is always non-null since it is the deserialization of a storage reference
					result = methodJVM.invoke(deserializedReceiver, deserializedActuals);
				}
				catch (InvocationTargetException e) {
					return failure(methodJVM, calleeIsAnnotatedAsView, e);
				}
				catch (IllegalArgumentException e) {
					throw new UnmatchedTargetException("Illegal argument passed to " + request.getStaticTarget());
				}
				catch (IllegalAccessException e) {
					throw new UnmatchedTargetException("Cannot access " + request.getStaticTarget());
				}
				catch (ExceptionInInitializerError e) {
					// Takamaka code verification bans static initializers and the white-listed library classes
					// should not have static initializers that might fail
					throw new StoreException("Unexpected failed execution of a static initializer of " + request.getStaticTarget());
				}

				if (calleeIsAnnotatedAsView)
					onlySideEffectsAreToBalanceAndNonceOfCaller(result);

				return success(methodJVM, result);
			}
			catch (HotmokaException e) {
				logFailure(Level.INFO, e);
				return TransactionResponses.methodCallFailed(updatesInCaseOfFailure(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), e.getClass().getName(), getMessageForResponse(e), where(e));
			}
		}

		private void deserializeReceiver() throws DeserializationException {
			deserializedReceiver = deserializer.deserialize(request.getReceiver());
		}

		@Override
		protected boolean isCallToFaucet() {
			return consensus.allowsUnsignedFaucet() && request.getStaticTarget().getName().startsWith("faucet")
				&& request.getStaticTarget().getDefiningClass().equals(StorageTypes.GAMETE) && request.getCaller().equals(request.getReceiver())
				&& callerIsGameteOfTheNode();
		}

		/**
		 * Checks that the callee is not static and that
		 * a view request actually executes a method annotated as {@code @@View}.
		 * 
		 * @param methodJVM the method
		 * @param calleeIsAnnotatedAsView true if the callee is annotated as {@code @@View}
		 * @throws UnmatchedTargetException if that condition is not satisfied
		 */
		private void calleeIsConsistent(Method methodJVM, boolean calleeIsAnnotatedAsView) throws UnmatchedTargetException {
			if (Modifier.isStatic(methodJVM.getModifiers()))
				throw new UnmatchedTargetException("Cannot call a static method");

			if (!calleeIsAnnotatedAsView && isView())
				throw new UnmatchedTargetException("Cannot call a method not annotated as @View");
		}

		/**
		 * Resolves the method that must be called, assuming that it is annotated as {@code @@FromContract}.
		 * 
		 * @return the method
		 * @throws UnmatchedTargetException if the method could not be found
		 * @throws UnknownTypeException if the class of the method or of some parameter or return type cannot be found
		 */
		private Method getFromContractMethod() throws UnmatchedTargetException, UnknownTypeException {
			MethodSignature method = request.getStaticTarget();
			Class<?>[] argTypes = formalsAsClassForFromContract();
			Class<?> returnType;

			if (method instanceof NonVoidMethodSignature nvms) {
				try {
					returnType = classLoader.loadClass(nvms.getReturnType());
				}
				catch (ClassNotFoundException e) {
					throw new UnknownTypeException(nvms.getReturnType());
				}
			}
			else
				returnType = void.class;

			try {
				return classLoader.resolveMethod(method.getDefiningClass().getName(), method.getName(), argTypes, returnType)
						.orElseThrow(() -> new UnmatchedTargetException("Cannot find method " + method.toString()));
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(method.getDefiningClass());
			}
		}

		private void receiverIsExported() throws TransactionRejectedException {
			enforceExported(request.getReceiver());
		}

		@Override
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			super.scanPotentiallyAffectedObjects(consumer);
			consumer.accept(deserializedReceiver);
		}

		/**
		 * Charges the caller of system calls for rewarding the validators, with the amount
		 * of coins that must be rewarded. That is, it creates "out of thin-air" as many coins
		 * as must be distributed to the validators.
		 */
		private void mintCoinsForRewardToValidators() {
			if (isSystemCall()) {
				var staticTarget = request.getStaticTarget();
				Optional<StorageReference> manifest;

				if ((staticTarget.equals(MethodSignatures.VALIDATORS_REWARD) || staticTarget.equals(MethodSignatures.VALIDATORS_REWARD_MOKAMINT_NODE)
						|| staticTarget.equals(MethodSignatures.VALIDATORS_REWARD_MOKAMINT_MINER))
						&& (manifest = environment.getManifest()).isPresent() && request.getCaller().equals(manifest.get())) {

					// the rewarded coins are passed as first argument
					var actuals = request.actuals().toArray(StorageValue[]::new);
					if (actuals.length >= 1 && actuals[0] instanceof BigIntegerValue biv) {
						Object caller = getDeserializedCaller();
						classLoader.setBalanceOf(caller, classLoader.getBalanceOf(caller).add(biv.getValue()));
					}
				}
			}
		}
	}
}