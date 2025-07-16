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

package io.hotmoka.helpers.api;

/**
 * An exception stating that a node is misbehaving, that is, performing in a buggy way.
 * If the node is a local node, then this is an actual bug in its code. If, instead,
 * the node is a remote node, then it might just be a byzantine node.
 */
@SuppressWarnings("serial")
public class MisbehavingNodeException extends Exception {

	/**
	 * Creates a new exception.
	 */
	public MisbehavingNodeException() {
		super("The node is misbehaving");
	}

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public MisbehavingNodeException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public MisbehavingNodeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public MisbehavingNodeException(String message, Throwable cause) {
		super(message, cause);
	}
}