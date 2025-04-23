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

package io.hotmoka.moka.internal.accounts;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show", description = "Show information about an account.")
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the storage reference of the account", converter = StorageReferenceOfAccountOptionConverter.class)
    private StorageReference account;

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	public final static int MAX_PRINTED_KEY = 200;

	@Override
	protected void execute() throws CommandException {
		execute(this::body);
	}

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		SignatureAlgorithm signature;

		try {
			signature = SignatureHelpers.of(remote).signatureAlgorithmFor(account);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The account uses a non-available signature algorithm", e);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("The account object does not exist in the node");
		}

		var takamakaCode = remote.getTakamakaCode();
		BigInteger balance;

		try {
			balance = remote.runInstanceMethodCallTransaction(
					TransactionRequests.instanceViewMethodCall(account, _100_000, takamakaCode, MethodSignatures.BALANCE, account))
					.orElseThrow(() -> new NodeException(MethodSignatures.BALANCE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.BALANCE, NodeException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not access the balance of the account", e);
		}

		String publicKeyBase64;
		try {
			publicKeyBase64 = remote.runInstanceMethodCallTransaction(
					TransactionRequests.instanceViewMethodCall(account, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, account))
					.orElseThrow(() -> new NodeException(MethodSignatures.PUBLIC_KEY + " should not return void"))
					.asReturnedString(MethodSignatures.PUBLIC_KEY, NodeException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not access the balance of the account", e);
		}

		var accountInfo = new AccountInfo(balance, signature, publicKeyBase64);

		if (json())
			System.out.println(new Gson().toJsonTree(accountInfo));
		else
			System.out.println(accountInfo);
	}
}