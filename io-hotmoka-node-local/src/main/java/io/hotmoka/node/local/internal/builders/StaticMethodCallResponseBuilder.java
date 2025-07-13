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
import java.util.logging.Level;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnmatchedTargetException;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.UncheckedStoreException;
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
	 * @param environment the execution environment where the response is built
	 */
	public StaticMethodCallResponseBuilder(TransactionReference reference, StaticMethodCallTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	public ResponseCreation<MethodCallTransactionResponse> getResponseCreation() throws TransactionRejectedException, InterruptedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest>.ResponseCreator {

		private ResponseCreator() throws TransactionRejectedException {}

		@Override
		protected MethodCallTransactionResponse body() throws TransactionRejectedException {
			checkConsistency();

			try {
				init();
				deserializeActuals();

				Method methodJVM = getMethod();
				boolean calleeIsAnnotatedAsView = hasAnnotation(methodJVM, Constants.VIEW_NAME);
				calleeIsConsistent(methodJVM, calleeIsAnnotatedAsView);
				ensureWhiteListingOf(methodJVM, getDeserializedActuals());

				Object result;
				try {
					result = methodJVM.invoke(null, getDeserializedActuals()); // no receiver
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
					throw new UncheckedStoreException("Unexpected failed execution of a static initializer of " + request.getStaticTarget());
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

		/**
		 * Checks that the callee is static and that
		 * a view request actually executes a method annotated as {@code @@View}.
		 * 
		 * @param methodJVM the method
		 * @param calleeIsAnnotatedAsView true if the callee is annotated as {@code @@View}
		 * @throws UnmatchedTargetException if that condition is not satisfied
		 */
		private void calleeIsConsistent(Method methodJVM, boolean calleeIsAnnotatedAsView) throws UnmatchedTargetException {
			if (!Modifier.isStatic(methodJVM.getModifiers()))
				throw new UnmatchedTargetException("Cannot call an instance method");

			if (!calleeIsAnnotatedAsView && isView())
				throw new UnmatchedTargetException("Cannot call a method not annotated as @View");
		}
	}
}