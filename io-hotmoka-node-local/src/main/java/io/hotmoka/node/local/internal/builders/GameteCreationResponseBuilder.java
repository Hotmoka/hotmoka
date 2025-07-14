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
import java.util.List;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.ClassLoaderCreationException;
import io.hotmoka.node.api.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.api.EngineClassLoader;

/**
 * The creator of a response for a transaction that creates a gamete.
 */
public class GameteCreationResponseBuilder extends AbstractInitialResponseBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * Creates the builder of a response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public GameteCreationResponseBuilder(TransactionReference reference, GameteCreationTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws ClassLoaderCreationException {
		return environment.getClassLoader(request.getClasspath(), consensus);
	}

	@Override
	public ResponseCreation<GameteCreationTransactionResponse> getResponseCreation() throws TransactionRejectedException, InterruptedException {
		return new ResponseCreator() {

			@Override
			protected GameteCreationTransactionResponse body() throws TransactionRejectedException {
				checkConsistency();

				try {
					Object gamete = classLoader.getGamete().getDeclaredConstructor(String.class).newInstance(request.getPublicKey());
					classLoader.setBalanceOf(gamete, request.getInitialAmount());
					return TransactionResponses.gameteCreation(updatesExtractor.extractUpdatesFrom(List.of(gamete)), classLoader.getStorageReferenceOf(gamete));
				}
				catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | IllegalAssignmentToFieldInStorageException e) {
					throw new LocalNodeException("Could not call the constructor of the gamete", e);
				}
			}
		}
		.create();
	}
}