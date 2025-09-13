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
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.moka.AccountsShowOutputs;
import io.hotmoka.moka.api.accounts.AccountsShowOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.StorageReferenceOrBase58Key;
import io.hotmoka.moka.internal.converters.StorageReferenceOrBase58KeyOptionConverter;
import io.hotmoka.moka.internal.json.AccountsShowOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show", header = "Show information about an account.", showDefaultValues = true)
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the storage account of the account or its Base58-encoded public key (if the account is in the accounts ledger)", converter = StorageReferenceOrBase58KeyOptionConverter.class)
    private StorageReferenceOrBase58Key account;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException, MisbehavingNodeException, UninitializedNodeException, UnexpectedCodeException {
		new Body(remote);
	}

	private class Body {
		private final RemoteNode remote;
		private final TransactionReference takamakaCode;
		private final boolean publicKeyAsBeenProvided;
		private final StorageReference account;

		private Body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException, MisbehavingNodeException, UninitializedNodeException, UnexpectedCodeException {
			this.remote = remote;
			this.takamakaCode = remote.getTakamakaCode();
			this.publicKeyAsBeenProvided = Show.this.account.asBase58Key().isPresent();
			this.account = getAccount();

			SignatureAlgorithm signature = getSignature();
			BigInteger balance = getBalance();
			String publicKeyBase64 = getPublicKeyBase64();

			try {
				report(new Output(account, balance, signature, publicKeyBase64), AccountsShowOutputs.Encoder::new);
			}
			catch (Base64ConversionException e) {
				throw new CommandException("The key in the account object " + account + " is not in base64 format", e);
			}
		}

		private StorageReference getAccount() throws CommandException, ClosedNodeException, TimeoutException, InterruptedException, UninitializedNodeException, UnexpectedCodeException {
			if (publicKeyAsBeenProvided) {
				String publicKeyBase58 = Show.this.account.asBase58Key().get(); // this must exists here
				String publicKeyBase64 = Base64.toBase64String(Base58.fromBase58String(publicKeyBase58, s -> new RuntimeException("The option converter for the account is misbehaving")));
				StorageReference manifest = remote.getManifest();
				StorageValue result;

				try {
					// we look in the accounts ledger
					var ledger = remote.runInstanceMethodCallTransaction
							(TransactionRequests.instanceViewMethodCall(manifest, _500_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
							.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_ACCOUNTS_LEDGER))
							.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, UnexpectedValueException::new);

					result = remote.runInstanceMethodCallTransaction
							(TransactionRequests.instanceViewMethodCall(manifest, _500_000, takamakaCode, MethodSignatures.GET_FROM_ACCOUNTS_LEDGER, ledger, StorageValues.stringOf(publicKeyBase64)))
							.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_FROM_ACCOUNTS_LEDGER));
				}
				catch (CodeExecutionException | TransactionException | TransactionRejectedException e) {
					throw new CommandException("Could not access the accounts ledger", e);
				}

				if (result instanceof StorageReference sr)
					return sr;
				else if (result instanceof NullValue)
					throw new CommandException("Nobody has paid anonymously to the key " + publicKeyBase58 + " up to now.");
				else
					throw new UnexpectedValueException("An unexpected value of type " + result.getClass().getSimpleName() + " has been found in the accounts ledger");
			}
			else
				return Show.this.account.asReference().get(); // this must exists here
		}

		private SignatureAlgorithm getSignature() throws MisbehavingNodeException, ClosedNodeException, InterruptedException, TimeoutException, CommandException {
			try {
				return SignatureHelpers.of(remote).signatureAlgorithmFor(account);
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The account " + account + " uses a non-available signature algorithm", e);
			}
			catch (UnknownReferenceException e) {
				if (publicKeyAsBeenProvided)
					throw new MisbehavingNodeException("The account ledger contains account " + account + " but the latter does not exist in the store of the node");
				else
					throw new CommandException("The account object " + account + " does not exist in the node");
			}
			catch (UnsupportedVerificationVersionException e) {
				throw new CommandException("The node uses a verification version that is not available");
			}
		}

		private BigInteger getBalance() throws CommandException, UnexpectedCodeException, ClosedNodeException, TimeoutException, InterruptedException {
			try {
				return remote.runInstanceMethodCallTransaction(
						TransactionRequests.instanceViewMethodCall(account, _500_000, takamakaCode, MethodSignatures.BALANCE, account))
						.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.BALANCE))
						.asReturnedBigInteger(MethodSignatures.BALANCE, UnexpectedValueException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Could not access the balance of account " + account, e);
			}
		}

		private String getPublicKeyBase64() throws CommandException, UnexpectedCodeException, ClosedNodeException, TimeoutException, InterruptedException {
			try {
				return remote.runInstanceMethodCallTransaction(
						TransactionRequests.instanceViewMethodCall(account, _500_000, takamakaCode, MethodSignatures.PUBLIC_KEY, account))
						.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.PUBLIC_KEY))
						.asReturnedString(MethodSignatures.PUBLIC_KEY, UnexpectedValueException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Could not access the public key of account " + account, e);
			}
		}
	}

	/**
	 * The output of this command.
	 */
	@Immutable
	public static class Output implements AccountsShowOutput {
		private final StorageReference account;
		private final BigInteger balance;
		private final SignatureAlgorithm signature;
		private final String publicKeyBase58;
		private final String publicKeyBase64;

		private Output(StorageReference account, BigInteger balance, SignatureAlgorithm signature, String publicKeyBase64) throws Base64ConversionException {
			this.account = account;
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

			this.account = json.getAccount().unmap().asReference(value -> new InconsistentJsonException("The reference to the account shown must be a storage reference, not a " + value.getClass().getName()));

			this.balance = Objects.requireNonNull(json.getBalance(), "balance cannot be null", exp);
			if (balance.signum() < 0)
				throw new InconsistentJsonException("The balance of the account cannot be negative");

			this.signature = SignatureAlgorithms.of(Objects.requireNonNull(json.getSignature(), "signature cannot be null", exp));
			this.publicKeyBase58 = Base58.requireBase58(Objects.requireNonNull(json.getPublicKeyBase58(), "publicKeyBase58 cannot be null", exp), exp);
			this.publicKeyBase64 = Base64.requireBase64(Objects.requireNonNull(json.getPublicKeyBase64(), "publicKeyBase64 cannot be null", exp), exp);
		}

		@Override
		public StorageReference getAccount() {
			return account;
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

			sb.append(green("Account: " + account) + "\n");
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