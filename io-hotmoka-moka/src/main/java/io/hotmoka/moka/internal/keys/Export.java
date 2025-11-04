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

package io.hotmoka.moka.internal.keys;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.BIP39Dictionaries;
import io.hotmoka.crypto.BIP39Mnemonics;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.BIP39Mnemonic;
import io.hotmoka.moka.KeysExportOutputs;
import io.hotmoka.moka.api.keys.KeysExportOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.json.KeysExportOutputJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "export", header = "Export a key pair file as BIP39 words.", showDefaultValues = true)
public class Export extends AbstractMokaCommand {

	@Parameters(index = "0", description = "the path of the file holding the key pair")
    private Path keys;

	@Override
	protected void execute() throws CommandException {
		try {
			var entropy = Entropies.load(this.keys);
			var mnemonics = BIP39Mnemonics.of(entropy.getEntropyAsBytes(), BIP39Dictionaries.ENGLISH_DICTIONARY);

			report(new Output(mnemonics), KeysExportOutputs.Encoder::new);
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + keys + "\"", e);
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements KeysExportOutput {
		private final String[] bip39Words;

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(KeysExportOutputJson json) throws InconsistentJsonException {
			this.bip39Words = json.getBip39Words().toArray(String[]::new);
			if (bip39Words.length != 12)
				throw new InconsistentJsonException("Hotmoka keys are represented by 12 BIP39 words, but " + bip39Words.length + " words have been provided instead");
		}

		private Output(BIP39Mnemonic bip39Words) {
			this.bip39Words = bip39Words.stream().toArray(String[]::new);
		}

		@Override
		public Stream<String> getBip39Words() {
			return Stream.of(bip39Words);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("The following BIP39 words represent the key pair:\n");
        	for (int pos = 0; pos < bip39Words.length; pos++)
        		sb.append(String.format("%2d: %s\n", pos + 1, bip39Words[pos]));

        	return sb.toString();
		}
	}
}