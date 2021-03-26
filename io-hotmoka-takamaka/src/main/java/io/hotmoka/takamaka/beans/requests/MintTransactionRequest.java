package io.hotmoka.takamaka.beans.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.takamaka.beans.responses.MintTransactionResponse;

/**
 * A request for adding or reducing the coins of an account.
 */
@Immutable
public class MintTransactionRequest extends NonInitialTransactionRequest<MintTransactionResponse> implements SignedTransactionRequest {

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
	 * The chain identifier where this request can be executed, to forbid transaction replay across chains.
	 */
	public final String chainId;

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
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
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
		super(caller, nonce, gasLimit, gasPrice, classpath);

		this.greenAmount = greenAmount;
		this.redAmount = redAmount;
		this.chainId = chainId;
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
		super(caller, nonce, gasLimit, gasPrice, classpath);

		this.greenAmount = greenAmount;
		this.redAmount = redAmount;
		this.chainId = chainId;
		this.signature = signature;
	}

	@Override
	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public String getChainId() {
		return chainId;
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);

		// we add the signature
		byte[] signature = getSignature();
		context.writeCompactInt(signature.length);
		context.write(signature);
	}

	@Override
	public final byte[] toByteArrayWithoutSignature() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			intoWithoutSignature(new MarshallingContext(oos));
			oos.flush();
			return baos.toByteArray();
		}
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  chainId: " + chainId + "\n"
			+ "  greemAmount: " + greenAmount + "\n"
			+ "  redAmount: " + redAmount + "\n"
			+ "  signature: " + bytesToHex(signature);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MintTransactionRequest) {
			MintTransactionRequest otherCast = (MintTransactionRequest) other;
			return super.equals(otherCast) && greenAmount.equals(otherCast.greenAmount) && redAmount.equals(otherCast.redAmount)
				&& chainId.equals(otherCast.chainId);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ greenAmount.hashCode() ^ redAmount.hashCode() ^ chainId.hashCode();
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(gasCostModel.storageCostOf(greenAmount))
			.add(gasCostModel.storageCostOf(redAmount))
			.add(gasCostModel.storageCostOfBytes(signature.length))
			.add(gasCostModel.storageCostOf(chainId));
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.writeByte(EXPANSION_SELECTOR);
		// after the expansion selector, the qualified name of the class must follow
		context.writeUTF(MintTransactionRequest.class.getName());
		context.writeUTF(chainId);
		super.intoWithoutSignature(context);
		context.writeBigInteger(greenAmount);
		context.writeBigInteger(redAmount);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static MintTransactionRequest from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		String chainId = context.readUTF();
		StorageReference caller = StorageReference.from(context);
		BigInteger gasLimit = context.readBigInteger();
		BigInteger gasPrice = context.readBigInteger();
		TransactionReference classpath = TransactionReference.from(context);
		BigInteger nonce = context.readBigInteger();
		BigInteger greenAmount = context.readBigInteger();
		BigInteger redAmount = context.readBigInteger();
		byte[] signature = unmarshallSignature(context);

		return new MintTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, greenAmount, redAmount);
	}
}