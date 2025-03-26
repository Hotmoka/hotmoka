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

/**
 * An exception stating that a store state identifier is unknown.
 */
@SuppressWarnings("serial")
public class UnknownStateIdException extends Exception {

	/**
	 * Creates a new exception.
	 */
	public UnknownStateIdException() {
		super("Unknown state identifier");
	}

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public UnknownStateIdException(String message) {
		super(Objects.requireNonNull(message, "message cannot be null"));
	}

	/**
	 * Creates a new exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public UnknownStateIdException(Throwable cause) {
		super(String.valueOf(cause.getMessage()), cause);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public UnknownStateIdException(String message, Throwable cause) {
		super(Objects.requireNonNull(message, "message cannot be null"), Objects.requireNonNull(cause, "cause cannot be null"));
	}

	/**
	 * Creates a new exception stating that the given state identifier is unknown.
	 * 
	 * @param stateId the unknown state identifier
	 */
	public UnknownStateIdException(StateId stateId) {
		super("Unknown state identifier " + stateId);
	}
}