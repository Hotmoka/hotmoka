package io.hotmoka.crypto.internal;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A signature algorithm that hashes data with SHA256 and then
 * sign them with the DSA algorithm.
 * 
 * @param <T> the type of values that get signed
 */
public class SHA256withDSA<T> implements SignatureAlgorithm<T> {

	/**
	 * The actual signing algorithm.
	 */
	private final Signature signature;

	/**
	 * The key pair generator.
	 */
	private final KeyPairGenerator keyPairGen;

	/**
	 * How values get transformed into bytes, before being hashed.
	 */
	private final BytesSupplier<? super T> supplier;

	public SHA256withDSA(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		this.signature = Signature.getInstance("SHA256withDSA");
		this.keyPairGen = KeyPairGenerator.getInstance("DSA");
		this.keyPairGen.initialize(2048);
		this.supplier = supplier;
	}

	@Override
	public KeyPair getKeyPair() {
		return keyPairGen.generateKeyPair();
	}

	@Override
	public byte[] sign(T what, PrivateKey privateKey) throws InvalidKeyException, SignatureException {
		byte[] bytes;

		try {
			bytes = supplier.get(what);
		}
		catch (Exception e) {
			throw new SignatureException("cannot transform value into bytes before signing", e);
		}

		synchronized (signature) {
			signature.initSign(privateKey);
			signature.update(bytes);
			return signature.sign();
		}
	}

	@Override
	public boolean verify(T what, PublicKey publicKey, byte[] signature) throws InvalidKeyException, SignatureException {
		byte[] bytes;

		try {
			bytes = supplier.get(what);
		}
		catch (Exception e) {
			throw new SignatureException("cannot transform value into bytes before verifying the signature", e);
		}

		synchronized (this.signature) { 
			this.signature.initVerify(publicKey);
			this.signature.update(bytes);
			return this.signature.verify(signature);
		}
	}
}