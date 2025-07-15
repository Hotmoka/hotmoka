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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.HotmokaTransactionException;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.NonWhiteListedCallException;
import io.hotmoka.node.local.UnknownTypeException;
import io.hotmoka.node.local.UnmatchedTargetException;

/**
 * The creator of a response for a transaction that executes a constructor of Takamaka code.
 */
public class ConstructorCallResponseBuilder extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public ConstructorCallResponseBuilder(TransactionReference reference, ConstructorCallTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	public ResponseCreation<ConstructorCallTransactionResponse> getResponseCreation() throws TransactionRejectedException, InterruptedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse>.ResponseCreator {

		private ResponseCreator() throws TransactionRejectedException {}

		@Override
		protected ConstructorCallTransactionResponse body() throws TransactionRejectedException {
			checkConsistency();

			try {
				init();
				deserializeActuals();

				Object[] deserializedActuals;
				Constructor<?> constructorJVM;

				// we first try to call the constructor with exactly the parameter types explicitly provided
				var maybeConstructor = getConstructor();
				if (maybeConstructor.isPresent()) {
					constructorJVM = maybeConstructor.get();
					deserializedActuals = getDeserializedActuals();
				}
				else {
					// if not found, we try to add the trailing types that characterize the @FromContract constructors
					maybeConstructor = getFromContractConstructor();
					if (maybeConstructor.isPresent()) {
						constructorJVM = maybeConstructor.get();
						deserializedActuals = getDeserializedActualsForFromContract();
					}
					else
						throw new UnmatchedTargetException(request.getStaticTarget());
				}
		
				ensureWhiteListingOf(constructorJVM, deserializedActuals);
		
				Object result;
				try {
					result = constructorJVM.newInstance(deserializedActuals);
				}
				catch (InvocationTargetException e) {
					return failure(constructorJVM, e);						
				}
				catch (IllegalArgumentException e) {
					throw new UnmatchedTargetException("Illegal argument passed to " + request.getStaticTarget());
				}
				catch (InstantiationException e) {
					throw new UnmatchedTargetException("Cannot instantiate class " + request.getStaticTarget().getDefiningClass());
				}
				catch (IllegalAccessException e) {
					throw new UnmatchedTargetException("Cannot access " + request.getStaticTarget());
				}
				catch (ExceptionInInitializerError e) {
					// Takamaka code verification bans static initializers and the white-listed library classes
					// should not have static initializers that might fail
					throw new LocalNodeException("Unexpected failed execution of a static initializer");
				}

				if (serialize(result) instanceof StorageReference sr)
					return success(result, sr);
				else
					// a constructor can only create an object, represented as a storage reference in Hotmoka
					throw new LocalNodeException("The return value of a constructor should be an object");
			}
			catch (HotmokaTransactionException e) {
				logFailure(Level.INFO, e);
				return TransactionResponses.constructorCallFailed(updatesInCaseOfFailure(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), e.getClass().getName(), getMessageForResponse(e), where(e));
			}
		}

		private ConstructorCallTransactionResponse success(Object result, StorageReference reference) {
			chargeGasForStorageOf(TransactionResponses.constructorCallSuccessful
					(reference, updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
			refundCallerForAllRemainingGas();
			return TransactionResponses.constructorCallSuccessful
					(reference, updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
		}

		private ConstructorCallTransactionResponse failure(Constructor<?> constructorJVM, InvocationTargetException e) {
			Throwable cause = e.getCause();
			String message = getMessageForResponse(cause);
			String causeClassName = cause.getClass().getName();
			String where = where(cause);

			if (isCheckedForThrowsExceptions(cause, constructorJVM)) {
				chargeGasForStorageOf(TransactionResponses.constructorCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), causeClassName, message, where));
				refundCallerForAllRemainingGas();
				return TransactionResponses.constructorCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), causeClassName, message, where);
			}
			else if (cause instanceof HotmokaTransactionException he)
				throw he;
			else {
				logFailure(Level.INFO, cause);
				return TransactionResponses.constructorCallFailed(updatesInCaseOfFailure(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), causeClassName, message, where);
			}
		}

		/**
		 * Resolves the constructor that must be called.
		 * 
		 * @return the constructor, if it could be found
		 */
		private Optional<Constructor<?>> getConstructor() {
			Class<?>[] argTypes = formalsAsClass();
			ConstructorSignature constructor = request.getStaticTarget();

			try {
				return classLoader.resolveConstructor(constructor.getDefiningClass().getName(), argTypes);
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(constructor.getDefiningClass());
			}
		}

		/**
		 * Resolves the constructor that must be called, assuming that it is a {@code @@FromContract}.
		 * 
		 * @return the constructor, if it could be found
		 */
		private Optional<Constructor<?>> getFromContractConstructor() {
			Class<?>[] argTypes = formalsAsClassForFromContract();
			ConstructorSignature constructor = request.getStaticTarget();

			try {
				return classLoader.resolveConstructor(constructor.getDefiningClass().getName(), argTypes);
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(constructor.getDefiningClass());
			}
		}

		/**
		 * Checks that the constructor called by this transaction is white-listed.
		 * 
		 * @param executable the constructor
		 * @param actuals the actual arguments passed to {@code executable}
		 * @throws NonWhiteListedCallException if {@code executable} is not white-listed
		 */
		private void ensureWhiteListingOf(Constructor<?> executable, Object[] actuals) {
			classLoader.getWhiteListingWizard().whiteListingModelOf(executable)
				.orElseThrow(() -> new NonWhiteListedCallException(request.getStaticTarget()));
		}

		@Override
		protected final int gasForStoringFailedResponse() {
			BigInteger gas = request.getGasLimit();
		
			return TransactionResponses.constructorCallFailed
				(Stream.empty(), gas, gas, gas, gas, "placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where").size();
		}
	}
}