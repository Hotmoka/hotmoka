package io.hotmoka.beans.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling a constructor of a storage class in a node.
 */
@Immutable
public class ConstructorCallTransactionRequest extends CodeExecutionTransactionRequest<ConstructorCallTransactionResponse> implements SignedTransactionRequest {
	final static byte SELECTOR = 4;

	/**
	 * The constructor to call.
	 */
	public final ConstructorSignature constructor;

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
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public ConstructorCallTransactionRequest(Signer signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		if (constructor == null)
			throw new IllegalArgumentException("constructor cannot be null");

		if (constructor.formals().count() != actuals.length)
			throw new IllegalArgumentException("argument count mismatch between formals and actuals");

		if (chainId == null)
			throw new IllegalArgumentException("chainId cannot be null");

		this.constructor = constructor;
		this.chainId = chainId;
		this.signature = signer.sign(this);
	}

	/**
	 * Builds the transaction request.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 */
	public ConstructorCallTransactionRequest(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) {
		super(caller, nonce, gasLimit, gasPrice, classpath, actuals);

		if (constructor == null)
			throw new IllegalArgumentException("constructor cannot be null");

		if (constructor.formals().count() != actuals.length)
			throw new IllegalArgumentException("argument count mismatch between formals and actuals");

		if (chainId == null)
			throw new IllegalArgumentException("chainId cannot be null");

		if (signature == null)
			throw new IllegalArgumentException("signature cannot be null");

		this.constructor = constructor;
		this.chainId = chainId;
		this.signature = signature;
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		intoWithoutSignature(context);

		// we add the signature
		byte[] signature = getSignature();
		writeLength(signature.length, context);
		context.oos.write(signature);
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
			+ "  constructor: " + constructor + "\n"
			+ "  actuals:\n" + actuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", "")) + "\n"
			+ "  signature: " + bytesToHex(signature);
	}

	@Override
	public ConstructorSignature getStaticTarget() {
		return constructor;
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
	public boolean equals(Object other) {
		if (other instanceof ConstructorCallTransactionRequest) {
			ConstructorCallTransactionRequest otherCast = (ConstructorCallTransactionRequest) other;
			return super.equals(other) && constructor.equals(otherCast.constructor) && chainId.equals(otherCast.chainId)
				&& Arrays.equals(signature, otherCast.signature);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ constructor.hashCode() ^ chainId.hashCode() ^ Arrays.hashCode(signature);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(constructor.size(gasCostModel))
			.add(gasCostModel.storageCostOfBytes(signature.length))
			.add(gasCostModel.storageCostOf(chainId));
	}

	@Override
	public void intoWithoutSignature(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		context.oos.writeUTF(chainId);
		super.intoWithoutSignature(context);
		constructor.into(context);
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
	public static ConstructorCallTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		String chainId = ois.readUTF();
		StorageReference caller = StorageReference.from(ois);
		BigInteger gasLimit = unmarshallBigInteger(ois);
		BigInteger gasPrice = unmarshallBigInteger(ois);
		TransactionReference classpath = TransactionReference.from(ois);
		BigInteger nonce = unmarshallBigInteger(ois);
		StorageValue[] actuals = unmarshallingOfArray(StorageValue::from, StorageValue[]::new, ois);
		ConstructorSignature constructor = (ConstructorSignature) CodeSignature.from(ois);
		byte[] signature = unmarshallSignature(ois);

		return new ConstructorCallTransactionRequest(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals);
	}
}