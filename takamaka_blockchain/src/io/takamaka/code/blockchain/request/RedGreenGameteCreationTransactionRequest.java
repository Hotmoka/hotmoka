package io.takamaka.code.blockchain.request;

import java.math.BigInteger;

import io.takamaka.code.blockchain.Blockchain;
import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A request for creating an initial red/green gamete.
 */
@Immutable
public class RedGreenGameteCreationTransactionRequest extends GameteCreationTransactionRequest {
	private static final long serialVersionUID = -6733566802012789524L;

	/**
	 * The amount of red coin provided to the gamete.
	 */

	public final BigInteger redInitialAmount;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(JarStoreInitialTransactionRequest)}
	 * @param initialAmount the amount of green coins provided to the gamete
	 * @param redInitialAmount the amount of red coins provided to the gamete
	 */
	public RedGreenGameteCreationTransactionRequest(Classpath classpath, BigInteger initialAmount, BigInteger redInitialAmount) {
		super(classpath, initialAmount);

		this.redInitialAmount = redInitialAmount;
	}

	@Override
	public String toString() {
        return super.toString() + "  redInitialAmount: " + redInitialAmount;
	}
}