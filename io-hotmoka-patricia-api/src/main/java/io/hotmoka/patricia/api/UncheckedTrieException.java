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

package io.hotmoka.patricia.api;

import java.util.Objects;

/**
 * An exception stating that the execution of a method of a Patricia trie contains a bug.
 */
@SuppressWarnings("serial")
public class UncheckedTrieException extends RuntimeException { // TODO: rename into TrieException at the end

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public UncheckedTrieException(String message) {
		super(Objects.requireNonNull(message, "message cannot be null"));
	}

	/**
	 * Creates a new exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public UncheckedTrieException(Throwable cause) {
		super(String.valueOf(cause.getMessage()), cause);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public UncheckedTrieException(String message, Throwable cause) {
		super(Objects.requireNonNull(message, "message cannot be null"), Objects.requireNonNull(cause, "cause cannot be null"));
	}
}