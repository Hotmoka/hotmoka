package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.require;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * The validator of a network of nodes. It can be used to
 * collect money when transactions get validated.
 */
public final class Validator extends ExternallyOwnedAccount {

	/**
	 * The identifier of the validator, unique in the network.
	 * This must be fixed for a given secret.
	 */
	public final String id;

	/**
	 * True if and only if this validator has been revealed.
	 */
	private boolean revealed;

	/**
	 * Creates a validator. It starts as an externally owned account with no funds.
	 * 
	 * @param id the identifier of the validator, unique in the network; this can be
	 *           anything, as long as it does not contain spaces; it is case-insensitive
	 *           and will be stored in lower-case
	 * @param publicKey the Base64-encoded public key of the Takamaka account
	 * @throws NullPointerException if {@code id} or {@code publicKey} is null
	 */
	public Validator(String id, String publicKey) {
		super(publicKey);

		require(id != null, "the identifier of a validator cannot be null");
		require(!id.contains(" "), "spaces are not allowed in a validator identifier");

		this.id = id.toLowerCase();
	}

	/**
	 * Marks this validator as revealed if the first 40 hexadecimal digits
	 * of the sha256 hash of the secret coincide with the identifier of this validator.
	 * 
	 * @param secret the secret
	 * @throws NoSuchAlgorithmException if the SHA-256 hashing is not available in the Java distribution
	 */
	public final @FromContract void reveal(String secret) throws NoSuchAlgorithmException {
		require(caller() == this, "only the same validator can reveal itself");
		require(secret != null, "the secret cannot be null");
		require(id.equals(bytesToHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes())).substring(0, 40)),
			"the first 40 hex digits of the sha256 of the secret must be equal to the id of the validator");

		revealed = true;
	}

	public final @View boolean isRevealed() {
		return revealed;
	}

	@Override
	public @View String toString() {
		if (revealed)
			return id + " [revealed]";
		else
			return id + " [unrevealed]";
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
		final byte[] HEX_ARRAY = "0123456789abcdef".getBytes();
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}
}