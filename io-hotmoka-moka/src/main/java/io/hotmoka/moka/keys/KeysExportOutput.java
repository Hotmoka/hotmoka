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

import io.hotmoka.moka.internal.keys.Export;

/**
 * The output of the moka keys export command.
 */
public interface KeysExportOutput {

	/**
	 * Yields the output of the command from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the output of the command
	 */
	static KeysExportOutput of(String json) {
		return new Export.Output(json);
	}

	/**
	 * Yields the BIP39 words in the output of the command
	 */
	String[] getBip39Words();

	/**
	 * Yields the output of the command as a string.
	 * 
	 * @param json true if and only if the string must be in JSON format
	 * @return the output of the command as a string
	 */
	String toString(boolean json);
}