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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.moka.AccountsShowOutputs;
import io.hotmoka.moka.api.accounts.AccountsShowOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.AccountsShowOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show", header = "Show information about an account.", showDefaultValues = true)
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the storage reference of the account", converter = StorageReferenceOptionConverter.class)
    private StorageReference account;

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	public final static int MAX_PRINTED_KEY = 200;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException {
		try {
		SignatureAlgorithm signature;
		try {
			signature = SignatureHelpers.of(remote).signatureAlgorithmFor(account);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The account " + account + " uses a non-available signature algorithm", e);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("The account object " + account + " does not exist in the node");
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new CommandException("The node uses a verification version that is not available");
		}

		TransactionReference takamakaCode;

		try {
			takamakaCode = remote.getTakamakaCode();
		}
		catch (UninitializedNodeException e) {
			throw new CommandException("The node is not initialized yet!", e);
		}

		BigInteger balance;

		try {
			balance = remote.runInstanceMethodCallTransaction(
					TransactionRequests.instanceViewMethodCall(account, _100_000, takamakaCode, MethodSignatures.BALANCE, account))
					.orElseThrow(() -> new CommandException(MethodSignatures.BALANCE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.BALANCE, CommandException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not access the balance of account " + account, e);
		}

		String publicKeyBase64;
		try {
			publicKeyBase64 = remote.runInstanceMethodCallTransaction(
					TransactionRequests.instanceViewMethodCall(account, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, account))
					.orElseThrow(() -> new CommandException(MethodSignatures.PUBLIC_KEY + " should not return void"))
					.asReturnedString(MethodSignatures.PUBLIC_KEY, CommandException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not access the public key of account " + account, e);
		}

		try {
			report(json(), new Output(balance, signature, publicKeyBase64), AccountsShowOutputs.Encoder::new);
		}
		catch (Base64ConversionException e) {
			throw new CommandException("The key in the account object " + account + " is not in base64 format", e);
		}
		}
		catch (NodeException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	/**
	 * The output of this command.
	 */
	@Immutable
	public static class Output implements AccountsShowOutput {
		private final BigInteger balance;
		private final SignatureAlgorithm signature;
		private final String publicKeyBase58;
		private final String publicKeyBase64;

		/**
		 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
		 */
		public final static int MAX_PRINTED_KEY = 200;

		private Output(BigInteger balance, SignatureAlgorithm signature, String publicKeyBase64) throws Base64ConversionException {
			this.balance = balance;
			this.signature = signature;
			this.publicKeyBase64 = publicKeyBase64;
			this.publicKeyBase58 = Base58.toBase58String(Base64.fromBase64String(publicKeyBase64));
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(AccountsShowOutputJson json) throws InconsistentJsonException, NoSuchAlgorithmException {
			ExceptionSupplierFromMessage<InconsistentJsonException> exp = InconsistentJsonException::new;

			this.balance = Objects.requireNonNull(json.getBalance(), "balance cannot be null", exp);
			if (balance.signum() < 0)
				throw new InconsistentJsonException("The balance of the account cannot be negative");

			this.signature = SignatureAlgorithms.of(Objects.requireNonNull(json.getSignature(), "signature cannot be null", exp));
			this.publicKeyBase58 = Base58.requireBase58(Objects.requireNonNull(json.getPublicKeyBase58(), "publicKeyBase58 cannot be null", exp), exp);
			this.publicKeyBase64 = Base64.requireBase64(Objects.requireNonNull(json.getPublicKeyBase64(), "publicKeyBase64 cannot be null", exp), exp);
		}

		@Override
		public BigInteger getBalance() {
			return balance;
		}

		@Override
		public SignatureAlgorithm getSignature() {
			return signature;
		}

		@Override
		public String getPublicKeyBase58() {
			return publicKeyBase58;
		}

		@Override
		public String getPublicKeyBase64() {
			return publicKeyBase64;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
	
			sb.append("* balance: " + balance + "\n");

			if (publicKeyBase58.length() > MAX_PRINTED_KEY)
				sb.append("* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)\n");
			else
				sb.append("* public key: " + publicKeyBase58 + " (" + signature + ", base58)\n");

			if (publicKeyBase64.length() > MAX_PRINTED_KEY)
				sb.append("* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)\n");
			else
				sb.append("* public key: " + publicKeyBase64 + " (" + signature + ", base64)\n");

			return sb.toString();
		}
	}
}