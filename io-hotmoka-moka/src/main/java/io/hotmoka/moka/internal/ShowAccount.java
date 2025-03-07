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

package io.hotmoka.moka.internal;

import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.util.function.Function;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.Hex;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "show-account",
	description = "Show an account",
	showDefaultValues = true)
public class ShowAccount extends AbstractCommand {

	@Parameters(index = "0", description = "the reference of the account to show")
    private String reference;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = { "--balances" }, description = "show the balances of the account")
	private boolean balances;

	@Option(names = { "--keys" }, description = "show the private and public key of the account")
	private boolean keys;

	@Option(names = { "--password" }, description = "the password of the account; if not specified, it will be asked interactively")
	private String password;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Override
	protected void execute() throws Exception {
		if (keys)
			password = ensurePassword(password, "the account", interactive, false);

		var account = Accounts.of(reference, s -> new CommandException("The reference " + reference + " is not a valid storage reference for an account: " + s));
		System.out.println("reference: " + account.getReference());
		System.out.println("entropy: " + Base64.toBase64String(account.getEntropyAsBytes()));

		if (balances || keys) {
			try (var node = RemoteNodes.of(uri, 10_000)) {
				if (balances)
					showBalances(account, node);

				if (keys)
					showKeys(account, node);
			}
		}

		System.out.println();
		printPassphrase(account);
	}

	private void showKeys(Account account, Node node) throws Exception {
		var algorithm = SignatureHelpers.of(node).signatureAlgorithmFor(account.getReference());
		System.out.println("signature type: " + algorithm);
		KeyPair keys = account.keys(password, algorithm);
		byte[] privateKey = algorithm.encodingOf(keys.getPrivate());
		System.out.println("private key Base58: " + Base58.encode(privateKey));
		System.out.println("private key Base64: " + Base64.toBase64String(privateKey));
		byte[] publicKey = algorithm.encodingOf(keys.getPublic());
		System.out.println("public key Base58: " + Base58.encode(publicKey));
		System.out.println("public key Base64: " + Base64.toBase64String(publicKey));
		byte[] concatenated = new byte[privateKey.length + publicKey.length];
		System.arraycopy(privateKey, 0, concatenated, 0, privateKey.length);
		System.arraycopy(publicKey, 0, concatenated, privateKey.length, publicKey.length);
		System.out.println("Concatenated private+public key Base64: " + Base64.toBase64String(concatenated));
		byte[] hashedKey = HashingAlgorithms.sha256().getHasher(Function.identity()).hash(publicKey);
		String hex = Hex.toHexString(hashedKey, 0, 20).toUpperCase();
		System.out.println("Tendermint-like address: " + hex);
	}

	private void showBalances(Account account, Node node) throws Exception {
		var takamakaCode = node.getTakamakaCode();
		StorageReference reference = account.getReference();
		BigInteger balance = ((BigIntegerValue) node.runInstanceMethodCallTransaction(
			TransactionRequests.instanceViewMethodCall(reference, _100_000, takamakaCode, MethodSignatures.BALANCE, reference))
			.orElseThrow(() -> new CommandException(MethodSignatures.BALANCE + " should not return void"))).getValue();
		System.out.println("balance: " + balance);
	}
}