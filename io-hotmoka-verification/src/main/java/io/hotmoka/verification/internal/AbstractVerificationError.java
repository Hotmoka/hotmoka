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

package io.hotmoka.verification.internal;

import static java.util.Comparator.comparing;

import java.util.Comparator;
import java.util.Objects;

/**
 * Partial implementation of an error generated
 * during the verification of the class files of a Takamaka program.
 * If an error occurs, then instrumentation cannot proceed and will be aborted.
 * Errors are first ordered by where they occur, then by class name and finally by message.
 */
public abstract class AbstractVerificationError implements io.hotmoka.verification.api.VerificationError {
	private final String where;
	private final String message;

	private final static Comparator<io.hotmoka.verification.api.VerificationError> comparator =
		comparing(io.hotmoka.verification.api.VerificationError::getWhere)
			.thenComparing(error -> error.getClass().getName())
			.thenComparing(io.hotmoka.verification.api.VerificationError::getMessage);

	/**
	 * Creates an error at the given class.
	 * 
	 * @param where a string that lets the user identify the class where the error occurs
	 * @param message the message of the error
	 */
	protected AbstractVerificationError(String where, String message) {
		this.where = Objects.requireNonNull(where);
		this.message = Objects.requireNonNull(message);
	}

	/**
	 * Creates an error at the given program field.
	 * 
	 * @param where a string that lets the user identify the class where the error occurs
	 * @param fieldName the name of the field where the error occurs
	 * @param message the message of the error
	 */
	protected AbstractVerificationError(String where, String fieldName, String message) {
		this.where = Objects.requireNonNull(where) + " field " + Objects.requireNonNull(fieldName);
		this.message = Objects.requireNonNull(message);;
	}

	/**
	 * Creates an error at the given program line.
	 * 
	 * @param where a string that lets the user identify the class where the error occurs
	 * @param methodName the name of the method where the error occurs
	 * @param line the line where the error occurs. Use -1 if the error is related to the method as a whole
	 * @param message the message of the error
	 */
	protected AbstractVerificationError(String where, String methodName, int line, String message) {
		this.where = Objects.requireNonNull(where) + (line >= 0 ? (":" + line) : (" method " + Objects.requireNonNull(methodName)));
		this.message = Objects.requireNonNull(message);;
	}

	@Override
	public final int compareTo(io.hotmoka.verification.api.VerificationError other) {
		return comparator.compare(this, other);
	}

	@Override
	public final boolean equals(Object other) {
		return other instanceof io.hotmoka.verification.api.VerificationError error && getClass() == other.getClass()
			&& where.equals(error.getWhere()) && message.equals(error.getMessage());
	}

	@Override
	public final int hashCode() {
		return where.hashCode() ^ message.hashCode();
	}

	@Override
	public final String toString() {
		return where + ": " + message;
	}

	@Override
	public String getWhere() {
		return where;
	}

	@Override
	public String getMessage() {
		return message;
	}
}