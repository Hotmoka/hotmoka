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

package io.hotmoka.node.tendermint.internal;

/**
 * An exception thrown when the Tendermint tool misbehaves.
 */
@SuppressWarnings("serial")
public class TendermintException extends Exception {

	/**
	 * Creates the exception.
	 * 
	 * @param message the message of the exception
	 */
	public TendermintException(String message) {
		super(message);
	}

	/**
	 * Creates the exception.
	 * 
	 * @param cause the cause of the exception
	 */
	public TendermintException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates the exception.
	 * 
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 */
	public TendermintException(String message, Throwable cause) {
		super(message, cause);
	}
}