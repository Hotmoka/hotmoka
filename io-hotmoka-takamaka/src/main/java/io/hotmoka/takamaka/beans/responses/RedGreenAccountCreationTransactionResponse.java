package io.hotmoka.takamaka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class RedGreenAccountCreationTransactionResponse extends NonInitialTransactionResponse implements TransactionResponseWithUpdates {

	/**
	 * The account created.
	 */
	public final StorageReference account;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param account the account created
	 */
	public RedGreenAccountCreationTransactionResponse(Stream<Update> updates, StorageReference account) {
		super(updates, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);

		this.account = account;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RedGreenAccountCreationTransactionResponse) {
			RedGreenAccountCreationTransactionResponse otherCast = (RedGreenAccountCreationTransactionResponse) other;
			return super.equals(other) && account.equals(otherCast.account);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return account.hashCode() ^ super.hashCode();
	}

	@Override
	public String toString() {
        return super.toString() + "\n  account: " + account;
	}

	/**
	 * Yields the outcome of the execution having this response.
	 * 
	 * @return the outcome
	 */
	public StorageReference getOutcome() {
		return account;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(EXPANSION_SELECTOR);
		// after the expansion selector, the qualified name of the class must follow
		context.oos.writeUTF(MintTransactionSuccessfulResponse.class.getName());
		super.into(context);
		account.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static RedGreenAccountCreationTransactionResponse from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, ois));
		return new RedGreenAccountCreationTransactionResponse(updates, StorageReference.from(ois));
	}
}