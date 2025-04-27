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

import io.hotmoka.annotations.Immutable;

/**
 * An error generated during the verification of the class files of a Takamaka program.
 * If an error occurs, code instrumentation cannot proceed and will be aborted.
 * Errors are first ordered by where they occur, then by class name and finally by message.
 */
@Immutable
public interface VerificationError extends Comparable<VerificationError> {
	
	/**
	 * Yields the description of the program point where the error occurs.
	 * 
	 * @return the description
	 */
	String getWhere();

	/**
	 * Yields the message that describes the error.
	 * 
	 * @return the message
	 */
	String getMessage();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}