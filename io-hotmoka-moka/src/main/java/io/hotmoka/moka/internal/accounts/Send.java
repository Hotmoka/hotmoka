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

package io.hotmoka.moka.internal.accounts;

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.GAMETE;
import static io.hotmoka.node.StorageTypes.PAYABLE_CONTRACT;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.moka.AccountsSendOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.accounts.AccountsSendOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.Base58OptionConverter;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.converters.TransactionReferenceOptionConverter;
import io.hotmoka.moka.internal.json.AccountsSendOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "send",
	description = "Send units of coin to a payable contract",
	showDefaultValues = true)
public class Send extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the amount of coins to send to the destination; this will be deduced from the balance of the payer", defaultValue = "0")
    private BigInteger amount;

	@ArgGroup(exclusive = true, multiplicity = "1", heading = "The destination must be specified in either of these two alternative ways:\n")
	private DestinationIdentifier destination;

	@Option(names = "--payer", paramLabel = "<storage reference>", description = "the account that pays for the amount sent to the destination; if missing, the faucet of the network will be used, if it is open", converter = StorageReferenceOptionConverter.class)
	private StorageReference payer;

	@Option(names = "--password-of-payer", description = "the password of the payer; this is not used if the payer is the faucet", interactive = true, defaultValue = "")
	private char[] passwordOfPayer;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the current key pair of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--classpath", paramLabel = "<transaction reference>", description = "the classpath used to interpret payer and destination; if missing, the reference to the transaction that created the destination account will be used; if the destination is a key, the reference to the transaction that created the payer will be used", converter = TransactionReferenceOptionConverter.class)
    private TransactionReference classpath;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	/**
	 * The specification of the destination of a send operation: either
	 * a Base58-encoded public key or a storage reference.
	 */
	private static class DestinationIdentifier {

		@Option(names = "--key", paramLabel = "<Base58-encoded string>", description = "as a public key", converter = Base58OptionConverter.class)
		private String key;

		@Option(names = "--account", paramLabel = "<storage reference>", description = "as an account", converter = StorageReferenceOptionConverter.class)
		private StorageReference account;

		@Override
		public String toString() {
			return key != null ? ("public key " + key) : account.toString();
		}
	}

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		if (payer != null)
			if (destination.account != null)
				new SendFromPayerAccountToDestinationAccount(remote);
			else
				new SendFromPayerAccountToDestinationKey(remote);
		else
			if(destination.account != null)
				new SendFromFaucetToDestinationAccount(remote);
			else
				throw new CommandException("It is not possible to let the faucet pay to a public key; please specify either --payer or --account");
	}

	private TransactionReference getClasspath(RemoteNode remote) throws NodeException, TimeoutException, InterruptedException, CommandException {
		if (classpath != null)
			return classpath;
		else if (destination.account != null)
			return getClasspathAtCreationTimeOf(destination.account, remote);
		else
			return getClasspathAtCreationTimeOf(payer, remote);
	}

	private class SendFromPayerAccountToDestinationAccount {
		private final RemoteNode remote;
		private final String chainId;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final TransactionReference classpath;
		private final BigInteger nonce;
		private final InstanceMethodCallTransactionRequest request;
	
		private SendFromPayerAccountToDestinationAccount(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			String passwordOfPayerAsString = new String(passwordOfPayer);
	
			try {
				this.remote = remote;
				this.chainId = remote.getConfig().getChainId();
				SignatureAlgorithm signatureOfPayer = determineSignatureOf(payer, remote);
				this.signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
				this.gasLimit = determineGasLimit(() -> gasLimitHeuristic(signatureOfPayer));
				this.gasPrice = determineGasPrice(remote);
				this.classpath = getClasspath(remote);
				askForConfirmation("send " + amount + " panas from " + payer + " to " + destination, gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(payer, remote);
				this.request = mkRequest();
				report(json(), executeRequest(), AccountsSendOutputs.Encoder::new);
			}
			finally {
				passwordOfPayerAsString = null;
				Arrays.fill(passwordOfPayer, ' ');
			}
		}
	
		private InstanceMethodCallTransactionRequest mkRequest() throws CommandException {
			try {
				return TransactionRequests.instanceMethodCall
						(signer, payer, nonce, chainId, gasLimit, gasPrice, classpath,
						MethodSignatures.RECEIVE_BIG_INTEGER, destination.account, StorageValues.bigIntegerOf(amount));
			}
			catch (InvalidKeyException | SignatureException e) {
				throw new CommandException("The key pair of " + payer + " seems corrupted!", e);
			}
		}

		private Output executeRequest() throws CommandException, NodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<GasCost> gasCost = Optional.empty();
			Optional<String> errorMessage = Optional.empty();

			try {
				if (post()) {
					if (!json())
						System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");

					remote.postInstanceMethodCallTransaction(request);

					if (!json())
						System.out.println("done.");
				}
				else {
					if (!json())
						System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");

					try {
						remote.addInstanceMethodCallTransaction(request);
						if (!json())
							System.out.println("done.");
					}
					catch (TransactionException | CodeExecutionException e) {
						if (!json())
							System.out.println("failed. Are the key pair of the payer and its password correct?");

						errorMessage = Optional.of(e.getMessage());
					}

					gasCost = Optional.of(computeIncurredGasCost(remote, gasPrice, transaction));
				}
			}
			catch (TransactionRejectedException e) {
				throw new CommandException("Transaction " + transaction + " has been rejected!", e);
			}

			return new Output(transaction, gasCost, errorMessage, Optional.empty());
		}

		private BigInteger gasLimitHeuristic(SignatureAlgorithm signatureOfPayer) throws CommandException {
			return _100_000.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
		}
	}

	private class SendFromFaucetToDestinationAccount {
		private final RemoteNode remote;
		private final StorageReference gamete;
		private final String chainId;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final TransactionReference classpath;
		private final InstanceMethodCallTransactionRequest request;
	
		private SendFromFaucetToDestinationAccount(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;
			this.gamete = getGamete();
			this.chainId = remote.getConfig().getChainId();
			// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
			SignatureAlgorithm signatureOfFaucet = SignatureAlgorithms.empty();
			this.signer = signatureOfFaucet.getSigner(signatureOfFaucet.getKeyPair().getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
			this.gasLimit = determineGasLimit(() -> gasLimitHeuristic(signatureOfFaucet));
			this.gasPrice = determineGasPrice(remote);
			this.classpath = getClasspath(remote);
			this.request = mkRequest();
			report(json(), executeRequest(), AccountsSendOutputs.Encoder::new);
		}
	
		private InstanceMethodCallTransactionRequest mkRequest() {
			try {
				// we use a random nonce: although the nonce is not checked for calls to the faucet,
				// this avoids the risk of the request being rejected because it is repeated
				return TransactionRequests.instanceMethodCall
						(signer, gamete, new BigInteger(64, new SecureRandom()), chainId, gasLimit, gasPrice, classpath,
						MethodSignatures.ofVoid(GAMETE, "faucet", PAYABLE_CONTRACT, BIG_INTEGER),
						gamete, StorageValues.bigIntegerOf(amount));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same (empty!) signature algorithm, thus it cannot be invalid
				throw new RuntimeException(e);
			}
		}
	
		private Output executeRequest() throws CommandException, NodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<String> errorMessage = Optional.empty();
	
			try {
				if (post()) {
					if (!json())
						System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");
	
					remote.postInstanceMethodCallTransaction(request);
	
					if (!json())
						System.out.println("done.");
				}
				else {
					if (!json())
						System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");
	
					try {
						remote.addInstanceMethodCallTransaction(request);
						if (!json())
							System.out.println("done.");
					}
					catch (TransactionException | CodeExecutionException e) {
						if (!json())
							System.out.println("failed.");
	
						errorMessage = Optional.of(e.getMessage());
					}
				}
			}
			catch (TransactionRejectedException e) {
				throw new CommandException("Transaction " + transaction + " has been rejected!", e);
			}
	
			return new Output(transaction, Optional.empty(), errorMessage, Optional.empty());
		}
	
		private StorageReference getGamete() throws NodeException, TimeoutException, InterruptedException, CommandException {
			var manifest = remote.getManifest();
			var takamakaCode = remote.getTakamakaCode();
	
			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
						.orElseThrow(() -> new CommandException(MethodSignatures.GET_GAMETE + " should not return void"))
						.asReturnedReference(MethodSignatures.GET_GAMETE, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Could not determine the gamete of the node");
			}
		}
	
		private BigInteger gasLimitHeuristic(SignatureAlgorithm signatureOfPayer) throws CommandException {
			return _100_000.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
		}
	}

	private class SendFromPayerAccountToDestinationKey {
		private final RemoteNode remote;
		private final StorageReference accountsLedger;
		private final String chainId;
		private final SignatureAlgorithm signatureOfDestination;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final TransactionReference classpath;
		private final BigInteger nonce;
		private final InstanceMethodCallTransactionRequest request;
	
		private SendFromPayerAccountToDestinationKey(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			String passwordOfPayerAsString = new String(passwordOfPayer);
	
			try {
				this.remote = remote;
				this.accountsLedger = getAccountsLedger();
				this.chainId = remote.getConfig().getChainId();
				SignatureAlgorithm signatureOfPayer = determineSignatureOf(payer, remote);
				this.signatureOfDestination = determineSignatureOfDestination();
				this.signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
				this.gasLimit = determineGasLimit(() -> gasLimitHeuristic(signatureOfPayer));
				this.gasPrice = determineGasPrice(remote);
				this.classpath = getClasspath(remote);
				askForConfirmation("send " + amount + " panas from " + payer + " to " + destination, gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(payer, remote);
				this.request = mkRequest();
				report(json(), executeRequest(), AccountsSendOutputs.Encoder::new);
			}
			finally {
				passwordOfPayerAsString = null;
				Arrays.fill(passwordOfPayer, ' ');
			}
		}
	
		private InstanceMethodCallTransactionRequest mkRequest() throws CommandException {
			try {
				String publicKeyEncoded = Base64.toBase64String(signatureOfDestination.encodingOf(signatureOfDestination.publicKeyFromEncoding(Base58.fromBase58String(destination.key))));
	
				return TransactionRequests.instanceMethodCall
					(signer, payer, nonce,
					chainId, gasLimit, gasPrice, classpath,
					MethodSignatures.ADD_INTO_ACCOUNTS_LEDGER,
					accountsLedger,
					StorageValues.bigIntegerOf(amount), StorageValues.stringOf(publicKeyEncoded));
			}
			catch (Base58ConversionException e) {
				// this should not happen, since the parameter is checked by its converter
				throw new RuntimeException(e);
			}
			catch (InvalidKeyException | SignatureException | InvalidKeySpecException e) {
				throw new CommandException("The key pair of " + payer + " seems corrupted!", e);
			}
		}
	
		private Output executeRequest() throws CommandException, NodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<GasCost> gasCost = Optional.empty();
			Optional<String> errorMessage = Optional.empty();
			Optional<StorageReference> destination = Optional.empty();
	
			try {
				if (post()) {
					if (!json())
						System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");
	
					remote.postInstanceMethodCallTransaction(request);
	
					if (!json())
						System.out.println("done.");
				}
				else {
					if (!json())
						System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");
	
					try {
						StorageReference destinationInAccountsdLedger = remote.addInstanceMethodCallTransaction(request)
								.orElseThrow(() -> new CommandException(MethodSignatures.ADD_INTO_ACCOUNTS_LEDGER + " should not return void"))
								.asReturnedReference(MethodSignatures.ADD_INTO_ACCOUNTS_LEDGER, CommandException::new);

						destination = Optional.of(destinationInAccountsdLedger);

						if (!json())
							System.out.println("done.");
					}
					catch (TransactionException | CodeExecutionException e) {
						if (!json())
							System.out.println("failed. Are the key pair of the payer and its password correct?");
	
						errorMessage = Optional.of(e.getMessage());
					}
	
					gasCost = Optional.of(computeIncurredGasCost(remote, gasPrice, transaction));
				}
			}
			catch (TransactionRejectedException e) {
				throw new CommandException("Transaction " + transaction + " has been rejected!", e);
			}
	
			return new Output(transaction, gasCost, errorMessage, destination);
		}
	
		private SignatureAlgorithm determineSignatureOfDestination() throws CommandException {
			try {
				// the accounts ledger only allows ed25519 accounts currently
				return SignatureAlgorithms.ed25519();
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The ed25119 signature lagorotihm of the destination account is not available");
			}
		}
	
		private StorageReference getAccountsLedger() throws NodeException, TimeoutException, InterruptedException, CommandException {
			var manifest = remote.getManifest();
			var takamakaCode = remote.getTakamakaCode();
	
			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
						.orElseThrow(() -> new CommandException(MethodSignatures.GET_ACCOUNTS_LEDGER + " should not return void"))
						.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Could not determine the accounts ledger of the node");
			}
		}
	
		private BigInteger gasLimitHeuristic(SignatureAlgorithm signatureOfPayer) throws CommandException {
			switch (signatureOfDestination.getName()) {
			case "ed25519":
				return BigInteger.valueOf(600_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			case "sha256dsa":
				return BigInteger.valueOf(700_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			case "qtesla1":
				return BigInteger.valueOf(3_500_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			case "qtesla3":
				return BigInteger.valueOf(6_500_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			default:
				throw new CommandException("Cannot create accounts with signature algorithm " + signatureOfDestination);
			}
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractGasCostCommandOutput implements AccountsSendOutput {

		/**
		 * The account that received the sent coins, if the payment was into a key; this means that this is the
		 * account, in the accounts ledger, for that key; this is missing if the payment was not into a key
		 * or the the transaction has just been posted.
		 */
		private final Optional<StorageReference> destinationInAccountsLedger;

		private Output(TransactionReference transaction, Optional<GasCost> gasCost, Optional<String> errorMessage, Optional<StorageReference> destinationInAccountsLedger) {
			super(transaction, gasCost, errorMessage);

			this.destinationInAccountsLedger = destinationInAccountsLedger;
		}

		public Output(AccountsSendOutputJson json) throws InconsistentJsonException {
			super(json);

			var destinationInAccountsLedgerJson = json.getDestinationInAccountsLedger();
			if (destinationInAccountsLedgerJson != null)
				this.destinationInAccountsLedger = Optional.of(destinationInAccountsLedgerJson.get().unmap().asReference(value -> new InconsistentJsonException("destinationInAccountsLedger should be a StorageReference, not a " + value.getClass().getName())));
			else
				this.destinationInAccountsLedger = Optional.empty();
		}

		@Override
		public Optional<StorageReference> getDestinationInAccountsLedger() {
			return destinationInAccountsLedger;
		}

		@Override
		protected void toString(StringBuilder sb) {
			destinationInAccountsLedger.ifPresent(account -> {
				sb.append("The payment went into account " + account + ".\n");
				sb.append("The owner of the destination key pair can bind it now to its address with:\n");
				sb.append(asCommand("  moka keys bind file_containing_the_destination_key_pair --password --url url_of_this_Hotmoka_node\n"));
				sb.append("or with:\n");
				sb.append(asCommand("  moka keys bind file_containing_the_destination_key_pair --password --reference " + account + "\n"));
			});
		}
	}
}