/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.node.local.internal.tries;

/**
 * An exception thrown when the index of the a Hotmoka node is corrupted.
 */
@SuppressWarnings("serial")
public class IndexException extends RuntimeException {

	/**
	 * Creates an exception with the given message.
	 * 
	 * @param message the message
	 */
	public IndexException(String message) {
		super(message);
	}

	/**
	 * Creates an exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public IndexException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates an exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public IndexException(String message, Throwable cause) {
		super(message, cause);
	}
}