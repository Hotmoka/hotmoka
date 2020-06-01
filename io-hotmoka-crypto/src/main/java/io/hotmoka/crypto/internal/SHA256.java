package io.hotmoka.crypto.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.crypto.BytesSupplier;
import io.hotmoka.crypto.HashingAlgorithm;

/**
 * The SHA256 hashing algorithm.
 * 
 * @param <T> the type of values that get hashed
 */
public class SHA256<T> implements HashingAlgorithm<T>{

	private final MessageDigest digest;

	/**
	 * How values get transformed into bytes, before being hashed.
	 */
	private final BytesSupplier<? super T> supplier;

	public SHA256(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		this.digest = MessageDigest.getInstance("SHA-256");
		this.supplier = supplier;
	}

	@Override
	public byte[] hash(T what) {
		try {
			byte[] bytes = supplier.get(what);

			synchronized (digest) {
				digest.reset();
				return digest.digest(bytes);
			}
		}
		catch(Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public int length() {
		return 32;
	}
}