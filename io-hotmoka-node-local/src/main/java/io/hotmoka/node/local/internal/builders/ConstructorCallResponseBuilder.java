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
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.node.NonWhiteListedCallException;
import io.hotmoka.node.OutOfGasException;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.DeserializationException;
import io.hotmoka.node.local.api.StoreException;

/**
 * The creator of a response for a transaction that executes a constructor of Takamaka code.
 */
public class ConstructorCallResponseBuilder extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {
	private final static Logger LOGGER = Logger.getLogger(ConstructorCallResponseBuilder.class.getName());

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

		private ResponseCreator() throws TransactionRejectedException, StoreException {
		}

		@Override
		protected ConstructorCallTransactionResponse body() throws TransactionRejectedException, StoreException {
			checkConsistency();

			try {
				init();
				var actuals = request.actuals().toArray(StorageValue[]::new);
				this.deserializedActuals = new Object[actuals.length];
				int pos = 0;
				for (StorageValue actual: actuals)
					deserializedActuals[pos++] = deserializer.deserialize(actual);
		
				Object[] deserializedActuals;
				Constructor<?> constructorJVM;
		
				try {
					// we first try to call the constructor with exactly the parameter types explicitly provided
					constructorJVM = getConstructor();
					deserializedActuals = this.deserializedActuals;
				}
				catch (NoSuchMethodException e) {
					// if not found, we try to add the trailing types that characterize the @FromContract constructors
					try {
						constructorJVM = getFromContractConstructor();
						deserializedActuals = addExtraActualsForFromContract();
					}
					catch (NoSuchMethodException ee) {
						throw e; // the message must be relative to the constructor as the user sees it
					}
				}
		
				ensureWhiteListingOf(constructorJVM, deserializedActuals);
		
				Object result;
				try {
					result = constructorJVM.newInstance(deserializedActuals);
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, constructorJVM)) {
						chargeGasForStorageOf(TransactionResponses.constructorCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), cause.getClass().getName(), getMessage(cause), where(cause)));
						refundPayerForAllRemainingGas();
						return TransactionResponses.constructorCallException(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), cause.getClass().getName(), getMessage(cause), where(cause));
					}
					else
						throw e;
				}
		
				chargeGasForStorageOf(TransactionResponses.constructorCallSuccessful
					((StorageReference) serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				return TransactionResponses.constructorCallSuccessful
					((StorageReference) serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (OutOfGasException | NonWhiteListedCallException | IllegalAssignmentToFieldInStorage | DeserializationException | ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException t) {
				LOGGER.warning(TransactionReferences.of(environment.getHasher().hash(getRequest())) + ": failed with message: \"" + t.getMessage() + "\"");
				return TransactionResponses.constructorCallFailed(updatesInCaseOfException(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), t.getClass().getName(), getMessage(t), where(t));
			}
			catch (InvocationTargetException e) {
				Throwable t = e.getCause();
				LOGGER.warning(TransactionReferences.of(environment.getHasher().hash(getRequest())) + ": failed with message: \"" + t.getMessage() + "\"");
				return TransactionResponses.constructorCallFailed(updatesInCaseOfException(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), t.getClass().getName(), getMessage(t), where(t));
			}
		}

		/**
		 * Resolves the constructor that must be called.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClass();
			ConstructorSignature constructor = request.getStaticTarget();

			return classLoader.resolveConstructor(constructor.getDefiningClass().getName(), argTypes)
					.orElseThrow(() -> new NoSuchMethodException(constructor.toString()));
		}

		/**
		 * Resolves the constructor that must be called, assuming that it is a {@code @@FromContract}.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getFromContractConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClassForFromContract();
			ConstructorSignature constructor = request.getStaticTarget();

			return classLoader.resolveConstructor(constructor.getDefiningClass().getName(), argTypes)
					.orElseThrow(() -> new NoSuchMethodException(constructor.toString()));
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
				.orElseThrow(() -> new NonWhiteListedCallException("Illegal call to non-white-listed constructor of " + request.getStaticTarget().getDefiningClass()));
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