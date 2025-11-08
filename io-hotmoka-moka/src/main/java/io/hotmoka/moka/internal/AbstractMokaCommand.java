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

package io.hotmoka.moka.internal;

import io.hotmoka.cli.AbstractCommandWithJsonOutput;
import io.hotmoka.node.api.transactions.TransactionReference;
import picocli.CommandLine.Help.Ansi;

/**
 * Shared code among the commands that do not need to connect to a remote Hotmoka node.
 */
public abstract class AbstractMokaCommand extends AbstractCommandWithJsonOutput {

	/**
	 * Styles the given transaction reference in transaction reference style.
	 * 
	 * @param reference the reference
	 * @return the styled text
	 */
	protected static String asTransactionReference(TransactionReference reference) {
		return Ansi.AUTO.string("@|fg(5;3;2) " + reference + "|@");
	}
}