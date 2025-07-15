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
import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.api.NonWhiteListedCallException;
import io.hotmoka.node.api.SideEffectsInViewMethodException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownTypeException;
import io.hotmoka.node.api.requests.MethodCallTransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * The creator of a response for a transaction that executes a method of Takamaka code.
 * 
 * @param <Request> the type of the request of the transaction
 */
public abstract class MethodCallResponseBuilder<Request extends MethodCallTransactionRequest> extends CodeCallResponseBuilder<Request, MethodCallTransactionResponse> {

	/**
	 * Builds the creator of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	protected MethodCallResponseBuilder(TransactionReference reference, Request request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	protected abstract class ResponseCreator extends CodeCallResponseBuilder<Request, MethodCallTransactionResponse>.ResponseCreator {

		protected ResponseCreator() throws TransactionRejectedException {}

		/**
		 * Checks that the view annotation, if any, is satisfied.
		 * 
		 * @param result the returned value of the method, if any
		 * @throws SideEffectsInViewMethodException if the method is annotated as view, but generated side-effects
		 */
		protected final void onlySideEffectsAreToBalanceAndNonceOfCaller(Object result) throws SideEffectsInViewMethodException, IllegalAssignmentToFieldInStorageException {
			if (!onlyAffectedBalanceOrNonceOfCaller(result))
				throw new SideEffectsInViewMethodException(request.getStaticTarget());
		}

		/**
		 * Checks that the method called by this transaction is white-listed.
		 * 
		 * @param executable the method
		 * @param actuals the actual arguments passed to {@code executable}, including the receiver for instance methods
		 * @throws NonWhiteListedCallException if {@code executable} is not white-listed
		 */
		protected final void ensureWhiteListingOf(Method executable, Object[] actuals) throws NonWhiteListedCallException {
			classLoader.getWhiteListingWizard().whiteListingModelOf(executable)
				.orElseThrow(() -> new NonWhiteListedCallException(request.getStaticTarget()));
		}

		protected final Optional<Method> getMethod() throws UnknownTypeException {
			MethodSignature method = request.getStaticTarget();
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

			Class<?>[] argTypes = formalsAsClass();

			try {
				return classLoader.resolveMethod(method.getDefiningClass().getName(), method.getName(), argTypes, returnType);
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(method.getDefiningClass());
			}
		}

		protected final MethodCallTransactionResponse success(Method methodJVM, Object result) throws HotmokaException {
			if (methodJVM.getReturnType() == void.class) {
				chargeGasForStorageOf(TransactionResponses.voidMethodCallSuccessful(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundCallerForAllRemainingGas();
				return TransactionResponses.voidMethodCallSuccessful(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			else {
				chargeGasForStorageOf(TransactionResponses.nonVoidMethodCallSuccessful(serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundCallerForAllRemainingGas();
				return TransactionResponses.nonVoidMethodCallSuccessful(serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
		}

		protected final MethodCallTransactionResponse failure(Method methodJVM, boolean calleeIsAnnotatedAsView, InvocationTargetException e) throws HotmokaException {
			Throwable cause = e.getCause();
			String message = getMessageForResponse(cause);
			String causeClassName = cause.getClass().getName();
			String where = where(cause);

			if (isCheckedForThrowsExceptions(cause, methodJVM)) {
				if (calleeIsAnnotatedAsView)
					onlySideEffectsAreToBalanceAndNonceOfCaller(calleeIsAnnotatedAsView);

				chargeGasForStorageOf(TransactionResponses.methodCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), causeClassName, message, where));
				refundCallerForAllRemainingGas();
				return TransactionResponses.methodCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), causeClassName, message, where);
			}
			else {
				logFailure(Level.INFO, cause);
				return TransactionResponses.methodCallFailed(updatesInCaseOfFailure(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), causeClassName, message, where);
			}
		}

		@Override
		protected final int gasForStoringFailedResponse() {
			BigInteger gas = request.getGasLimit();
		
			return TransactionResponses.methodCallFailed
				(Stream.empty(), gas, gas, gas, gas, "placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where").size();
		}

		/**
		 * Determines if the execution only affected the balance or nonce of the caller contract.
		 * 
		 * @param result the returned value for method calls or created object for constructor calls, if any
		 * @return true if and only if that condition holds
		 */
		private boolean onlyAffectedBalanceOrNonceOfCaller(Object result) throws IllegalAssignmentToFieldInStorageException {
			return updates(result).allMatch(this::isUpdateToBalanceOrNonceOfCaller);
		}
	}
}