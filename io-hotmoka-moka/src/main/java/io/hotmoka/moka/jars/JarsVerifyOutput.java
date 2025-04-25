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

package io.hotmoka.moka.jars;

import java.io.PrintStream;
import java.util.stream.Stream;

import io.hotmoka.moka.internal.jars.Verify;

/**
 * The output of the moka jars verify command.
 */
public interface JarsVerifyOutput {

	/**
	 * Yields the output of the command from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the output of the command
	 */
	static JarsVerifyOutput of(String json) {
		return Verify.Output.of(json);
	}

	/**
	 * Yields the errors in this output.
	 * 
	 * @return the errors in this output
	 */
	Stream<ErrorJSON> getErrors();

	/**
	 * Prints this output as a string.
	 * 
	 * @param out the destination print stream
	 * @param json true if and only if the string must be in JSON format
	 */
	void println(PrintStream out, boolean json);

	class ErrorJSON {
		public final String where;
		public final String message;

		public ErrorJSON(io.hotmoka.verification.api.Error error) {
			this.where = error.getWhere();
			this.message = error.getMessage();
		}

		@Override
		public String toString() {
			return where + ": " + message;
		}
	}
}