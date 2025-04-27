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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.KeysExportOutputs;
import io.hotmoka.moka.KeysImportOutputs;
import io.hotmoka.moka.KeysShowOutputs;
import io.hotmoka.moka.MokaNew;
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
		var actual = KeysCreateOutputs.from(MokaNew.keysCreate("--dir=" + dir + " --name=" + name + " --signature=" + signature + " --password=" + password + " --show-private --json"));
		var entropy = Entropies.load(dir.resolve(name));
		var keys = entropy.keys(password, signature);
		byte[] publicKeyBytes = signature.encodingOf(keys.getPublic());
		byte[] privateKeyBytes = signature.encodingOf(keys.getPrivate());
		var concatenated = new byte[privateKeyBytes.length + publicKeyBytes.length];
		System.arraycopy(privateKeyBytes, 0, concatenated, 0, privateKeyBytes.length);
		System.arraycopy(publicKeyBytes, 0, concatenated, privateKeyBytes.length, publicKeyBytes.length);
		String expectedConcatenatedBase64 = Base64.toBase64String(concatenated);
		String expectedPublicKeyBase58 = Base58.toBase58String(publicKeyBytes);
		String expectedPublicKeyBase64 = Base64.toBase64String(publicKeyBytes);
		byte[] sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);
		String expectedTendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
		String expectedPrivateKeyBase58 = Base58.toBase58String(privateKeyBytes);
		String expectedPrivateKeyBase64 = Base64.toBase64String(privateKeyBytes);
		assertEquals(expectedConcatenatedBase64, actual.getConcatenatedBase64().get());
		assertEquals(expectedPublicKeyBase58, actual.getPublicKeyBase58());
		assertEquals(expectedPublicKeyBase64, actual.getPublicKeyBase64());
		assertEquals(expectedTendermintAddress, actual.getTendermintAddress());
		assertEquals(expectedPrivateKeyBase58, actual.getPrivateKeyBase58().get());
		assertEquals(expectedPrivateKeyBase64, actual.getPrivateKeyBase64().get());
	}

	@Test
	@DisplayName("[moka keys create] information about a key pair file is correctly reported")
	public void showKeyFromKeyPairWorksCorrectly(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var name = "mykey.pem";
		MokaNew.keysCreate("--dir=" + dir + " --name=" + name + " --signature=" + signature + " --password=" + password);
		Path key = dir.resolve(name);
		var actual = KeysShowOutputs.from(MokaNew.keysShow(key + " --signature=" + signature + " --password=" + password + " --show-private --json"));
		var entropy = Entropies.load(key);
		var keys = entropy.keys(password, signature);
		byte[] publicKeyBytes = signature.encodingOf(keys.getPublic());
		byte[] privateKeyBytes = signature.encodingOf(keys.getPrivate());
		var concatenated = new byte[privateKeyBytes.length + publicKeyBytes.length];
		System.arraycopy(privateKeyBytes, 0, concatenated, 0, privateKeyBytes.length);
		System.arraycopy(publicKeyBytes, 0, concatenated, privateKeyBytes.length, publicKeyBytes.length);
		String expectedConcatenatedBase64 = Base64.toBase64String(concatenated);
		String expectedPublicKeyBase58 = Base58.toBase58String(publicKeyBytes);
		String expectedPublicKeyBase64 = Base64.toBase64String(publicKeyBytes);
		byte[] sha256HashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKeyBytes);
		String expectedTendermintAddress = Hex.toHexString(sha256HashedKey, 0, 20).toUpperCase();
		String expectedPrivateKeyBase58 = Base58.toBase58String(privateKeyBytes);
		String expectedPrivateKeyBase64 = Base64.toBase64String(privateKeyBytes);
		assertEquals(expectedConcatenatedBase64, actual.getConcatenatedBase64().get());
		assertEquals(expectedPublicKeyBase58, actual.getPublicKeyBase58());
		assertEquals(expectedPublicKeyBase64, actual.getPublicKeyBase64());
		assertEquals(expectedTendermintAddress, actual.getTendermintAddress());
		assertEquals(expectedPrivateKeyBase58, actual.getPrivateKeyBase58().get());
		assertEquals(expectedPrivateKeyBase64, actual.getPrivateKeyBase64().get());
	}

	@Test
	@DisplayName("[moka keys export/import] information about a key pair file of an account is correctly exported and imported")
	public void exportImportKeyPairOfAccountWorksCorrectly(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var expectedReference = StorageValues.reference("3e79b7ee8d8ef89bc6768c1c985ff09f60e167c515ea6c49236d3e22c2070089#0");
		// we name the key pair file as a storage reference, so that it is already the key pair file of an account
		MokaNew.keysCreate("--dir=" + dir + " --name=" + expectedReference + ".pem --signature=" + signature + " --password=" + password);
		var expectedEntropy = Entropies.load(dir.resolve(expectedReference + ".pem"));
		var keysExportOutput = KeysExportOutputs.from(MokaNew.keysExport(expectedReference + " --dir=" + dir + " --json"));
		String spaceSeparatedWords = keysExportOutput.getBip39Words().collect(Collectors.joining(" "));
		// we re-import the key file into a difference directory, so that it does not override the original file
		Path copy = dir.resolve("copy");
		Files.createDirectories(copy);
		var keysImportOutput = KeysImportOutputs.from(MokaNew.keysImport(spaceSeparatedWords + " --dir=" + copy + " --json"));
		var actualReference = keysImportOutput.getReference();
		var actualEntropy = Entropies.load(copy.resolve(actualReference + ".pem"));

		// both the accounts references and their entropies must match
		assertEquals(expectedReference, actualReference);
		assertEquals(expectedEntropy, actualEntropy);
	}
}