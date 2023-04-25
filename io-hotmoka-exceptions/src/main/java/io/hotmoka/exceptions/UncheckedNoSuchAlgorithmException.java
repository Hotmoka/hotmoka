/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.exceptions;

import java.security.NoSuchAlgorithmException;

/**
 * An unchecked no such algorithm exception.
 */
@SuppressWarnings("serial")
public class UncheckedNoSuchAlgorithmException extends RuntimeException {

	public UncheckedNoSuchAlgorithmException(String message) {
		super(new NoSuchAlgorithmException(message));
	}

	public UncheckedNoSuchAlgorithmException(NoSuchAlgorithmException cause) {
		super(cause);
	}

	public UncheckedNoSuchAlgorithmException(String message, NoSuchAlgorithmException cause) {
		super(message, cause);
	}

	@Override
	public UncheckedNoSuchAlgorithmException getCause() {
		return (UncheckedNoSuchAlgorithmException) super.getCause();
	}

	public interface SupplierWithException<T> {
		T get() throws NoSuchAlgorithmException;
	}

	public static <T> T wraps(SupplierWithException<T> supplier) {
		try {
			return supplier.get();
		}
		catch (NoSuchAlgorithmException e) {
			throw new UncheckedNoSuchAlgorithmException(e);
		}
	}

	public interface CodeWithException {
		void apply() throws NoSuchAlgorithmException;
	}

	public static void wraps(CodeWithException code) {
		try {
			code.apply();
		}
		catch (NoSuchAlgorithmException e) {
			throw new UncheckedNoSuchAlgorithmException(e);
		}
	}
}