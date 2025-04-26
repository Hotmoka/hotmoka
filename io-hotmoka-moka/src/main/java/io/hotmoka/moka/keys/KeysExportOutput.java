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

package io.hotmoka.moka.keys;

import java.io.PrintStream;
import java.util.stream.Stream;

import io.hotmoka.moka.internal.keys.Export;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The output of the moka {@code keys export} command.
 */
public interface KeysExportOutput {

	/**
	 * Yields the output of the command from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the output of the command
	 */
	static KeysExportOutput of(String json) {
		return Export.Output.of(json);
	}

	/**
	 * Yields the BIP39 words in the output of the command.
	 * 
	 * @return the BIP39 words in the output of the command
	 */
	Stream<String> getBip39Words();

	/**
	 * Prints this output as a string.
	 * 
	 * @param out the destination print stream
	 * @param reference the reference of the account that is being exported
	 * @param json true if and only if the string must be in JSON format
	 */
	void println(PrintStream out, StorageReference reference, boolean json);
}