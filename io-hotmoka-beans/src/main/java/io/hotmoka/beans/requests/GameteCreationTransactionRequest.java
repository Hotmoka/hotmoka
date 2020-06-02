package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;

/**
 * A request for creating an initial gamete.
 */
@Immutable
public class GameteCreationTransactionRequest extends InitialTransactionRequest<GameteCreationTransactionResponse> {
	final static byte SELECTOR = 0;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(JarStoreInitialTransactionRequest)}.
	 */
	public final TransactionReference classpath;

	/**
	 * The amount of coin provided to the gamete.
	 */

	public final BigInteger initialAmount;

	/**
	 * The Base64-encoded public key that will be assigned to the gamete.
	 */
	public final String publicKey;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(JarStoreInitialTransactionRequest)}
	 * @param initialAmount the amount of coin provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public GameteCreationTransactionRequest(TransactionReference classpath, BigInteger initialAmount, String publicKey) {
		this.classpath = classpath;
		this.initialAmount = initialAmount;
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  initialAmount: " + initialAmount + "\n"
        	+ "  publicKey: " + publicKey;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GameteCreationTransactionRequest) {
			GameteCreationTransactionRequest otherCast = (GameteCreationTransactionRequest) other;
			return classpath.equals(otherCast.classpath) && initialAmount.equals(otherCast.initialAmount) && publicKey.equals(otherCast.publicKey);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ publicKey.hashCode();
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		classpath.into(oos);
		marshal(initialAmount, oos);
		oos.writeUTF(publicKey);
	}

	@Override
	public void check() throws TransactionRejectedException {
		if (initialAmount.signum() < 0)
			throw new TransactionRejectedException("the gamete must be initialized with a non-negative amount of coins");

		super.check();
	}
}