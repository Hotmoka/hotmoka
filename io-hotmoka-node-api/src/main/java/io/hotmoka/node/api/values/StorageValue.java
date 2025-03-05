/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.api.values;

import java.math.BigInteger;
import java.util.function.Function;

import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;

/**
 * A value that can be stored in the store of a Hotmoka node, passed as argument
 * or returned between the outside world and the node.
 */
public interface StorageValue extends Marshallable, Comparable<StorageValue> {

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();

	/**
	 * Yields this value as a big integer, if it is a big integer.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a big integer
	 * @param exception the supplier of the exception thrown if this value is not a big integer
	 * @return the value, as a big integer
	 * @throws E if this value is not a big integer
	 */
	<E extends Exception> BigInteger asBigInteger(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a big integer, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a big integer
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a big integer; it receives
	 *                  a string clarifying that the method was expected to return a big integer while it
	 *                  returned another kind of storage value
	 * @return the value, as a big integer
	 * @throws E if this value is not a big integer
	 */
	<E extends Exception> BigInteger asReturnedBigInteger(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a boolean, if it is a boolean.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a boolean
	 * @param exception the supplier of the exception thrown if this value is not a boolean
	 * @return the value, as a boolean
	 * @throws E if this value is not a boolean
	 */
	<E extends Exception> boolean asBoolean(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a boolean, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a boolean
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a boolean; it receives
	 *                  a string clarifying that the method was expected to return a boolean while it
	 *                  returned another kind of storage value
	 * @return the value, as a boolean
	 * @throws E if this value is not a boolean
	 */
	<E extends Exception> boolean asReturnedBoolean(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a byte, if it is a byte.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a byte
	 * @param exception the supplier of the exception thrown if this value is not a byte
	 * @return the value, as a byte
	 * @throws E if this value is not a byte
	 */
	<E extends Exception> byte asByte(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a byte, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a byte
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a byte; it receives
	 *                  a string clarifying that the method was expected to return a byte while it
	 *                  returned another kind of storage value
	 * @return the value, as a byte
	 * @throws E if this value is not a byte
	 */
	<E extends Exception> byte asReturnedByte(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a char, if it is a char.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a char
	 * @param exception the supplier of the exception thrown if this value is not a char
	 * @return the value, as a char
	 * @throws E if this value is not a char
	 */
	<E extends Exception> char asChar(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a char, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a char
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a char; it receives
	 *                  a string clarifying that the method was expected to return a char while it
	 *                  returned another kind of storage value
	 * @return the value, as a char
	 * @throws E if this value is not a char
	 */
	<E extends Exception> char asReturnedChar(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a double, if it is a double.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a double
	 * @param exception the supplier of the exception thrown if this value is not a double
	 * @return the value, as a double
	 * @throws E if this value is not a double
	 */
	<E extends Exception> double asDouble(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a double, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a double
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a double; it receives
	 *                  a string clarifying that the method was expected to return a double while it
	 *                  returned another kind of storage value
	 * @return the value, as a double
	 * @throws E if this value is not a double
	 */
	<E extends Exception> double asReturnedDouble(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a float, if it is a float.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a float
	 * @param exception the supplier of the exception thrown if this value is not a float
	 * @return the value, as a float
	 * @throws E if this value is not a float
	 */
	<E extends Exception> float asFloat(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a float, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a float
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a float; it receives
	 *                  a string clarifying that the method was expected to return a float while it
	 *                  returned another kind of storage value
	 * @return the value, as a float
	 * @throws E if this value is not a float
	 */
	<E extends Exception> float asReturnedFloat(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as an int, if it is an int.
	 * 
	 * @param <E> the type of the exception thrown if this value is not an int
	 * @param exception the supplier of the exception thrown if this value is not an int
	 * @return the value, as an int
	 * @throws E if this value is not an int
	 */
	<E extends Exception> int asInt(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as an int, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not an int
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not an int; it receives
	 *                  a string clarifying that the method was expected to return an int while it
	 *                  returned another kind of storage value
	 * @return the value, as an int
	 * @throws E if this value is not an int
	 */
	<E extends Exception> int asReturnedInt(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a long, if it is a long.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a long
	 * @param exception the supplier of the exception thrown if this value is not a long
	 * @return the value, as a long
	 * @throws E if this value is not a long
	 */
	<E extends Exception> long asLong(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a long, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a long
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a long; it receives
	 *                  a string clarifying that the method was expected to return a long while it
	 *                  returned another kind of storage value
	 * @return the value, as a long
	 * @throws E if this value is not a long
	 */
	<E extends Exception> long asReturnedLong(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a short, if it is a short.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a short
	 * @param exception the supplier of the exception thrown if this value is not a short
	 * @return the value, as a short
	 * @throws E if this value is not a short
	 */
	<E extends Exception> short asShort(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a short, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a short
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a short; it receives
	 *                  a string clarifying that the method was expected to return a short while it
	 *                  returned another kind of storage value
	 * @return the value, as a short
	 * @throws E if this value is not a short
	 */
	<E extends Exception> short asReturnedShort(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a reference, if it is a reference.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a reference
	 * @param exception the supplier of the exception thrown if this value is not a reference
	 * @return the value, as a reference
	 * @throws E if this value is not a reference
	 */
	<E extends Exception> StorageReference asReference(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a reference, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a reference
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a reference; it receives
	 *                  a string clarifying that the method was expected to return a reference while it
	 *                  returned another kind of storage value
	 * @return the value, as a reference
	 * @throws E if this value is not a reference
	 */
	<E extends Exception> StorageReference asReturnedReference(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;

	/**
	 * Yields this value as a string, if it is a string.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a string
	 * @param exception the supplier of the exception thrown if this value is not a string
	 * @return the value, as a string
	 * @throws E if this value is not a string
	 */
	<E extends Exception> String asString(Function<StorageValue, ? extends E> exception) throws E;

	/**
	 * Yields this value as a string, assuming that it is returned by the given method.
	 * 
	 * @param <E> the type of the exception thrown if this value is not a string
	 * @param method the called method
	 * @param exception the supplier of the exception thrown if this value is not a string; it receives
	 *                  a string clarifying that the method was expected to return a string while it
	 *                  returned another kind of storage value
	 * @return the value, as a string
	 * @throws E if this value is not a string
	 */
	<E extends Exception> String asReturnedString(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E;
}