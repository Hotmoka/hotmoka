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

package io.hotmoka.node.local.internal.transactions;

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
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 * @throws StoreException 
	 */
	public InstanceMethodCallResponseBuilder(TransactionReference reference, AbstractInstanceMethodCallTransactionRequest request, AbstractStoreTransactionImpl<?,?> storeTransaction) throws TransactionRejectedException, StoreException {
		super(reference, request, storeTransaction);

		// calls to @View methods are allowed to receive non-exported values
		if (transactionIsSigned()) 
			receiverIsExported();
	}

	private void receiverIsExported() throws TransactionRejectedException, StoreException {
		enforceExported(request.getReceiver());
	}

	@Override
	public MethodCallTransactionResponse getResponse() throws StoreException {
		return new ResponseCreator().create();
	}

	@Override
	protected boolean transactionIsSigned() {
		return super.transactionIsSigned() && !isCallToFaucet();
	}

	private boolean callerIsGameteOfTheNode() {
		try {
			return storeTransaction.getGamete().filter(request.getCaller()::equals).isPresent();
		}
		catch (StoreException e) {
			LOGGER.log(Level.SEVERE, "", e);
			return false;
		}
	}

	private boolean isCallToFaucet() {
		return consensus.allowsUnsignedFaucet() && request.getStaticTarget().getMethodName().startsWith("faucet")
			&& request.getStaticTarget().getDefiningClass().equals(StorageTypes.GAMETE) && request.getCaller().equals(request.getReceiver())
			&& callerIsGameteOfTheNode();
	}

	/**
	 * Resolves the method that must be called, assuming that it is annotated as {@code @@FromContract}.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws SecurityException if the method could not be accessed
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	private Method getFromContractMethod() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
		MethodSignature method = request.getStaticTarget();
		Class<?> returnType = method instanceof NonVoidMethodSignature nvms ? classLoader.loadClass(nvms.getReturnType()) : void.class;
		Class<?>[] argTypes = formalsAsClassForFromContract();
	
		return classLoader.resolveMethod(method.getDefiningClass().getName(), method.getMethodName(), argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
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

		private ResponseCreator() {
		}

		@Override
		protected Object deserializedPayer() {
			return getDeserializedCaller();
		}

		@Override
		protected MethodCallTransactionResponse body() {
			try {
				init();
				this.deserializedReceiver = deserializer.deserialize(request.getReceiver());
				this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);

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
						chargeGasForStorageOf(TransactionResponses.methodCallException(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						refundPayerForAllRemainingGas();
						return TransactionResponses.methodCallException(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
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
					chargeGasForStorageOf(TransactionResponses.methodCallSuccessful(serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					return TransactionResponses.methodCallSuccessful(serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				LOGGER.log(Level.INFO, "transaction failed: " + t.getMessage());
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();

				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller or validators
				return TransactionResponses.methodCallFailed(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
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

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}

		@Override
		protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
			super.scanPotentiallyAffectedObjects(consumer);

			// the receiver is accessible from environment of the caller
			consumer.accept(deserializedReceiver);
		}

		@Override
		protected void ensureWhiteListingOf(Method executable, Object[] actuals) throws ClassNotFoundException {
			super.ensureWhiteListingOf(executable, actuals);

			// we check the annotations on the receiver as well
			Optional<Method> model = classLoader.getWhiteListingWizard().whiteListingModelOf(executable);
			if (model.isPresent() && !Modifier.isStatic(executable.getModifiers()))
				checkWhiteListingProofObligations(model.get().getName(), deserializedReceiver, model.get().getAnnotations());
		}

		/**
		 * For system calls to the rewarding method of the validators.
		 */
		private void mintCoinsForRewardToValidators() {
			try {
				Optional<StorageReference> manifest = storeTransaction.getManifest();
				if (isSystemCall() && request.getStaticTarget().equals(MethodSignatures.VALIDATORS_REWARD) && manifest.isPresent() && request.getCaller().equals(manifest.get())) {
					Optional<StorageValue> firstArg = request.actuals().findFirst();
					if (firstArg.isPresent()) {
						StorageValue value = firstArg.get();
						if (value instanceof BigIntegerValue biv) {
							Object caller = getDeserializedCaller();
							classLoader.setBalanceOf(caller, classLoader.getBalanceOf(caller).add(biv.getValue()));
						}
					}
				}
			}
			catch (StoreException e) {
				LOGGER.log(Level.SEVERE, "cannot access the manifest", e);
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