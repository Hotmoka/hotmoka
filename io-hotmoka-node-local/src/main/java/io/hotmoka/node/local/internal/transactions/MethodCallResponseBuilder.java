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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.api.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.NonWhiteListedCallException;
import io.hotmoka.node.SideEffectsInViewMethodException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.local.internal.NodeInternal;

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
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected MethodCallResponseBuilder(TransactionReference reference, Request request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected final int gasForStoringFailedResponse() {
		BigInteger gas = request.getGasLimit();

		return new MethodCallTransactionFailedResponse
			("placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where",
			false, Stream.empty(), gas, gas, gas, gas).size();
	}

	/**
	 * Resolves the method that must be called.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	protected final Method getMethod() throws ClassNotFoundException, NoSuchMethodException {
		MethodSignature method = request.method;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).getReturnType()) : void.class;
		Class<?>[] argTypes = formalsAsClass();
	
		return classLoader.resolveMethod(method.getDefiningClass().getName(), method.getMethodName(), argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	protected abstract class ResponseCreator extends CodeCallResponseBuilder<Request, MethodCallTransactionResponse>.ResponseCreator {

		protected ResponseCreator() throws TransactionRejectedException {
		}

		/**
		 * Checks that the view annotation, if any, is satisfied.
		 * 
		 * @param isView true if and only if the method is annotated as view
		 * @param result the returned value of the method, if any
		 * @throws SideEffectsInViewMethodException if the method is annotated as view, but generated side-effects
		 */
		protected final void viewMustBeSatisfied(boolean isView, Object result) throws SideEffectsInViewMethodException {
			if (isView && !onlyAffectedBalanceOrNonceOfCallerOrBalanceOfValidators(result))
				throw new SideEffectsInViewMethodException(request.method);
		}

		/**
		 * Checks that the method called by this transaction
		 * is white-listed and its white-listing proof-obligations hold.
		 * 
		 * @param executable the method
		 * @param actuals the actual arguments passed to {@code executable}, including the receiver for instance methods
		 * @throws ClassNotFoundException if some class could not be found during the check
		 */
		protected void ensureWhiteListingOf(Method executable, Object[] actuals) throws ClassNotFoundException {
			Optional<Method> model = classLoader.getWhiteListingWizard().whiteListingModelOf(executable);
			if (model.isEmpty())
				throw new NonWhiteListedCallException("illegal call to non-white-listed method " + request.method.getDefiningClass() + "." + request.method.getMethodName());

			Annotation[][] anns = model.get().getParameterAnnotations();
			String methodName = model.get().getName();

			for (int pos = 0; pos < actuals.length; pos++)
				checkWhiteListingProofObligations(methodName, actuals[pos], anns[pos]);
		}

		/**
		 * Determines if the execution only affected the balance or nonce of the caller contract or
		 * the balance of the validators contract.
		 * 
		 * @param result the returned value for method calls or created object for constructor calls, if any
		 * @return true if and only if that condition holds
		 */
		private boolean onlyAffectedBalanceOrNonceOfCallerOrBalanceOfValidators(Object result) {
			return updates(result).allMatch(this::isUpdateToBalanceOrNonceOfCaller);
		}
	}
}