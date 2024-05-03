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

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.internal.AbstractLocalNodeImpl;
import io.hotmoka.stores.EngineClassLoader;
import io.hotmoka.stores.StoreException;
import io.hotmoka.stores.StoreTransaction;

/**
 * The creator of a response for a transaction that creates a gamete.
 */
public class GameteCreationResponseBuilder extends AbstractInitialResponseBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * Creates the builder of a response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public GameteCreationResponseBuilder(TransactionReference reference, GameteCreationTransactionRequest request, StoreTransaction<?> transaction, AbstractLocalNodeImpl<?,?> node) throws TransactionRejectedException {
		super(reference, request, transaction, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws NodeException {
		try {
			return storeTransaction.getClassLoader(request.getClasspath(), consensus);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public GameteCreationTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected GameteCreationTransactionResponse body() {
				try {
					Object gamete = classLoader.getGamete().getDeclaredConstructor(String.class).newInstance(request.getPublicKey());
					classLoader.setBalanceOf(gamete, request.getInitialAmount());
					classLoader.setRedBalanceOf(gamete, request.getRedInitialAmount());
					return TransactionResponses.gameteCreation(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					throw new RuntimeException("Unexpected exception", e);
				}
			}
		}
		.create();
	}
}