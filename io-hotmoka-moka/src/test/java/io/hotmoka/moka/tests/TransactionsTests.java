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
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.moka.Moka;
import io.hotmoka.moka.NodesManifestAddressOutputs;
import io.hotmoka.moka.TransactionsShowOutputs;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;

/**
 * Tests for the {@code moka transactions} commands.
 */
public class TransactionsTests extends AbstractMokaTestWithNode {
	
	@Test
	@DisplayName("[moka transactions show] showing the transaction that created the manifest of the node works")
	public void transactionsShowWorks() throws Exception {
		var manifest = NodesManifestAddressOutputs.from(Moka.nodesManifestAddress("--json --uri=ws://localhost:" + PORT)).getManifest();
		var transactionsShowOutput = TransactionsShowOutputs.from(Moka.transactionsShow(manifest.getTransaction() + " --json --uri=ws://localhost:" + PORT));

		// the transaction that created the manifest requested the execution of a constructor (that of the manifest)
		assertTrue(transactionsShowOutput.getRequest() instanceof ConstructorCallTransactionRequest);

		// the transaction that created the manifest was successful
		if (transactionsShowOutput.getResponse() instanceof ConstructorCallTransactionSuccessfulResponse cctsr)
			// the successful result actually created the manifest
			assertEquals(cctsr.getNewObject(), manifest);
		else
			fail();
	}
}