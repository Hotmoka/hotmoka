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
import java.util.function.Function;

import io.hotmoka.exceptions.UncheckedIOException;

/**
 * A context used during bytes unmarshalling into objects.
 */
public interface UnmarshallingContext extends AutoCloseable {

	/**
	 * Yields an object unmarshalled from this context.
	 * This context must have an object unmarshaller registered for the
	 * class of the object.
	 * 
	 * @param <C> the type of the object
	 * @param clazz the class of the object
	 * @return the unmarshalled object
	 * @throws UncheckedIOException if the object could not be unmarshalled
	 */
	<C> C readObject(Class<C> clazz) throws UncheckedIOException;

	/**
	 * Yields an array of marshallables unmarshalled from this context.
	 * 
	 * @param <T> the type of the marshallables
	 * @param unmarshaller the object that unmarshals a single marshallable
	 * @param supplier the creator of the resulting array of marshallables
	 * @return the array
	 * @throws UncheckedIOException if some marshallable could not be unmarshalled
	 * @throws ClassNotFoundException if some marshallable could not be unmarshalled
	 */
	<T extends Marshallable> T[] readArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier) throws UncheckedIOException, ClassNotFoundException;

	byte readByte() throws UncheckedIOException;

	char readChar() throws UncheckedIOException;

	boolean readBoolean() throws UncheckedIOException;

	int readInt() throws UncheckedIOException;

	/**
	 * Reads an optimized integer.
	 * 
	 * @return the integer
	 * @throws UncheckedIOException if the integer cannot be read
	 */
	int readCompactInt() throws UncheckedIOException;

	short readShort() throws UncheckedIOException;

	long readLong() throws UncheckedIOException;

	float readFloat() throws UncheckedIOException;

	double readDouble() throws UncheckedIOException;

	String readUTF() throws UncheckedIOException;

	/**
	 * Reads {@code length} bytes from the context.
	 * 
	 * @param length the number of bytes to read
	 * @param errorMessage the message of the exception if it was not possible to read exactly {@code length} bytes
	 * @return the bytes
	 * @throws UncheckedIOException if it was not possible to read {@code length} bytes
	 */
	byte[] readBytes(int length, String errorMessage) throws UncheckedIOException;

	String readStringShared() throws UncheckedIOException;

	/**
	 * Reads a big integer, taking into account
	 * optimized representations used for the big integer.
	 * 
	 * @return the big integer
	 * @throws IOException if the big integer could not be written
	 */
	BigInteger readBigInteger() throws UncheckedIOException;

	@Override
	void close() throws UncheckedIOException;
}