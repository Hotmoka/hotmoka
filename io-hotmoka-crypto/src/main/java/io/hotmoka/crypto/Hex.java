/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.crypto;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.EncoderException;

/**
 * Simple class for translation of byte arrays into and from hexadecimal strings.
 */
public final class Hex {

	private Hex() {}

	/**
	 * Yields a string hexadecimal representation of the given bytes.
	 * 
	 * @param data the bytes (most significant byte first)
	 * @return the hexadecimal string representation
	 */
	public static String toHexString(byte[] data) {
		return org.bouncycastle.util.encoders.Hex.toHexString(data);
	}

	/**
	 * Yields a string hexadecimal representation of a portion of the given bytes.
	 * 
	 * @param data the bytes (most significant byte first)
	 * @param offset the starting point in {@code bytes}
	 * @param length the number of bytes to consider
	 * @return the hexadecimal string representation
	 * @throws HexConversionException if the conversion fails
	 */
	public static String toHexString(byte[] data, int offset, int length) throws HexConversionException {
		try {
			return org.bouncycastle.util.encoders.Hex.toHexString(data, offset, length);
		}
		catch (EncoderException e) {
			throw new HexConversionException(e);
		}
	}

	/**
	 * Yields a byte representation of the given hexadecimal string.
	 * 
	 * @param hex the hexadecimal string
	 * @return the byte representation (most significant byte first)
	 * @throws HexConversionException if the conversion fails
	 */
	public static byte[] fromHexString(String hex) throws HexConversionException {
		try {
			return org.bouncycastle.util.encoders.Hex.decode(hex);
		}
		catch (DecoderException e) {
			throw new HexConversionException(e);
		}
	}
}