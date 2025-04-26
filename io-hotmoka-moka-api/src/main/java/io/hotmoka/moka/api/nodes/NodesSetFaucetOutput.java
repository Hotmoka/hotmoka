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

package io.hotmoka.moka.api.nodes;

import java.io.PrintStream;
import java.math.BigInteger;

/**
 * The output of the {@code moka nodes set-faucet} command.
 */
public interface NodesSetFaucetOutput {

	/**
	 * Prints this output as a string.
	 * 
	 * @param out the destination print stream
	 * @param threshold the value of the threshold set for the faucet
	 * @param json true if and only if the string must be in JSON format
	 */
	void println(PrintStream out, BigInteger threshold, boolean json);
}