/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.verification.api;

import java.util.Optional;

/**
 * An exception thrown when the verification of some jar failed.
 */
public class VerificationException extends Exception {
	private static final long serialVersionUID = -1232455923178336022L;
	
	/**
	 * The error that caused the exception. This might be {@code null}.
	 */
	private final io.hotmoka.verification.api.Error error;

	/**
	 * Creates a verification exception not referring to any specific error.
	 */
	public VerificationException() {
		this.error = null;
	}

	/**
	 * Creates a verification exception referring to a given error.
	 * 
	 * @param error the error
	 */
	public VerificationException(io.hotmoka.verification.api.Error error) {
		super(error.toString());

		this.error = error;
	}

	/**
	 * Yields the verification error that caused the exception.
	 * 
	 * @return the error, if any
	 */
	public Optional<io.hotmoka.verification.api.Error> getError() {
		return Optional.ofNullable(error);
	}
}