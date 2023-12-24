/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.marshalling.api;

import java.io.IOException;
import java.math.BigInteger;

/**
 * A context used during object marshaling into bytes.
 */
public interface MarshallingContext extends AutoCloseable {

	/**
	 * Writes the given object into this context, which must have
	 * an object marshaller registered for the class of the object.
	 * 
	 * @param <C> the type of the object
	 * @param clazz the class of the object
	 * @param value the object to marshal
	 * @throws IOException if an I/O error occurs
	 */
	<C> void writeObject(Class<C> clazz, C value) throws IOException;

	/**
	 * Writes the given byte into this context. Only the 8 least significant bits
	 * of {@code b} are written.
	 * 
	 * @param b the byte to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeByte(int b) throws IOException;

	/**
	 * Writes the given character into this context. Only the 16 least significant bits
	 * of {@code c} are written.
	 * 
	 * @param c the character to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeChar(int c) throws IOException;

	/**
	 * Writes the given integer into this context. This requires that the
	 * integer will be subsequently read through {@link UnmarshallingContext#readInt()}.
	 * 
	 * @param i the integer to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeInt(int i) throws IOException;

	/**
	 * Writes the given integer into this context, using compact representations for
	 * frequent cases. This requires that the integer
	 * will be subsequently read through {@link UnmarshallingContext#readCompactInt()}.
	 * 
	 * @param i the integer to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeCompactInt(int i) throws IOException;

	/**
	 * Writes the given string into this context. This requires that
	 * the string will be subsequently extracted through {@link UnmarshallingContext#readStringShared()}.
	 * In comparison to {@link #writeStringUnshared(String)}, this representation might be smaller
	 * if the string is likely repeated in the same marshalled object.
	 * 
	 * @param s the string to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeStringShared(String s) throws IOException;

	/**
	 * Writes the given string into this context. This requires that
	 * the string will be subsequently extracted through {@link UnmarshallingContext#readStringUnshared()}.
	 * In comparison to {@link #writeStringShared(String)}, this representation might be smaller
	 * if the string is unlikely to be repeated in the same marshalled object.
	 * 
	 * @param s the string to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeStringUnshared(String s) throws IOException;

	/**
	 * Writes the given bytes into this context. The length of the array is not written,
	 * hence this method can be applied when the size of the array is known from some
	 * structural constraint.
	 * 
	 * @param bytes the bytes to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeBytes(byte[] bytes) throws IOException;

	/**
	 * Writes the length of the given bytes and the bytes itself into this context.
	 * 
	 * @param bytes the bytes to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeLengthAndBytes(byte[] bytes) throws IOException;

	/**
	 * Writes the length of the given array of marshallables and its elements
	 * into this context. It is assumed that the array will be read back with
	 * {@link UnmarshallingContext#readLengthAndArray(io.hotmoka.marshalling.api.Unmarshaller, java.util.function.Function)}.
	 * 
	 * @param marshallables the array of marshallables
	 * @throws IOException if some elements could not be marshalled
	 */
	void writeLengthAndArray(Marshallable[] marshallables) throws IOException;

	/**
	 * Writes the given double into this context.
	 * 
	 * @param d the double to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeDouble(double d) throws IOException;

	/**
	 * Writes the given float into this context.
	 * 
	 * @param f the float to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeFloat(float f) throws IOException;

	/**
	 * Writes the given long into this context.
	 * 
	 * @param l the long to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeLong(long l) throws IOException;

	/**
	 * Writes the given long into this context, using compact representations for
	 * frequent cases. This requires that the long
	 * will be subsequently read through {@link UnmarshallingContext#readCompactLong()}.
	 * 
	 * @param l the long to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeCompactLong(long l) throws IOException;

	/**
	 * Writes the given short into this context.
	 * 
	 * @param s the short to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeShort(int s) throws IOException;

	/**
	 * Writes the given boolean into this context.
	 * 
	 * @param b the boolean to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeBoolean(boolean b) throws IOException;

	/**
	 * Writes the given big integer into this context, in a compact way.
	 * 
	 * @param bi the big integer to write
	 * @throws IOException if an I/O error occurs
	 */
	void writeBigInteger(BigInteger bi) throws IOException;

	/**
	 * Flushes the buffer of the writer into disk, if any.
	 * 
	 * @throws IOException if an I/O error occurs
	 */
	void flush() throws IOException;

	@Override
	void close() throws IOException;
}