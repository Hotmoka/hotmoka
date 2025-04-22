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

package io.hotmoka.moka.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import io.hotmoka.moka.MokaNew;
import io.hotmoka.testing.AbstractLoggedTests;

/**
 * Shared code of the tests for the moka tool.
 */
public abstract class AbstractMokaTest extends AbstractLoggedTests {

	/**
	 * Runs the given command-line with the moka tool. It performs as calling "moka command".
	 * 
	 * @param command the command to run with moka
	 * @return the standard output of moka
	 * @throws IOException if the construction of the return value failed
	 */
	protected static String runWithRedirectedStandardOutput(String command) throws IOException {
		var originalOut = System.out;

		try (var baos = new ByteArrayOutputStream(); var out = new PrintStream(baos)) {
			System.setOut(out);
			MokaNew.main(command);
			return new String(baos.toByteArray());
		}
		finally {
			System.setOut(originalOut);
		}
	}

	/**
	 * Runs the given command-line with the moka tool. It performs as calling "moka command".
	 * 
	 * @param command the command to run with moka
	 * @param in the stream to use as standard input of the command
	 * @return the standard output of moka
	 * @throws IOException if the construction of the return value failed
	 */
	protected static String runWithRedirectedStandardOutput(String command, InputStream in) throws IOException {
		var originalIn = System.in;
		var originalOut = System.out;

		try (var baos = new ByteArrayOutputStream(); var out = new PrintStream(baos)) {
			System.setIn(in);
			System.setOut(out);
			MokaNew.main(command);
			return new String(baos.toByteArray());
		}
		finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
		}
	}
}