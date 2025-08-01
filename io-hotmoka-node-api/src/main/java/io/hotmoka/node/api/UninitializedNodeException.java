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

package io.hotmoka.node.api;

import java.util.Objects;

/**
 * An exception stating that a Hotmoka node has not been initialized yet.
 */
@SuppressWarnings("serial")
public class UninitializedNodeException extends Exception {

	/**
	 * Creates a new exception.
	 */
	public UninitializedNodeException() { // called by reflection, do not remove
		super("The node is not initialized yet");
	}

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public UninitializedNodeException(String message) {
		super(Objects.requireNonNull(message, "message cannot be null"));
	}
}