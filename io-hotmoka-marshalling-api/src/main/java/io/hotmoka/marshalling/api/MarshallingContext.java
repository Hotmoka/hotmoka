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
	 * Writes the given object into the output stream. This context must have
	 * an object marshaller registered for the class of the object.
	 * 
	 * @param <C> the type of the object
	 * @param clazz the class of the object
	 * @param value the object to marshal
	 * @throws IOException if the object could not be unmarshalled
	 */
	<C> void writeObject(Class<C> clazz, C value) throws IOException;

	/**
	 * Writes the given string into the output stream. It uses a memory
	 * to avoid repeated writing of the same string: the second write
	 * will refer to the first one.
	 * 
	 * @param s the string to write
	 * @throws IOException if the string could not be written
	 */
	void writeStringShared(String s) throws IOException;

	void writeByte(int b) throws IOException;

	void writeChar(int c) throws IOException;

	void writeInt(int i) throws IOException;

	/**
	 * Writes the given integer, in a way that compacts small integers.
	 * 
	 * @param i the integer
	 * @throws IOException if the integer cannot be marshalled
	 */
	void writeCompactInt(int i) throws IOException;

	void writeUTF(String s) throws IOException;

	void write(byte[] bytes) throws IOException;

	void writeDouble(double d) throws IOException;

	void writeFloat(float f) throws IOException;

	void writeLong(long l) throws IOException;

	void writeShort(int s) throws IOException;

	void writeBoolean(boolean b) throws IOException;

	/**
	 * Writes the given big integer, in a compact way.
	 * 
	 * @param bi the big integer
	 * @throws IOException if the big integer could not be written
	 */
	void writeBigInteger(BigInteger bi) throws IOException;

	void flush() throws IOException;

	@Override
	void close() throws IOException;
}