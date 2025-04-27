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

import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.VerificationError;
import io.hotmoka.verification.errors.CallerNotOnThisError;
import io.hotmoka.verification.errors.CallerOutsideFromContractError;
import io.hotmoka.verification.errors.FromContractNotInStorageError;
import io.hotmoka.verification.errors.IllegalAccessToNonWhiteListedFieldError;
import io.hotmoka.verification.errors.IllegalBootstrapMethodError;
import io.hotmoka.verification.errors.IllegalCallToFromContractError;
import io.hotmoka.verification.errors.IllegalCallToFromContractOnThisError;
import io.hotmoka.verification.errors.IllegalCallToNonWhiteListedConstructorError;
import io.hotmoka.verification.errors.IllegalCallToNonWhiteListedMethodError;
import io.hotmoka.verification.errors.IllegalCallToPayableConstructorOnThis;
import io.hotmoka.verification.errors.IllegalFieldNameError;
import io.hotmoka.verification.errors.IllegalFinalizerError;
import io.hotmoka.verification.errors.IllegalFromContractArgumentError;
import io.hotmoka.verification.errors.IllegalJsrInstructionError;
import io.hotmoka.verification.errors.IllegalMethodNameError;
import io.hotmoka.verification.errors.IllegalModificationOfAmountInConstructorChaining;
import io.hotmoka.verification.errors.IllegalNativeMethodError;
import io.hotmoka.verification.errors.IllegalPackageNameError;
import io.hotmoka.verification.errors.IllegalPutstaticInstructionError;
import io.hotmoka.verification.errors.IllegalRetInstructionError;
import io.hotmoka.verification.errors.IllegalStaticInitializationError;
import io.hotmoka.verification.errors.IllegalSynchronizationError;
import io.hotmoka.verification.errors.IllegalTypeForStorageFieldError;
import io.hotmoka.verification.errors.IllegalUpdateOfLocal0Error;
import io.hotmoka.verification.errors.IllegalUseOfDummyInFieldSignatureError;
import io.hotmoka.verification.errors.IllegalUseOfDummyInMethodSignatureError;
import io.hotmoka.verification.errors.InconsistentFromContractError;
import io.hotmoka.verification.errors.InconsistentPayableError;
import io.hotmoka.verification.errors.InconsistentThrowsExceptionsError;
import io.hotmoka.verification.errors.PayableNotInContractError;
import io.hotmoka.verification.errors.PayableWithoutAmountError;
import io.hotmoka.verification.errors.PayableWithoutFromContractError;
import io.hotmoka.verification.errors.ThrowsExceptionsOnNonPublicError;
import io.hotmoka.verification.errors.UncheckedExceptionHandlerError;
import io.hotmoka.verification.internal.json.VerificationErrorJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
	 * @param field the field where the error occurs
	 * @param message the message of the error
	 */
	protected AbstractVerificationError(String where, Field field, String message) {
		this.where = Objects.requireNonNull(where) + " field " + field.getName();
		this.message = Objects.requireNonNull(message);
	}

	/**
	 * Creates an error at the given program line.
	 * 
	 * @param where a string that lets the user identify the class where the error occurs
	 * @param method the method where the error occurs
	 * @param line the line where the error occurs. Use -1 if the error is related to the method as a whole
	 * @param message the message of the error
	 */
	protected AbstractVerificationError(String where, MethodGen method, int line, String message) {
		this.where = Objects.requireNonNull(where) + (line >= 0 ? (":" + line) : (" method " + method.getName()));
		this.message = Objects.requireNonNull(message);
	}

	/**
	 * Yields the verification error represented by the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the verification error
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public static VerificationError from(VerificationErrorJson json) throws InconsistentJsonException {
		String type = io.hotmoka.exceptions.Objects.requireNonNull(json.getType(), "type cannot be null", InconsistentJsonException::new);
		String where = io.hotmoka.exceptions.Objects.requireNonNull(json.getWhere(), "where cannot be null", InconsistentJsonException::new);
		String message = io.hotmoka.exceptions.Objects.requireNonNull(json.getMessage(), "message cannot be null", InconsistentJsonException::new);

		if (CallerNotOnThisError.class.getSimpleName().equals(type))
			return new CallerNotOnThisError(where, message);
		else if (CallerOutsideFromContractError.class.getSimpleName().equals(type))
			return new CallerOutsideFromContractError(where, message);
		else if (FromContractNotInStorageError.class.getSimpleName().equals(type))
			return new FromContractNotInStorageError(where, message);
		else if (IllegalAccessToNonWhiteListedFieldError.class.getSimpleName().equals(type))
			return new IllegalAccessToNonWhiteListedFieldError(where, message);
		else if (IllegalBootstrapMethodError.class.getSimpleName().equals(type))
			return new IllegalBootstrapMethodError(where, message);
		else if (IllegalCallToFromContractError.class.getSimpleName().equals(type))
			return new IllegalCallToFromContractError(where, message);
		else if (IllegalCallToFromContractOnThisError.class.getSimpleName().equals(type))
			return new IllegalCallToFromContractOnThisError(where, message);
		else if (IllegalCallToNonWhiteListedConstructorError.class.getSimpleName().equals(type))
			return new IllegalCallToNonWhiteListedConstructorError(where, message);
		else if (IllegalCallToNonWhiteListedMethodError.class.getSimpleName().equals(type))
			return new IllegalCallToNonWhiteListedMethodError(where, message);
		else if (IllegalCallToPayableConstructorOnThis.class.getSimpleName().equals(type))
			return new IllegalCallToPayableConstructorOnThis(where, message);
		else if (IllegalFieldNameError.class.getSimpleName().equals(type))
			return new IllegalFieldNameError(where, message);
		else if (IllegalFinalizerError.class.getSimpleName().equals(type))
			return new IllegalFinalizerError(where, message);
		else if (IllegalFromContractArgumentError.class.getSimpleName().equals(type))
			return new IllegalFromContractArgumentError(where, message);
		else if (IllegalJsrInstructionError.class.getSimpleName().equals(type))
			return new IllegalJsrInstructionError(where, message);
		else if (IllegalMethodNameError.class.getSimpleName().equals(type))
			return new IllegalMethodNameError(where, message);
		else if (IllegalModificationOfAmountInConstructorChaining.class.getSimpleName().equals(type))
			return new IllegalModificationOfAmountInConstructorChaining(where, message);
		else if (IllegalNativeMethodError.class.getSimpleName().equals(type))
			return new IllegalNativeMethodError(where, message);
		else if (IllegalPackageNameError.class.getSimpleName().equals(type))
			return new IllegalPackageNameError(where, message);
		else if (IllegalPutstaticInstructionError.class.getSimpleName().equals(type))
			return new IllegalPutstaticInstructionError(where, message);
		else if (IllegalRetInstructionError.class.getSimpleName().equals(type))
			return new IllegalRetInstructionError(where, message);
		else if (IllegalStaticInitializationError.class.getSimpleName().equals(type))
			return new IllegalStaticInitializationError(where, message);
		else if (IllegalSynchronizationError.class.getSimpleName().equals(type))
			return new IllegalSynchronizationError(where, message);
		else if (IllegalTypeForStorageFieldError.class.getSimpleName().equals(type))
			return new IllegalTypeForStorageFieldError(where, message);
		else if (IllegalUpdateOfLocal0Error.class.getSimpleName().equals(type))
			return new IllegalUpdateOfLocal0Error(where, message);
		else if (IllegalUseOfDummyInFieldSignatureError.class.getSimpleName().equals(type))
			return new IllegalUseOfDummyInFieldSignatureError(where, message);
		else if (IllegalUseOfDummyInMethodSignatureError.class.getSimpleName().equals(type))
			return new IllegalUseOfDummyInMethodSignatureError(where, message);
		else if (InconsistentFromContractError.class.getSimpleName().equals(type))
			return new InconsistentFromContractError(where, message);
		else if (InconsistentPayableError.class.getSimpleName().equals(type))
			return new InconsistentPayableError(where, message);
		else if (InconsistentThrowsExceptionsError.class.getSimpleName().equals(type))
			return new InconsistentThrowsExceptionsError(where, message);
		else if (PayableNotInContractError.class.getSimpleName().equals(type))
			return new PayableNotInContractError(where, message);
		else if (PayableWithoutAmountError.class.getSimpleName().equals(type))
			return new PayableWithoutAmountError(where, message);
		else if (PayableWithoutFromContractError.class.getSimpleName().equals(type))
			return new PayableWithoutFromContractError(where, message);
		else if (ThrowsExceptionsOnNonPublicError.class.getSimpleName().equals(type))
			return new ThrowsExceptionsOnNonPublicError(where, message);
		else if (UncheckedExceptionHandlerError.class.getSimpleName().equals(type))
			return new UncheckedExceptionHandlerError(where, message);
		else {
			if (type.length() > 100)
				type = type.substring(0, 100) + "...";

			throw new InconsistentJsonException("Unknown error type " + type);
		}
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