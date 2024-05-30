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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.DeserializationException;
import io.hotmoka.node.local.api.StoreException;
import io.takamaka.code.constants.Constants;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code.
 */
public class StaticMethodCallResponseBuilder extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment used for computing the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public StaticMethodCallResponseBuilder(TransactionReference reference, StaticMethodCallTransactionRequest request, ExecutionEnvironment<?> environment) throws TransactionRejectedException, StoreException {
		super(reference, request, environment);
	}

	@Override
	public MethodCallTransactionResponse getResponse() throws StoreException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest>.ResponseCreator {

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		private ResponseCreator() {
		}

		@Override
		protected MethodCallTransactionResponse body() {
			try {
				init();
				this.deserializedActuals = CheckSupplier.check(StoreException.class, DeserializationException.class,
					() -> request.actuals().map(UncheckFunction.uncheck(deserializer::deserialize)).toArray(Object[]::new));

				Method methodJVM = getMethod();
				boolean isView = hasAnnotation(methodJVM, Constants.VIEW_NAME);
				validateCallee(methodJVM, isView);
				ensureWhiteListingOf(methodJVM, deserializedActuals);

				Object result;
				try {
					result = methodJVM.invoke(null, deserializedActuals); // no receiver
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
					chargeGasForStorageOf(TransactionResponses.methodCallSuccessful(serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					return TransactionResponses.methodCallSuccessful(serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller or validators
				try {
					return TransactionResponses.methodCallFailed(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
				}
				catch (UpdatesExtractionException | StoreException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}

		/**
		 * Checks that the called method respects the expected constraints.
		 * 
		 * @param methodJVM the method
		 * @param isView true if the method is annotated as view
		 * @throws NoSuchMethodException if the constraints are not satisfied
		 */
		private void validateCallee(Method methodJVM, boolean isView) throws NoSuchMethodException {
			if (!Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("cannot call an instance method");

			if (!isView && StaticMethodCallResponseBuilder.this.isView())
				throw new NoSuchMethodException("cannot call a method not annotated as @View");
		}
	}
}