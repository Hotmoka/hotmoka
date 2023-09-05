/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.tools.internal.moka;

import java.nio.file.Path;

import io.hotmoka.crypto.BIP39Dictionaries;
import io.hotmoka.crypto.BIP39Mnemonics;
import io.hotmoka.node.Accounts;
import picocli.CommandLine.Command;

@Command(name = "import-account",
	description = "Imports an account",
	showDefaultValues = true)
public class ImportAccount extends AbstractCommand {

	@Override
	protected void execute() throws Exception {
		System.out.println("Insert the 36 words of the passphrase of the account to import:");
		String[] words = new String[36];
		int pos = 0;
		while (pos < words.length) {
			words[pos] = ask("word #" + (pos + 1) + ": ");
			if (BIP39Dictionaries.ENGLISH_DICTIONARY.getAllWords().anyMatch(words[pos]::equals))
				pos++;
			else
				System.out.println("The word \"" + words[pos] + "\" does not exist in the BIP39 English dictionary. Try again.");
		}

		var account = BIP39Mnemonics.of(words).toAccount(Accounts::of);
		System.out.println("The account " + account + " has been imported.");
		Path fileName = account.dump();
        System.out.println("Its entropy has been saved into the file \"" + fileName + "\".");
	}
}