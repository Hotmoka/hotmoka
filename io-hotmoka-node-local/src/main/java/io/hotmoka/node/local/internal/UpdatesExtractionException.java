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

package io.hotmoka.node.local.internal;

/**
 * An exception thrown when the extraction of the updates to objects in RAM fails.
 */
@SuppressWarnings("serial")
public class UpdatesExtractionException extends Exception {

	/**
	 * Creates the exception with the given message.
	 * 
	 * @param message the message
	 */
	UpdatesExtractionException(String message) {
		super(message);
	}

	/**
	 * Creates the exception with the given message and cause.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	UpdatesExtractionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates the exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	UpdatesExtractionException(Throwable cause) {
		super("Cannot extract the updates", cause);
	}
}
