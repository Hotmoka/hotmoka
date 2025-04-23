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

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.internal.keys.KeysInfo;

/**
 * Tests for the moka keys command.
 */
public class KeysTests extends AbstractMokaTest {

	@Test
	@DisplayName("the creation of a key generates a pem file that contains the entropy of the key")
	public void createKeyPEMHoldsEntropyOfKey(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var name = "mykey.pem";
		String result = runWithRedirectedStandardOutput("keys create --dir=" + dir + " --name=" + name + " --signature=" + signature + " --password=" + password + " --show-private --json");
		KeysInfo actual = new Gson().fromJson(result, KeysInfo.class);
		var entropy = Entropies.load(dir.resolve(name));
		var keys = entropy.keys(password, signature);
		var expected = new KeysInfo(signature, keys, true);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("information about a key pem file is correctly reported")
	public void showKeyFromPEMWorksCorrectly(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.sha256dsa();
		var password = "mypassword";
		var name = "mykey.pem";
		runWithRedirectedStandardOutput("keys create --dir=" + dir + " --name=" + name + " --signature=" + signature + " --password=" + password + " --show-private --json");
		Path key = dir.resolve(name);
		var entropy = Entropies.load(key);
		var keys = entropy.keys(password, signature);
		var expected = new KeysInfo(signature, keys, true);
		String result = runWithRedirectedStandardOutput("keys show " + key + " --signature=" + signature + " --password=" + password + " --show-private --json");
		KeysInfo actual = new Gson().fromJson(result, KeysInfo.class);
		assertEquals(expected, actual);
	}
}