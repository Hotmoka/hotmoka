package io.hotmoka.moka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.internal.accounts.KeysInfo;

public class CreateKeyTests extends AbstractMokaTest {

	@Test
	@DisplayName("the created pem file contains the entropy of the key")
	public void createdPEMHoldsEntropyOfKey(@TempDir Path dir) throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		var password = "mypassword";
		String result = runWithRedirectedStandardOutput("accounts create-key --dir=" + dir + " --signature=" + signature + " --password=" + password + " --show-private --json");
		KeysInfo actual = new Gson().fromJson(result, KeysInfo.class);
		var entropy = Entropies.load(dir.resolve(actual.publicKeyBase58 + ".pem"));
		var keys = entropy.keys(password, signature);
		var expected = new KeysInfo(signature, keys, true);
		assertEquals(expected, actual);
	}
}