package io.takamaka.code.whitelisting.internal.database.java.security;

public interface MessageDigest {
	// this is white-listed since it is not possible to register new providers
	// by using white-listed methods only
	java.security.MessageDigest getInstance(java.lang.String algorithm);
	byte[] digest();
	byte[] digest(byte[] input);
	int digest(byte[] buf, int offset, int len);
	void update(byte input);
	void update(byte[] input);
	void update(byte[] input, int offset, int len);
}