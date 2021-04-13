package io.takamaka.code.whitelisting.internal.database.version0.java.util;

public interface Base64 {
	java.util.Base64.Decoder getDecoder();
	java.util.Base64.Encoder getEncoder();
	
	interface Decoder {
		byte[] decode(byte[] src);
		byte[] decode(String src);
		int decode(byte[] src, byte[] dst);
	}

	interface Encoder {
		byte[] encode(byte[] src);
		int encode(byte[] src, byte[] dst);
		String encodeToString(byte[] src);
	}
}