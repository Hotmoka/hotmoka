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

package io.hotmoka.node.local;

import java.util.Objects;

/**
 * An exception stating that a Hotmoka node was not able to perform an operation.
 * This is a bug in the code of the node or a limit of the system. For instance,
 * it might occur because a cryptographic algorithm is not available or because
 * a file cannot be accessed. Therefore, this exception is not meant to be caught.
 */
@SuppressWarnings("serial")
public class NodeException extends RuntimeException {

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public NodeException(String message) {
		super(Objects.requireNonNull(message, "message cannot be null"));
	}

	/**
	 * Creates a new exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public NodeException(Throwable cause) {
		super(String.valueOf(cause.getMessage()), cause);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public NodeException(String message, Throwable cause) {
		super(Objects.requireNonNull(message, "message cannot be null"), Objects.requireNonNull(cause, "cause cannot be null"));
	}
}