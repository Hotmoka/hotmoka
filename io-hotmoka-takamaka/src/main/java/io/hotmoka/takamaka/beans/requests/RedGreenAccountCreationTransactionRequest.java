package io.hotmoka.takamaka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.takamaka.beans.responses.RedGreenAccountCreationTransactionResponse;

/**
 * A request for creating a red/green account, for free.
 */
@Immutable
public class RedGreenAccountCreationTransactionRequest extends NonInitialTransactionRequest<RedGreenAccountCreationTransactionResponse> {

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 */
	public final TransactionReference classpath;

	/**
	 * The amount of coin provided to the account.
	 */

	public final BigInteger initialAmount;

	/**
	 * The amount of red coin provided to the account.
	 */

	public final BigInteger redInitialAmount;

	/**
	 * The Base64-encoded public key that will be assigned to the account.
	 */
	public final String publicKey;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param initialAmount the amount of green coins provided to the account
	 * @param redInitialAmount the amount of red coins provided to the account
	 * @param publicKey the Base64-encoded public key that will be assigned to the account
	 */
	public RedGreenAccountCreationTransactionRequest(TransactionReference classpath, String chainId, BigInteger initialAmount, BigInteger redInitialAmount, String publicKey) {
		// we use a dummy caller
		super(new StorageReference(classpath, BigInteger.ZERO), BigInteger.ZERO, chainId, BigInteger.ONE, BigInteger.ONE, classpath);

		if (classpath == null)
			throw new IllegalArgumentException("classpath cannot be null");

		if (initialAmount == null)
			throw new IllegalArgumentException("initialAmount cannot be null");

		if (initialAmount.signum() < 0)
			throw new IllegalArgumentException("initialAmount cannot be negative");

		if (redInitialAmount == null)
			throw new IllegalArgumentException("redInitialAmount cannot be null");

		if (redInitialAmount.signum() < 0)
			throw new IllegalArgumentException("redInitialAmount cannot be negative");

		if (publicKey == null)
			throw new IllegalArgumentException("publicKey cannot be null");

		this.classpath = classpath;
		this.initialAmount = initialAmount;
		this.redInitialAmount = redInitialAmount;
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  initialAmount: " + initialAmount + "\n"
        	+ "  redInitialAmount: " + redInitialAmount + "\n"
        	+ "  publicKey: " + publicKey;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RedGreenAccountCreationTransactionRequest) {
			RedGreenAccountCreationTransactionRequest otherCast = (RedGreenAccountCreationTransactionRequest) other;
			return classpath.equals(otherCast.classpath) && initialAmount.equals(otherCast.initialAmount) && redInitialAmount.equals(otherCast.redInitialAmount)
				&& publicKey.equals(otherCast.publicKey);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ redInitialAmount.hashCode() ^ publicKey.hashCode();
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.oos.writeByte(EXPANSION_SELECTOR);
		// after the expansion selector, the qualified name of the class must follow
		context.oos.writeUTF(RedGreenAccountCreationTransactionRequest.class.getName());
		classpath.into(context);
		context.oos.writeUTF(chainId);
		marshal(initialAmount, context);
		marshal(redInitialAmount, context);
		context.oos.writeUTF(publicKey);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static RedGreenAccountCreationTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		TransactionReference classpath = TransactionReference.from(ois);
		String chainId = ois.readUTF();
		BigInteger initialAmount = unmarshallBigInteger(ois);
		BigInteger redInitialAmount = unmarshallBigInteger(ois);
		String publicKey = ois.readUTF();

		return new RedGreenAccountCreationTransactionRequest(classpath, chainId, initialAmount, redInitialAmount, publicKey);
	}

	@Override
	public byte[] getSignature() {
		return new byte[0];
	}
}