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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.MokaNew;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;

/**
 * Tests for the moka nodes command.
 */
public class NodesTests extends AbstractMokaTestWithNode {

	@Test
	@DisplayName("[moka nodes faucet] the description of the gamete account is correct")
	public void openingOfFaucetWorks() throws Exception {
		var takamakaCode = node.getTakamakaCode();

		// we read the current threshold for the faucet
		BigInteger currentMaxFaucet = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(gamete, BigInteger.valueOf(100_000L), takamakaCode, MethodSignatures.GET_MAX_FAUCET, gamete))
				.orElseThrow(() -> new CommandException(MethodSignatures.GET_MAX_FAUCET + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_MAX_FAUCET, CommandException::new);

		// we add 10000 to the threshold
		BigInteger expected = currentMaxFaucet.add(BigInteger.valueOf(10_000L));
		MokaNew.nodesFaucet(expected + " --dir=" + dir + " --password=" + passwordOfGamete);

		// we read the current threshold again: it should have been increased by 10000
		BigInteger actual = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(gamete, BigInteger.valueOf(100_000L), takamakaCode, MethodSignatures.GET_MAX_FAUCET, gamete))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_MAX_FAUCET + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_MAX_FAUCET, CommandException::new);

		assertEquals(expected, actual);
	}
}