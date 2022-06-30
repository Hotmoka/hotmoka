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

package io.hotmoka.local.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.constants.Constants;
import io.hotmoka.local.ViewResponseBuilder;
import io.hotmoka.local.internal.NodeInternal;

/**
 * The builder of the response of a transaction that executes an instance method of Takamaka code.
 */
public class InstanceMethodCallResponseBuilder extends MethodCallResponseBuilder<AbstractInstanceMethodCallTransactionRequest> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public InstanceMethodCallResponseBuilder(TransactionReference reference, AbstractInstanceMethodCallTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);

		try {
			// calls to @View methods are allowed to receive non-exported values
			if (transactionIsSigned()) 
				receiverIsExported();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	private void receiverIsExported() throws TransactionRejectedException {
		enforceExported(request.receiver);
	}

	@Override
	public MethodCallTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator().create();
	}

	@Override
	protected StorageReference getPayerFromRequest() {
		// calls to instance methods might be self charged, in which case the receiver is paying
		return isSelfCharged() ? request.receiver : request.caller;
	}

	@Override
	protected boolean transactionIsSigned() {
		return super.transactionIsSigned() && !isCallToFaucet();
	}

	@Override
	protected boolean ignoreGasPrice() {
		return super.ignoreGasPrice() || 
			// the add method of the accounts ledger can be called by the gamete with any gas, if the consensus allows it
			(consensus.allowsMintBurnFromGamete && isSpecialMethodThatGameteCanCallWithZeroGasPrice() && callerIsGameteOfTheNode());
	}

	private boolean isSpecialMethodThatGameteCanCallWithZeroGasPrice() {
		MethodSignature method = request.method;
		return CodeSignature.ADD_INTO_ACCOUNTS_LEDGER.equals(method) ||
			CodeSignature.EOA_MINT.equals(method) ||
			CodeSignature.EOA_BURN.equals(method);
	}

	private boolean callerIsGameteOfTheNode() {
		return node.getCaches().getGamete().filter(request.caller::equals).isPresent();
	}

	private boolean isCallToFaucet() {
		return consensus.allowsUnsignedFaucet && request.method.methodName.startsWith("faucet")
			&& request.method.definingClass.equals(ClassType.GAMETE) && request.caller.equals(request.receiver)
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
		MethodSignature method = request.method;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClassForFromContract();
	
		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	/**
	 * Determines if the target method exists and is annotated as @SelfCharged.
	 * 
	 * @return true if and only if that condition holds
	 */
	private boolean isSelfCharged() {
		// consensus might be null during the recomputation of the same consensus at the restart of a node
		if (consensus != null && consensus.allowsSelfCharged)
			try {
				try {
					// we first try to call the method with exactly the parameter types explicitly provided
					return hasAnnotation(getMethod(), Constants.SELF_CHARGED_NAME);
				}
				catch (NoSuchMethodException e) {
					// if not found, we try to add the trailing types that characterize the @Entry methods
					return hasAnnotation(getFromContractMethod(), Constants.SELF_CHARGED_NAME);
				}
			}
			catch (Throwable t) {
				// the method does not exist: ok to ignore, since this exception will be dealt with in body()
			}

		return false;
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

		private ResponseCreator() throws TransactionRejectedException {
		}

		@Override
		protected Object deserializedPayer() {
			// self charged methods use the receiver of the call as payer
			return isSelfCharged() ? deserializer.deserialize(request.receiver) : getDeserializedCaller();
		}

		@Override
		protected MethodCallTransactionResponse body() {
			try {
				init();
				this.deserializedReceiver = deserializer.deserialize(request.receiver);
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
						chargeGasForStorageOf(new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), isSelfCharged(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						refundPayerForAllRemainingGas();
						return new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), isSelfCharged(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else
						throw cause;
				}

				viewMustBeSatisfied(isView, result);

				if (methodJVM.getReturnType() == void.class) {
					chargeGasForStorageOf(new VoidMethodCallTransactionSuccessfulResponse(isSelfCharged(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					return new VoidMethodCallTransactionSuccessfulResponse(isSelfCharged(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else {
					chargeGasForStorageOf(new MethodCallTransactionSuccessfulResponse(serializer.serialize(result), isSelfCharged(), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					return new MethodCallTransactionSuccessfulResponse(serializer.serialize(result), isSelfCharged(), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				logger.log(Level.INFO, "transaction failed", t);
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();

				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller or validators
				return new MethodCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), isSelfCharged(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
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

			if (!isView && InstanceMethodCallResponseBuilder.this instanceof ViewResponseBuilder)
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
			Optional<StorageReference> manifest = node.getStoreUtilities().getManifestUncommitted();
			if (isSystemCall() && request.method.equals(CodeSignature.VALIDATORS_REWARD) && manifest.isPresent() && request.caller.equals(manifest.get())) {
				Optional<StorageValue> firstArg = request.actuals().findFirst();
				if (firstArg.isPresent()) {
					StorageValue value = firstArg.get();
					if (value instanceof BigIntegerValue) {
						BigInteger amount = ((BigIntegerValue) value).value;
						Object caller = getDeserializedCaller();
						classLoader.setBalanceOf(caller, classLoader.getBalanceOf(caller).add(amount));
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
			Object[] result = new Object[al + 2];
			System.arraycopy(deserializedActuals, 0, result, 0, al);
			result[al] = getDeserializedCaller();
			result[al + 1] = null; // Dummy is not used

			return result;
		}
	}
}