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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.moka.AccountsCreateOutputs;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.KeysCreateOutputs;
import io.hotmoka.moka.Moka;
import io.hotmoka.moka.ObjectsCallOutputs;
import io.hotmoka.moka.ObjectsCreateOutputs;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * Tests for the {@code moka objects} commands.
 */
public class ObjectsTests extends AbstractMokaTestWithNode {
	
	@Test
	@DisplayName("[moka objects create] the creation of new object works")
	public void objectCreationWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfNewAccount = "abcde";

		// create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// create a new account with that key pair, letting the gamete pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate(gamete + " 1000000000000 " + keyCreateOutputs.getFile() + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		StorageReference account = accountsCreateOutput.getAccount().get();
		// install the jar for TicTacToe contract
		var ticTacToeJar = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + io.hotmoka.constants.Constants.HOTMOKA_VERSION + "-tictactoe.jar");
		var jarsInstallOutput = JarsInstallOutputs.from(Moka.jarsInstall(account + " " + ticTacToeJar + " --dir=" + dir + " --json --password-of-payer=" + passwordOfNewAccount + " --uri=ws://localhost:" + PORT));
		// create a TicTacToe object, letting the new account pay for it
		var objectsCreateOutput = ObjectsCreateOutputs.from(Moka.objectsCreate(account + " io.hotmoka.examples.tictactoe.TicTacToe --password-of-payer=" + passwordOfNewAccount + " --classpath=" + jarsInstallOutput.getJar().get() + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));

		// the object has been actually created
		assertTrue(objectsCreateOutput.getObject().isPresent());

		// the object has been created by the transaction reported in the creation output
		assertEquals(objectsCreateOutput.getTransaction(), objectsCreateOutput.getObject().get().getTransaction());

		// the response reported in the creation output is a successful constructor call response
		TransactionResponse response = node.getResponse(objectsCreateOutput.getTransaction());
		assertTrue(response instanceof ConstructorCallTransactionSuccessfulResponse);
		ConstructorCallTransactionSuccessfulResponse successfulResponse = (ConstructorCallTransactionSuccessfulResponse) response;

		// that response has actually created the object
		assertEquals(objectsCreateOutput.getObject().get(), successfulResponse.getNewObject());
	}

	@Test
	@DisplayName("[moka objects call] the call to a method of an object works")
	public void objectCallWorks() throws Exception {
		var signature = SignatureAlgorithms.ed25519();
		String passwordOfNewAccount = "abcde";

		// create a key pair
		var keyCreateOutputs = KeysCreateOutputs.from(Moka.keysCreate("--signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir));
		// create a new account with that key pair, letting the gamete pay for it
		var accountsCreateOutput = AccountsCreateOutputs.from(Moka.accountsCreate(gamete + " 1000000000000 " + keyCreateOutputs.getFile() + " --password-of-payer=" + passwordOfGamete + " --dir=" + dir + " --signature=" + signature + " --password=" + passwordOfNewAccount + " --json --output-dir=" + dir + " --uri=ws://localhost:" + PORT));
		StorageReference account = accountsCreateOutput.getAccount().get();
		// install the jar for TicTacToe contract
		var ticTacToeJar = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + io.hotmoka.constants.Constants.HOTMOKA_VERSION + "-tictactoe.jar");
		var jarsInstallOutput = JarsInstallOutputs.from(Moka.jarsInstall(account + " " + ticTacToeJar + " --dir=" + dir + " --json --password-of-payer=" + passwordOfNewAccount + " --uri=ws://localhost:" + PORT));
		// create a TicTacToe object, letting the new account pay for it
		var objectsCreateOutput = ObjectsCreateOutputs.from(Moka.objectsCreate(account + " io.hotmoka.examples.tictactoe.TicTacToe --password-of-payer=" + passwordOfNewAccount + " --classpath=" + jarsInstallOutput.getJar().get() + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
		var ticTacToe = objectsCreateOutput.getObject().get();
		// call method play(100, 1, 1) of ticTacToe
		var playCallOutput = ObjectsCallOutputs.from(Moka.objectsCall(account + " io.hotmoka.examples.tictactoe.TicTacToe play 100 1 1 --classpath=" + jarsInstallOutput.getJar().get() + " --password-of-payer=" + passwordOfNewAccount + " --receiver=" + ticTacToe + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
		// the call generates no error
		assertTrue(playCallOutput.getErrorMessage().isEmpty());
		// call method at(1, 1) of ticTacToe
		var atCallOutput = ObjectsCallOutputs.from(Moka.objectsCall(account + " io.hotmoka.examples.tictactoe.TicTacToe at 1 1 --classpath=" + jarsInstallOutput.getJar().get() + " --password-of-payer=" + passwordOfNewAccount + " --receiver=" + ticTacToe + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
		// the call generates no error
		assertTrue(atCallOutput.getErrorMessage().isEmpty());
		// there is a result of the last call: the tile CROSS
		assertTrue(atCallOutput.getResult().isPresent());
		assertTrue(atCallOutput.getResult().get() instanceof StorageReference);
		var tile = atCallOutput.getResult().get().asReference(__ -> new RuntimeException());
		// call toString() on the tile
		var toStringCallOutput = ObjectsCallOutputs.from(Moka.objectsCall(account + " io.hotmoka.examples.tictactoe.TicTacToe$Tile toString --classpath=" + jarsInstallOutput.getJar().get() + " --password-of-payer=" + passwordOfNewAccount + " --receiver=" + tile + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
		// the result is the String "X"
		assertTrue(toStringCallOutput.getResult().isPresent());
		assertEquals("X", toStringCallOutput.getResult().get().asString(__ -> new RuntimeException()));
		// we try to play again with the same account
		var playAgainCallOutput = ObjectsCallOutputs.from(Moka.objectsCall(account + " io.hotmoka.examples.tictactoe.TicTacToe play 100 2 1 --classpath=" + jarsInstallOutput.getJar().get() + " --password-of-payer=" + passwordOfNewAccount + " --receiver=" + ticTacToe + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));
		// this time the call fails with an error
		assertTrue(playAgainCallOutput.getErrorMessage().isPresent());
		assertTrue(playAgainCallOutput.getErrorMessage().get().startsWith(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME + ": you cannot play against yourself"));
	}
}