package io.hotmoka.crypto.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.crypto.HashingAlgorithm;

/**
 * The SHA256 hashing algorithm.
 * 
 * @param <T> the type of values that get hashed
 */
public class SHA256<T extends Marshallable> implements HashingAlgorithm<T>{

	private final MessageDigest digest;

	public SHA256() throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA-256");
	}

	@Override
	public byte[] hash(T what) {
		try {
			byte[] bytes = what.toByteArray();

			synchronized (digest) {
				digest.reset();
				return digest.digest(bytes);
			}
		}
		catch(Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public int length() {
		return 32;
	}
}