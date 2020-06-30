package io.hotmoka.beans.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithm;

@Immutable
public abstract class NonInitialTransactionRequest<R extends NonInitialTransactionResponse> extends TransactionRequest<R> {

	/**
	 * The externally owned caller contract that pays for the transaction.
	 */
	public final StorageReference caller;

	/**
	 * The gas provided to the transaction.
	 */
	public final BigInteger gasLimit;

	/**
	 * The coins payed for each unit of gas consumed by the transaction.
	 */
	public final BigInteger gasPrice;

	/**
	 * The class path that specifies where the {@code caller} should be interpreted.
	 */
	public final TransactionReference classpath;

	/**
	 * The nonce used for transaction ordering and to forbid transaction replay on the same chain.
	 * It is relative to the caller.
	 */
	public final BigInteger nonce;

	/**
	 * The chain identifier where this request can be executed, to forbid transaction replay across chains.
	 */
	public final String chainId;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 */
	protected NonInitialTransactionRequest(StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath) {
		this.caller = caller;
		this.gasLimit = gasLimit;
		this.gasPrice = gasPrice;
		this.classpath = classpath;
		this.nonce = nonce;
		this.chainId = chainId;
	}

	/**
	 * Yields the signature of the request. This should be the signature of its byte representation (excluding the signature itself)
	 * with the private key of the {@linkplain #caller}, or otherwise the signature is illegal and the request will be rejected.
	 * 
	 * @return the signature
	 */
	public abstract byte[] getSignature();

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  nonce: " + nonce + "\n"
        	+ "  chainId: " + chainId + "\n"
        	+ "  gas limit: " + gasLimit + "\n"
        	+ "  gas price: " + gasPrice + "\n"
        	+ "  class path: " + classpath;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof NonInitialTransactionRequest) {
			NonInitialTransactionRequest<?> otherCast = (NonInitialTransactionRequest<?>) other;
			return caller.equals(otherCast.caller) && gasLimit.equals(otherCast.gasLimit) && gasPrice.equals(otherCast.gasPrice)
				&& classpath.equals(otherCast.classpath) && nonce.equals(otherCast.nonce) && chainId.equals(otherCast.chainId);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return caller.hashCode() ^ gasLimit.hashCode() ^ gasPrice.hashCode() ^ classpath.hashCode() ^ nonce.hashCode() ^ chainId.hashCode();
	}

	@Override
	public final void into(ObjectOutputStream oos) throws IOException {
		intoWithoutSignature(oos);

		// we add the signature
		byte[] signature = getSignature();
		writeLength(signature.length, oos);
		oos.write(signature);
	}

	/**
	 * Marshals this object into the given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * The difference with {@linkplain #into(ObjectOutputStream)} is that the signature
	 * is not marshalled into the stream.
	 * 
	 * @param oos the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	public void intoWithoutSignature(ObjectOutputStream oos) throws IOException {
		caller.intoWithoutSelector(oos);
		marshal(gasLimit, oos);
		marshal(gasPrice, oos);
		classpath.into(oos);
		marshal(nonce, oos);
		oos.writeUTF(chainId);
	}

	/**
	 * Marshals this object into a byte array, without taking its signature into account.
	 * 
	 * @return the byte array resulting from marshalling this object
	 * @throws IOException if this object cannot be marshalled
	 */
	public final byte[] toByteArrayWithoutSignature() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			intoWithoutSignature(oos);
			oos.flush();
			return baos.toByteArray();
		}
	}

	@Override
	public void check() throws TransactionRejectedException {
		if (gasLimit.signum() < 0)
			throw new TransactionRejectedException("gas limit cannot be negative");

		if (gasPrice.signum() < 0)
			throw new TransactionRejectedException("gas price cannot be negative");

		if (chainId == null)
			throw new TransactionRejectedException("chain id cannot be null");

		super.check();
	}

	/**
	 * An object that provides the signature of a request.
	 */
	public interface Signer {

		/**
		 * Computes the signature of the given request.
		 * 
		 * @param what the request to sign
		 * @return the signature of the request
		 * @throws InvalidKeyException if the private key used for signing is invalid
		 * @throws SignatureException if the request cannot be signed
		 */
		byte[] sign(NonInitialTransactionRequest<?> what) throws InvalidKeyException, SignatureException;

		/**
		 * Yields a signer for the given algorithm with the given key pair.
		 * 
		 * @param signature the signing algorithm
		 * @param keys the key pair
		 * @return the signer
		 */
		static Signer with(SignatureAlgorithm<NonInitialTransactionRequest<?>> signature, KeyPair keys) {
			return what -> signature.sign(what, keys.getPrivate());
		}

		/**
		 * Yields a signer for the given algorithm with the given private key.
		 * 
		 * @param signature the signing algorithm
		 * @param key the private key
		 * @return the signer
		 */
		static Signer with(SignatureAlgorithm<NonInitialTransactionRequest<?>> signature, PrivateKey key) {
			return what -> signature.sign(what, key);
		}

		/**
		 * A signer of view requests on behalf of the manifest. Their transactions do not require a verified
		 * signature, hence this signer provides an empty signature.
		 * 
		 * @return the signer
		 */
		static Signer onBehalfOfManifest() {
			return what -> new byte[0];
		}
	}
}