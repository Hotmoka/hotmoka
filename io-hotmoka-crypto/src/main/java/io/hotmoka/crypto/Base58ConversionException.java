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

package io.hotmoka.crypto;

/**
 * An exception generated when the conversion between Base58 strings and bytes fails.
 */
@SuppressWarnings("serial")
public class Base58ConversionException extends Exception {

	/**
	 * Creates the exception.
	 * 
	 * @param message the message of the exception
	 */
	public Base58ConversionException(String message) {
		super(message);
	}

	/**
	 * Creates the exception.
	 * 
	 * @param cause the cause of the exception
	 */
	public Base58ConversionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates the exception.
	 * 
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 */
	public Base58ConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}