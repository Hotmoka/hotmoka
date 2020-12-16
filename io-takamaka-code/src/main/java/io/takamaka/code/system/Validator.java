package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.require;

import java.nio.charset.StandardCharsets;

import io.takamaka.code.lang.ExternallyOwnedAccount;
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

	// bytesToHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes())).substring(0, 40)

	@Override
	public @View String toString() {
		return "validator " + id;
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