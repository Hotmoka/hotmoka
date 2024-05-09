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

package io.hotmoka.node.local.api;

import java.util.Objects;

import io.hotmoka.node.api.signatures.FieldSignature;

/**
 * An exception stating that a field of an object has not been found in store.
 */
@SuppressWarnings("serial")
public class FieldNotFoundException extends Exception {

	/**
	 * Creates a new exception.
	 */
	public FieldNotFoundException() {
		super("A field has not been found in store");
	}

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public FieldNotFoundException(String message) {
		super(Objects.requireNonNull(message, "message cannot be null"));
	}

	/**
	 * Creates a new exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public FieldNotFoundException(Throwable cause) {
		super(String.valueOf(cause.getMessage()), cause);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public FieldNotFoundException(String message, Throwable cause) {
		super(Objects.requireNonNull(message, "message cannot be null"), Objects.requireNonNull(cause, "cause cannot be null"));
	}

	/**
	 * Creates a new exception for the given missing field.
	 * 
	 * @param field the missing field
	 */
	public FieldNotFoundException(FieldSignature field) {
		super("Field " + field + " has not been found in store");
	}
}