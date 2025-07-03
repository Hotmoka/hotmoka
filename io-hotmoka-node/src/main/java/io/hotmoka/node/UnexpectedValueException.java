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

package io.hotmoka.node;

import io.hotmoka.node.api.UnexpectedCodeException;

/**
 * An exception stating that a method returns an unexpected value or that
 * a field contains an unexpected value.
 */
@SuppressWarnings("serial")
public class UnexpectedValueException extends UnexpectedCodeException {

	/**
	 * Builds the exception.
	 * 
	 * @param message the message
	 */
	public UnexpectedValueException(String message) {
		super(message);
	}
}