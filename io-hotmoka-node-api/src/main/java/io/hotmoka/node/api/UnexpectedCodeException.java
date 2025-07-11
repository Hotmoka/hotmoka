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
 * An exception stating that some code in the store of a Hotmoka node
 * is different or behaves differently from what is expected.
 */
@SuppressWarnings("serial")
public class UnexpectedCodeException extends NodeException {

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of the exception
	 */
	public UnexpectedCodeException(String message) {
		super(Objects.requireNonNull(message));
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 */
	public UnexpectedCodeException(String message, Throwable cause) {
		super(Objects.requireNonNull(message), Objects.requireNonNull(cause));
	}
}