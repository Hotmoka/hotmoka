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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

/**
 */
public abstract class Check {

	public static <R> R checkNoSuchAlgorithmException(Supplier<R> supplier) throws NoSuchAlgorithmException {
		try {
			return supplier.get();
		}
		catch (UncheckedNoSuchAlgorithmException e) {
			throw e.getCause();
		}
	}

	public static <R> R checkIOException(Supplier<R> supplier) throws IOException {
		try {
			return supplier.get();
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public static <R> R checkInterruptedException(Supplier<R> supplier) throws InterruptedException {
		try {
			return supplier.get();
		}
		catch (UncheckedInterruptedException e) {
			throw e.getCause();
		}
	}

	public static <R> R checkNoSuchAlgorithmExceptionIOException(Supplier<R> supplier) throws NoSuchAlgorithmException, IOException {
		try {
			return supplier.get();
		}
		catch (UncheckedNoSuchAlgorithmException e) {
			throw e.getCause();
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
}
