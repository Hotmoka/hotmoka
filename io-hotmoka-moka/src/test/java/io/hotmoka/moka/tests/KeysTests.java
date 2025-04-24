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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.MokaNew;
import io.hotmoka.moka.keys.KeysCreateOutput;
import io.hotmoka.moka.keys.KeysExportOutput;
import io.hotmoka.moka.keys.KeysImportOutput;
import io.hotmoka.moka.keys.KeysShowOutput;
import io.hotmoka.node.StorageValues;

/**
 * Tests for the moka keys command.
 */
public class KeysTests extends AbstractMokaTest {

	@Test
	@DisplayName("[moka keys create] the creation of a key pair file contains the entropy of the keys")
	public void createdKeyPairHoldsEntropyOfKey(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var name = "mykey.pem";
		var actual = KeysCreateOutput.of(MokaNew.run("keys create --dir=" + dir + " --name=" + name + " --signature=" + signature + " --password=" + password + " --show-private --json"));
		var entropy = Entropies.load(dir.resolve(name));
		var keys = entropy.keys(password, signature);
		var expected = KeysCreateOutput.of(signature, keys, true);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("[moka keys create] information about a key pair file is correctly reported")
	public void showKeyFromKeyPairWorksCorrectly(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var name = "mykey.pem";
		MokaNew.run("keys create --dir=" + dir + " --name=" + name + " --signature=" + signature + " --password=" + password);
		Path key = dir.resolve(name);
		var entropy = Entropies.load(key);
		var keys = entropy.keys(password, signature);
		var expected = KeysShowOutput.of(signature, keys, true);
		var actual = KeysShowOutput.of(MokaNew.run("keys show " + key + " --signature=" + signature + " --password=" + password + " --show-private --json"));
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("[moka keys export/import] information about a key pair file of an account is correctly exported and imported")
	public void exportImportKeyPairOfAccountWorksCorrectly(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var expectedReference = StorageValues.reference("3e79b7ee8d8ef89bc6768c1c985ff09f60e167c515ea6c49236d3e22c2070089#0");
		// we name the key pair file as a storage reference, so that it is already the key pair file of an account
		MokaNew.run("keys create --dir=" + dir + " --name=" + expectedReference + ".pem --signature=" + signature + " --password=" + password);
		var expectedEntropy = Entropies.load(dir.resolve(expectedReference + ".pem"));
		var keysExportOutput = KeysExportOutput.of(MokaNew.run("keys export " + expectedReference + " --dir=" + dir + " --json"));
		String spaceSeparatedWords = Stream.of(keysExportOutput.getBip39Words()).collect(Collectors.joining(" "));
		// we re-import the key file into a difference directory, so that it does not override the original file
		Path copy = dir.resolve("copy");
		Files.createDirectories(copy);
		var keysImportOutput = KeysImportOutput.of(MokaNew.run("keys import " + spaceSeparatedWords + " --dir=" + copy + " --json"));
		var actualReference = StorageValues.reference(keysImportOutput.getReference());
		var actualEntropy = Entropies.load(copy.resolve(actualReference + ".pem"));

		// both the accounts references and their entropies must match
		assertEquals(expectedReference, actualReference);
		assertEquals(expectedEntropy, actualEntropy);
	}
}