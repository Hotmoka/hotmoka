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
import java.util.stream.Stream;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.NonWhiteListedCallException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownTypeException;
import io.hotmoka.node.api.UnmatchedTargetException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.api.StoreException;

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
	public ResponseCreation<ConstructorCallTransactionResponse> getResponseCreation() throws TransactionRejectedException, StoreException, InterruptedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse>.ResponseCreator {

		/**
		 * The deserialized actual arguments of the constructor.
		 */
		private Object[] deserializedActuals;

		private ResponseCreator() throws TransactionRejectedException, StoreException {}

		@Override
		protected ConstructorCallTransactionResponse body() throws TransactionRejectedException, StoreException {
			checkConsistency();

			try {
				init();
				deserializedActuals = deserializedActuals();

				Object[] deserializedActuals;
				Constructor<?> constructorJVM;
		
				try {
					// we first try to call the constructor with exactly the parameter types explicitly provided
					constructorJVM = getConstructor();
					deserializedActuals = this.deserializedActuals;
				}
				catch (UnmatchedTargetException e) {
					// if not found, we try to add the trailing types that characterize the @FromContract constructors
					constructorJVM = getFromContractConstructor();
					deserializedActuals = addExtraActualsForFromContract();
				}
		
				ensureWhiteListingOf(constructorJVM, deserializedActuals);
		
				Object result;
				try {
					result = constructorJVM.newInstance(deserializedActuals);
				}
				catch (InstantiationException | IllegalAccessException e) {
					throw new UnmatchedTargetException("Cannot instantiate class " + request.getStaticTarget().getDefiningClass());
				}
				catch (InvocationTargetException e) {
					return failure(constructorJVM, e);						
				}

				if (serialize(result) instanceof StorageReference sr)
					return success(result, sr);
				else
					// a constructor can only create an object, represented as a storage reference in Hotmoka
					throw new StoreException("The return value of a constructor should be an object");
			}
			catch (HotmokaException t) {
				return TransactionResponses.constructorCallFailed(updatesInCaseOfException(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), t.getClass().getName(), getMessage(t), where(t));
			}
		}

		private Object[] deserializedActuals() throws HotmokaException, StoreException {
			var actuals = request.actuals().toArray(StorageValue[]::new);
			Object[] deserializedActuals = new Object[actuals.length];
			int pos = 0;
			for (StorageValue actual: actuals)
				deserializedActuals[pos++] = deserializer.deserialize(actual);

			return deserializedActuals;
		}

		private ConstructorCallTransactionResponse success(Object result, StorageReference reference) throws HotmokaException, StoreException {
			chargeGasForStorageOf(TransactionResponses.constructorCallSuccessful
					(reference, updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
			refundCallerForAllRemainingGas();
			return TransactionResponses.constructorCallSuccessful
					(reference, updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
		}

		private ConstructorCallTransactionResponse failure(Constructor<?> constructorJVM, InvocationTargetException e) throws HotmokaException, StoreException {
			Throwable cause = e.getCause();
			String message = getMessage(cause);
			String causeClassName = cause.getClass().getName();
			String where = where(cause);

			if (isCheckedForThrowsExceptions(cause, constructorJVM)) {
				chargeGasForStorageOf(TransactionResponses.constructorCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), causeClassName, message, where));
				refundCallerForAllRemainingGas();
				return TransactionResponses.constructorCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), causeClassName, message, where);
			}
			else
				return TransactionResponses.constructorCallFailed(updatesInCaseOfException(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), causeClassName, message, where);
		}

		/**
		 * Resolves the constructor that must be called.
		 * 
		 * @return the constructor
		 * @throws UnmatchedTargetException if the constructor could not be found
		 * @throws UnknownTypeException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getConstructor() throws UnknownTypeException, UnmatchedTargetException {
			Class<?>[] argTypes = formalsAsClass2();
			ConstructorSignature constructor = request.getStaticTarget();

			try {
				return classLoader.resolveConstructor(constructor.getDefiningClass().getName(), argTypes)
					.orElseThrow(() -> new UnmatchedTargetException(constructor));
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(constructor.getDefiningClass());
			}
		}

		/**
		 * Resolves the constructor that must be called, assuming that it is a {@code @@FromContract}.
		 * 
		 * @return the constructor
		 * @throws UnmatchedTargetException if the constructor could not be found
		 * @throws UnknownTypeException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getFromContractConstructor() throws UnknownTypeException, UnmatchedTargetException {
			Class<?>[] argTypes = formalsAsClassForFromContract2();
			ConstructorSignature constructor = request.getStaticTarget();

			try {
				return classLoader.resolveConstructor(constructor.getDefiningClass().getName(), argTypes)
					.orElseThrow(() -> new UnmatchedTargetException(constructor));
			}
			catch (ClassNotFoundException e) {
				throw new UnknownTypeException(constructor.getDefiningClass());
			}
		}

		/**
		 * Adds to the actual parameters the implicit actuals that are passed
		 * to {@link io.takamaka.code.lang.FromContract} methods or constructors. They are the caller of
		 * the method or constructor and {@code null} for the dummy argument.
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

		/**
		 * Checks that the constructor called by this transaction is white-listed.
		 * 
		 * @param executable the constructor
		 * @param actuals the actual arguments passed to {@code executable}
		 * @throws NonWhiteListedCallException if {@code executable} is not white-listed
		 */
		private void ensureWhiteListingOf(Constructor<?> executable, Object[] actuals) throws NonWhiteListedCallException {
			classLoader.getWhiteListingWizard().whiteListingModelOf(executable)
				.orElseThrow(() -> new NonWhiteListedCallException(request.getStaticTarget()));
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}

		@Override
		protected final int gasForStoringFailedResponse() {
			BigInteger gas = request.getGasLimit();
		
			return TransactionResponses.constructorCallFailed
				(Stream.empty(), gas, gas, gas, gas, "placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where").size();
		}
	}
}