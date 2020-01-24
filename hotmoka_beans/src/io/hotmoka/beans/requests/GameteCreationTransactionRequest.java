package io.hotmoka.beans.requests;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;

/**
 * A request for creating an initial gamete.
 */
@Immutable
public class GameteCreationTransactionRequest implements InitialTransactionRequest<GameteCreationTransactionResponse> {

	private static final long serialVersionUID = -6733566802012789524L;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(JarStoreInitialTransactionRequest)}.
	 */
	public final Classpath classpath;

	/**
	 * The amount of coin provided to the gamete.
	 */

	public final BigInteger initialAmount;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(JarStoreInitialTransactionRequest)}
	 * @param initialAmount the amount of coin provided to the gamete
	 */
	public GameteCreationTransactionRequest(Classpath classpath, BigInteger initialAmount) {
		this.classpath = classpath;
		this.initialAmount = initialAmount;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  initialAmount: " + initialAmount;
	}
}