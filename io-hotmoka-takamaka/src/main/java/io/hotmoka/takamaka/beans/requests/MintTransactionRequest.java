package io.hotmoka.takamaka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.takamaka.beans.responses.MintTransactionResponse;

/**
 * A request for adding or reducing the coins of an account.
 */
@Immutable
public class MintTransactionRequest extends NonInitialTransactionRequest<MintTransactionResponse> {

	/**
	 * The amount of green coins that gets added to the caller of the transaction.
	 * This can be negative, in which case green coins are subtracted from those of the caller.
	 */
	public final BigInteger greenAmount;

	/**
	 * The amount of red coins that gets added to the caller of the transaction.
	 * This can be negative, in which case red coins are subtracted from those of the caller.
	 */
	public final BigInteger redAmount;

	/**
	 * The signature of the request.
	 */
	private final byte[] signature;

	/**
	 * Builds the transaction request.
	 * 
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param greenAmount the amount of green coins that gets added to the caller of the transaction.
	 *                    This can be negative, in which case green coins are subtracted from those of the caller
	 * @param redAmount the amount of red coins that gets added to the caller of the transaction.
	 *                  This can be negative, in which case red coins are subtracted from those of the caller
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public MintTransactionRequest(Signer signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, BigInteger greenAmount, BigInteger redAmount) throws InvalidKeyException, SignatureException {
		super(caller, nonce, chainId, gasLimit, gasPrice, classpath);

		this.greenAmount = greenAmount;
		this.redAmount = redAmount;
		this.signature = signer.sign(this);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param greenAmount the amount of green coins that gets added to the caller of the transaction.
	 *                    This can be negative, in which case green coins are subtracted from those of the caller
	 * @param redAmount the amount of red coins that gets added to the caller of the transaction.
	 *                  This can be negative, in which case red coins are subtracted from those of the caller
	 */
	public MintTransactionRequest(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, BigInteger greenAmount, BigInteger redAmount) {
		super(caller, nonce, chainId, gasLimit, gasPrice, classpath);

		this.greenAmount = greenAmount;
		this.redAmount = redAmount;
		this.signature = signature;
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
			+ "  greemAmount: " + greenAmount + "\n"
			+ "  redAmount: " + redAmount;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MintTransactionRequest) {
			MintTransactionRequest otherCast = (MintTransactionRequest) other;
			return super.equals(otherCast) && greenAmount.equals(otherCast.greenAmount) && redAmount.equals(otherCast.redAmount);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ greenAmount.hashCode() ^ redAmount.hashCode();
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(greenAmount)).add(gasCostModel.storageCostOf(redAmount));
	}

	@Override
	public void intoWithoutSignature(ObjectOutputStream oos) throws IOException {
		oos.writeByte(EXPANSION_SELECTOR);
		// after the expansion selector, the qualified name of the class must follow
		oos.writeUTF(MintTransactionRequest.class.getName());
		super.intoWithoutSignature(oos);
		marshal(greenAmount, oos);
		marshal(redAmount, oos);
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
	public static MintTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		StorageReference caller = StorageReference.from(ois);
		BigInteger gasLimit = unmarshallBigInteger(ois);
		BigInteger gasPrice = unmarshallBigInteger(ois);
		TransactionReference classpath = TransactionReference.from(ois);
		BigInteger nonce = unmarshallBigInteger(ois);
		String chainId = ois.readUTF();
		BigInteger greenAmount = unmarshallBigInteger(ois);
		BigInteger redAmount = unmarshallBigInteger(ois);
		byte[] signature = unmarshallSignature(ois);

		return new MintTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, greenAmount, redAmount);
	}
}