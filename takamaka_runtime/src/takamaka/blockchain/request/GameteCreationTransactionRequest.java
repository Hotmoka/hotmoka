package takamaka.blockchain.request;

import java.math.BigInteger;
import java.nio.file.Path;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.TransactionRequest;
import takamaka.lang.Immutable;

/**
 * A request for creating the initial gamete.
 */
@Immutable
public class GameteCreationTransactionRequest implements TransactionRequest {

	private static final long serialVersionUID = -6733566802012789524L;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(Path, Classpath...)}.
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
	 *                  have been already installed by a previous {@link Blockchain#addJarStoreInitialTransaction(Path, Classpath...)}
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