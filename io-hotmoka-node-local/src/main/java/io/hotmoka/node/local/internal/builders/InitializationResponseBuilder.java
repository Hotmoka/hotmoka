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

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.api.ClassLoaderCreationException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.takamaka.code.constants.Constants;

/**
 * The creator of a response for a transaction that initializes a node.
 */
public class InitializationResponseBuilder extends AbstractInitialResponseBuilder<InitializationTransactionRequest, InitializationTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public InitializationResponseBuilder(TransactionReference reference, InitializationTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	public ResponseCreation<InitializationTransactionResponse> getResponseCreation() throws TransactionRejectedException, InterruptedException {
		return new ResponseCreator() {

			@Override
			protected InitializationTransactionResponse body() throws TransactionRejectedException {
				checkConsistency();
				return TransactionResponses.initialization();	
			}

			@Override
			protected final void checkBeforeExecution() throws TransactionRejectedException {
				super.checkBeforeExecution();
				manifestMustExistAndBeOfManifestClass();
			}

			private void manifestMustExistAndBeOfManifestClass() throws TransactionRejectedException {
				String className;
			
				try {
					className = environment.getClassName(request.getManifest());
				}
				catch (UnknownReferenceException e) {
					throw new TransactionRejectedException("The manifest " + request.getManifest() + " cannot be found in store");
				}
			
				try {
					Class<?> clazz = classLoader.loadClass(className);
					if (!classLoader.getManifest().isAssignableFrom(clazz))
						throw new TransactionRejectedException("The manifest of an initialization request must be of type " + Constants.MANIFEST_NAME);
				}
				catch (ClassNotFoundException e) {
					throw new TransactionRejectedException("The class " + className + " of the manifest cannot be resolved");
				}
			}
		}
		.create();
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws ClassLoaderCreationException {
		return environment.getClassLoader(request.getClasspath(), consensus); // currently not used for this transaction
	}
}