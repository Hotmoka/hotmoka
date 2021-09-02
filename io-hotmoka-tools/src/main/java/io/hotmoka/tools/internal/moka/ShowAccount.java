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

import java.math.BigInteger;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.Account;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "show-account",
	description = "Shows an account",
	showDefaultValues = true)
public class ShowAccount extends AbstractCommand {

	@Parameters(index = "0", description = "the reference of the account to show")
    private String reference;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--balances" }, description = "shows also the balances of the account")
	private boolean balances;

	@Override
	protected void execute() throws Exception {
		Account account = new Account(reference);
		System.out.println("reference: " + account.reference);
		System.out.println("entropy: " + account.entropyAsHex());

		if (balances)
			showBalances(account);

		System.out.println();
		printPassphrase(account);
	}

	private void showBalances(Account account) throws Exception {
		try (Node node = RemoteNode.of(remoteNodeConfig(url))) {
			TransactionReference takamakaCode = node.getTakamakaCode();
			StorageReference reference = account.reference;
			BigInteger balance = ((BigIntegerValue) node.runInstanceMethodCallTransaction(
	            new InstanceMethodCallTransactionRequest(reference, _100_000, takamakaCode, MethodSignature.BALANCE, reference))).value;
			System.out.println("balance: " + balance + " Panareas");
			BigInteger balanceRed = ((BigIntegerValue) node.runInstanceMethodCallTransaction(
	            new InstanceMethodCallTransactionRequest(reference, _100_000, takamakaCode, MethodSignature.BALANCE_RED, reference))).value;
			System.out.println("balance red: " + balanceRed + " Panareas");
		}
	}
}