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

import java.nio.file.Path;

import io.hotmoka.moka.internal.keys.Bind;

/**
 * The output of the moka keys bind command.
 */
public interface KeysBindOutput {

	/**
	 * Yields the output of the command from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the output of the command
	 */
	static KeysBindOutput of(String json) {
		return Bind.Output.of(json);
	}

	/**
	 * Yields the output of the command as a string.
	 * 
	 * @param file the path of the account that has been written
	 * @param json true if and only if the string must be in JSON format
	 * @return the output of the command as a string
	 */
	String toString(Path file, boolean json);
}