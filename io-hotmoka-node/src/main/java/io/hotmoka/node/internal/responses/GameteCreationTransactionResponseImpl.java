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
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;

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
	public GameteCreationTransactionResponseImpl(Stream<Update> updates, StorageReference gamete) {
		this.updates = updates.toArray(Update[]::new);
		this.gamete = gamete;
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

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the response
	 * @throws IOException if the response could not be unmarshalled
	 */
	public static GameteCreationTransactionResponseImpl from(UnmarshallingContext context) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		return new GameteCreationTransactionResponseImpl(updates, StorageValues.referenceWithoutSelectorFrom(context));
	}
}