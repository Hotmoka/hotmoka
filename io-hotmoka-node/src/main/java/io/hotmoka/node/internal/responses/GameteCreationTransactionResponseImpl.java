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

package io.hotmoka.node.internal.responses;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.TransactionResponseJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * A response for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class GameteCreationTransactionResponseImpl extends TransactionResponseImpl implements GameteCreationTransactionResponse {
	final static byte SELECTOR = 0;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The created gamete.
	 */
	private final StorageReference gamete;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gamete the created gamete
	 */
	public <E extends Exception> GameteCreationTransactionResponseImpl(Stream<Update> updates, StorageReference gamete, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this(updates.toArray(Update[]::new), gamete, onIllegalArgs);
	}

	/**
	 * Unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public GameteCreationTransactionResponseImpl(UnmarshallingContext context) throws IOException {
		this(context.readLengthAndArray(Updates::from, Update[]::new), StorageReferenceImpl.fromWithoutSelector(context), IOException::new);
	}

	/**
	 * Creates a response from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public GameteCreationTransactionResponseImpl(TransactionResponseJson json) throws InconsistentJsonException {
		this(unmapUpdates(json), unmapIntoStorageReference(json.getGamete()), InconsistentJsonException::new);
	}

	/**
	 * Builds the transaction response.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gamete the created gamete
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> GameteCreationTransactionResponseImpl(Update[] updates, StorageReference gamete, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.updates = updates;
	
		for (var update: updates)
			Objects.requireNonNull(update, "updates cannot hold null elements", onIllegalArgs);
	
		this.gamete = Objects.requireNonNull(gamete, "gamete cannot be null", onIllegalArgs);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GameteCreationTransactionResponse gctr &&
			Arrays.equals(updates, gctr.getUpdates().toArray(Update[]::new)) &&
			gamete.equals(gctr.getGamete());
	}

	@Override
	public int hashCode() {
		return gamete.hashCode() ^ Arrays.hashCode(updates);
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public StorageReference getGamete() {
		return gamete;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  gamete: " + gamete + "\n"
       		+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	/**
	 * Yields the outcome of the execution having this response.
	 * 
	 * @return the outcome
	 */
	public StorageReference getOutcome() {
		return gamete;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeLengthAndArray(updates);
		gamete.intoWithoutSelector(context);
	}
}