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

/**
 */
public abstract class CheckRunnable {

	public static void checkNoSuchAlgorithmException(Runnable runnable) throws NoSuchAlgorithmException {
		try {
			runnable.run();
		}
		catch (UncheckedNoSuchAlgorithmException e) {
			throw e.getCause();
		}
	}

	public static void checkIOException(Runnable runnable) throws IOException {
		try {
			runnable.run();
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public static void checkInterruptedException(Runnable runnable) throws InterruptedException {
		try {
			runnable.run();
		}
		catch (UncheckedInterruptedException e) {
			throw e.getCause();
		}
	}

	public static void checkNoSuchAlgorithmExceptionIOException(Runnable runnable) throws NoSuchAlgorithmException, IOException {
		try {
			runnable.run();
		}
		catch (UncheckedNoSuchAlgorithmException e) {
			throw e.getCause();
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
}