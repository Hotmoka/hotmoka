package io.takamaka.code.blockchain.request;

import java.math.BigInteger;

import io.takamaka.code.blockchain.Blockchain;
import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.UpdateOfBalance;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A request for creating the initial gamete.
 */
@Immutable
public class GameteCreationTransactionRequest implements TransactionRequest {

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

	@Override
	public BigInteger size() {
		// this request is for a free transaction, at initialization of the blockchain
		return BigInteger.ZERO;
	}

	@Override
	public boolean hasMinimalGas(UpdateOfBalance balanceUpdateInCaseOfFailure) {
		// this request is for a free transaction, at initialization of the blockchain
		return true;
	}
}