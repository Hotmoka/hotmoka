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

package io.hotmoka.node;

import io.hotmoka.node.api.NodeException;

/**
 * An exception stating that a Hotmoka node is closed and cannot perform the request.
 */
@SuppressWarnings("serial")
public class ClosedNodeException extends NodeException {

	/**
	 * Creates a new exception.
	 */
	public ClosedNodeException() {
		super("The node is closed");
	}

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public ClosedNodeException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public ClosedNodeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public ClosedNodeException(String message, Throwable cause) {
		super(message, cause);
	}
}