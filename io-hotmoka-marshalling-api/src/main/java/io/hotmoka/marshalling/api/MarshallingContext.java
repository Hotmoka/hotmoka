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

import java.math.BigInteger;

import io.hotmoka.exceptions.UncheckedIOException;

/**
 * A context used during object marshaling into bytes.
 */
public interface MarshallingContext extends AutoCloseable {

	/**
	 * Writes the given object into the output stream. This context must have
	 * an object marshaller registered for the class of the object.
	 * 
	 * @param <C> the type of the object
	 * @param clazz the class of the object
	 * @param value the object to marshal
	 * @throws UncheckedIOException if the object could not be unmarshalled
	 */
	<C> void writeObject(Class<C> clazz, C value) throws UncheckedIOException;

	/**
	 * Writes the given string into the output stream. It uses a memory
	 * to avoid repeated writing of the same string: the second write
	 * will refer to the first one.
	 * 
	 * @param s the string to write
	 * @throws IOException if the string could not be written
	 */
	void writeStringShared(String s) throws UncheckedIOException;

	void writeByte(int b) throws UncheckedIOException;

	void writeChar(int c) throws UncheckedIOException;

	void writeInt(int i) throws UncheckedIOException;

	/**
	 * Writes the given integer, in a way that compacts small integers.
	 * 
	 * @param i the integer
	 * @throws UncheckedIOException if the integer cannot be marshalled
	 */
	void writeCompactInt(int i) throws UncheckedIOException;

	void writeUTF(String s) throws UncheckedIOException;

	void write(byte[] bytes) throws UncheckedIOException;

	void writeDouble(double d) throws UncheckedIOException;

	void writeFloat(float f) throws UncheckedIOException;

	void writeLong(long l) throws UncheckedIOException;

	void writeShort(int s) throws UncheckedIOException;

	void writeBoolean(boolean b) throws UncheckedIOException;

	/**
	 * Writes the given big integer, in a compact way.
	 * 
	 * @param bi the big integer
	 * @throws IOException if the big integer could not be written
	 */
	void writeBigInteger(BigInteger bi) throws UncheckedIOException;

	void flush() throws UncheckedIOException;

	@Override
	void close() throws UncheckedIOException;
}