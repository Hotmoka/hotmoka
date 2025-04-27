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

package io.hotmoka.moka.api.nodes.disk;

import java.io.PrintStream;
import java.net.URI;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The output of the {@code moka nodes disk init} command.
 */
@Immutable
public interface NodesDiskInitOutput {

	/**
	 * Yields the URI of the published node service.
	 * 
	 * @return the URI of the published node service
	 */
	URI getURI();

	/**
	 * Yields the reference to the gamete of the initialized node.
	 * 
	 * @return the reference to the gamete of the initialized node
	 */
	StorageReference getGamete();

	/**
	 * Prints this output as a string.
	 * 
	 * @param out the destination print stream
	 * @param json true if and only if the string must be in JSON format
	 */
	void println(PrintStream out, boolean json);
}