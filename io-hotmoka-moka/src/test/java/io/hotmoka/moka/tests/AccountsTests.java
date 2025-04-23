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

import java.math.BigInteger;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.internal.accounts.AccountInfo;

/**
 * Tests for the moka nodes command.
 */
public class AccountsTests extends AbstractMokaTestWithNode {
	
	@Test
	@DisplayName("[moka accounts show] the description of the gamete account is correct")
	public void descriptionOfGameteIsCorrect(@TempDir Path dir) throws Exception {
		// TODO: this should actually be applied to a brand account created in the test from new keys
		String result = runWithRedirectedStandardOutput("accounts show " + gamete + " --json");
		AccountInfo actual = new Gson().fromJson(result, AccountInfo.class);
		var ed25519 = SignatureAlgorithms.ed25519();
		AccountInfo expected = new AccountInfo(BigInteger.valueOf(1000000000000000000L), ed25519, Base64.toBase64String(ed25519.encodingOf(keysOfGamete.getPublic())));
		assertEquals(expected, actual);
	}
}