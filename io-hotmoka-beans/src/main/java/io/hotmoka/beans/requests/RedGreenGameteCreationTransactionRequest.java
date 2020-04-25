package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.internal.MarshallingUtils;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;

/**
 * A request for creating an initial red/green gamete.
 */
@Immutable
public class RedGreenGameteCreationTransactionRequest implements InitialTransactionRequest<GameteCreationTransactionResponse> {
	private static final long serialVersionUID = -6733566802012789524L;
	final static byte SELECTOR = 2;

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
		this.classpath = classpath;
		this.initialAmount = initialAmount;
		this.redInitialAmount = redInitialAmount;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  initialAmount: " + initialAmount
        	+ "  redInitialAmount: " + redInitialAmount;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RedGreenGameteCreationTransactionRequest) {
			RedGreenGameteCreationTransactionRequest otherCast = (RedGreenGameteCreationTransactionRequest) other;
			return classpath.equals(otherCast.classpath) && initialAmount.equals(otherCast.initialAmount) && redInitialAmount.equals(otherCast.redInitialAmount);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ redInitialAmount.hashCode();
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		classpath.into(oos);
		MarshallingUtils.marshal(initialAmount, oos);
		MarshallingUtils.marshal(redInitialAmount, oos);
	}
}