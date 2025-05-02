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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasCounters;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create a new account object.", showDefaultValues = true)
public class Create extends AbstractMokaRpcCommand {

	@Parameters(description = "the initial balance of the new account; this will be deduced from the balance of the payer", defaultValue = "0")
	private BigInteger balance;

	@Option(names = "--payer", description = "the account that pays for the creation; if missing, the faucet of the network will be used, if it is open", converter = StorageReferenceOfAccountOptionConverter.class)
    private StorageReference payer;

	@Option(names = "--dir", description = "the path of the directory where the key pair of the payer can be found", defaultValue = "")
    private Path dir;

	@Option(names = "--password-of-payer", description = "the password of the payer; this is not used if the payer is the faucet", interactive = true, defaultValue = "")
    private char[] passwordOfPayer;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private PublicKeyIdentifier publicKeyIdentifier;

	@Option(names = "--signature", description = "the signature algorithm of the new account (ed25519, sha256dsa, qtesla1, qtesla3); if missing, the deafult request signature of the node will be used", converter = SignatureOptionConverter.class)
	private SignatureAlgorithm signature;

	@Option(names = "--password", description = "the password of the key pair specified through --keys", interactive = true, defaultValue = "")
    private char[] password;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	/**
	 * The alternative ways of specifying the public key of the new account.
	 */
	private static class PublicKeyIdentifier {
		
		@Option(names = "--key", description = "the Base58-encoded public key of the new account")
		private String key;
	
		@Option(names = "--keys", description = "the key pair of the new account")
	    private Path keys;

		/**
		 * Yields the public key from this identifier.
		 * 
		 * @param signature the signature algorithm of the public key
		 * @return the public key
		 * @throws CommandException if some option is incorrect
		 */
		private PublicKey getPublicKey(SignatureAlgorithm signature, String passwordOfNewAccount) throws CommandException {
			if (key != null) {
				try {
					return signature.publicKeyFromEncoding(Base58.fromBase58String(key, message -> new CommandException("The public key specified by --key is not in Base58 format")));
				}
				catch (InvalidKeySpecException e) {
					throw new CommandException("The public key specified by --key is invalid for the signature algorithm " + signature, e);
				}
			}
			else {
				try {
					return Entropies.load(keys).keys(passwordOfNewAccount, signature).getPublic();
				}
				catch (IOException e) {
					throw new CommandException("Cannot access file \"" + keys + "\"!", e);
				}
			}
		}
	}

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		if (payer != null)
			new CreationFromPayer(remote);
	}

	private class CreationFromPayer {
		private final RemoteNode remote;
		private final Account payerAccount;
		private final SignatureAlgorithm signatureOfPayer;
		private final SignatureAlgorithm signatureOfNewAccount;
		private final PublicKey publicKeyOfNewAccount;
		private final String publicKetOfNewAccountBase64;
		private final ClassType eoaType;
		private final BigInteger proposedGas;
		private final BigInteger nonce;
		private final BigInteger gasPrice;
		private final ConstructorCallTransactionRequest request;
		private final TransactionReference referenceOfRequest;
		private final StorageReference referenceOfNewAccount;

		private CreationFromPayer(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;

			String passwordOfNewAccountAsString = new String(password);
			String passwordOfPayerAsString = new String(passwordOfPayer);

			try {
				this.signatureOfNewAccount = signature != null ? signature : remote.getConfig().getSignatureForRequests();
				this.publicKeyOfNewAccount = publicKeyIdentifier.getPublicKey(signatureOfNewAccount, passwordOfNewAccountAsString);
				this.payerAccount = mkPayerAccount();
				this.signatureOfPayer = determineSignatureOfPayer();
				this.nonce = determineNonceOfPayer();
				this.gasPrice = determineGasPrice();
				this.publicKetOfNewAccountBase64 = mkPublicKeyOfNewAccountBase64();
				this.eoaType = determineEOAType();
				this.proposedGas = computeProposedGas();
				this.request = mkRequest(passwordOfPayerAsString);
				askForConfirmation(proposedGas);
				this.referenceOfNewAccount = executeRequest();
				System.out.println("A new account " + referenceOfNewAccount + " has been created.");
				this.referenceOfRequest = computeReferenceOfRequest();
				System.out.println("Creation transaction: " + asTransactionReference(referenceOfRequest));
				printCosts(remote, new TransactionRequest[] { request });

				if (publicKeyIdentifier.keys != null)
					bindKeysToNewAccount();
			}
			finally {
				passwordOfNewAccountAsString = null;
				passwordOfPayerAsString = null;
				Arrays.fill(password, ' ');
				Arrays.fill(passwordOfPayer, ' ');
			}
		}

		private void bindKeysToNewAccount() throws CommandException {
			Entropy entropy;

			try {
				entropy = Entropies.load(publicKeyIdentifier.keys);
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + publicKeyIdentifier.keys + "\"!", e);
			}

			var newAccount = Accounts.of(entropy, referenceOfNewAccount);
			try {
				System.out.println("Its key pair has been saved into the file \"" + newAccount.dump() + "\".");
			}
			catch (IOException e) {
				throw new CommandException("Cannot save the key pair of the account in file \"" + newAccount + ".pem\"!");
			}
		}

		private StorageReference executeRequest() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return remote.addConstructorCallTransaction(request);
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("The creation transaction failed!", e);
			}
		}

		private ConstructorCallTransactionRequest mkRequest(String passwordOfPayerAsString) throws NodeException, TimeoutException, InterruptedException {
			try {
				Signer<SignedTransactionRequest<?>> signer = signatureOfPayer.getSigner(payerAccount.keys(passwordOfPayerAsString, signatureOfPayer).getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
				return TransactionRequests.constructorCall
						(signer, payer, nonce, remote.getConfig().getChainId(), proposedGas, gasPrice, remote.getTakamakaCode(),
								ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
								StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKetOfNewAccountBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same signature algorithm, it cannot be invalid
				throw new RuntimeException(e);
			}
		}

		private BigInteger computeProposedGas() throws CommandException {
			switch (signatureOfNewAccount.getName()) {
			case "ed25519":
				return _100_000.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			case "sha256dsa":
				return BigInteger.valueOf(200_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			case "qtesla1":
				return BigInteger.valueOf(3_000_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			case "qtesla3":
				return BigInteger.valueOf(6_000_000L).add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
			default:
				throw new CommandException("Cannot create accounts with signature algorithm " + signatureOfNewAccount);
			}
		}

		private ClassType determineEOAType() throws CommandException {
			switch (signatureOfNewAccount.getName()) {
			case "ed25519":
			case "sha256dsa":
			case "qtesla1":
			case "qtesla3":
				return StorageTypes.classNamed(StorageTypes.EOA + signatureOfNewAccount.getName().toUpperCase());
			default:
				throw new CommandException("Cannot create accounts with signature algorithm " + signatureOfNewAccount);
			}
		}

		private SignatureAlgorithm determineSignatureOfPayer() throws CommandException, NodeException, InterruptedException, TimeoutException {
			try {
				return SignatureHelpers.of(remote).signatureAlgorithmFor(payer);
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException(payer + " uses a non-available signature algorithm", e);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException(payer + " cannot be found in the store of the node");
			}
		}

		private BigInteger determineNonceOfPayer() throws CommandException, NodeException, InterruptedException, TimeoutException {
			try {
				return NonceHelpers.of(remote).getNonceOf(payer);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException(payer + " cannot be found in the store of the node");
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("Cannot determine the nonce of the payer and the current gas price!", e);
			}
		}

		private BigInteger determineGasPrice() throws CommandException, NodeException, TimeoutException, InterruptedException {
			try {
				return GasHelpers.of(remote).getGasPrice();
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("Cannot determine the nonce of the payer and the current gas price!", e);
			}
		}

		private String mkPublicKeyOfNewAccountBase64() {
			try {
				return Base64.toBase64String(signatureOfNewAccount.encodingOf(publicKeyOfNewAccount));
			}
			catch (InvalidKeyException e) {
				// the key has been created with the same signature algorithm, it cannot be invalid
				throw new RuntimeException(e);
			}
		}

		private Account mkPayerAccount() throws CommandException {
			try {
				return Accounts.of(payer, dir);
			}
			catch (IOException e) {
				throw new CommandException("Cannot read the key pair of the account: it was expected to be in file \"" + dir.resolve(payer.toString()) + ".pem\"", e);
			}
		}

		private TransactionReference computeReferenceOfRequest() throws CommandException {
			try {
				Hasher<TransactionRequest<?>> hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
				return TransactionReferences.of(hasher.hash(request));
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The sha256 hashing algorithm is not available");
			}
		}
	}

	/**
	 * Asks the user about the real intention to spend some gas.
	 * 
	 * @param gas the amount of gas
	 * @throws CommandException if the user replies negatively
	 */
	private void askForConfirmation(BigInteger gas) throws CommandException {
		if (!yes && !json() && !answerIsYes(asInteraction("Do you really want to spend up to " + gas + " gas units to create a new account [Y/N] ")))
			throw new CommandException("Stopped");
	}

	private static BigInteger gasForTransactionWhosePayerHasSignature(SignatureAlgorithm signature) {
		switch (signature.getName()) {
		case "qtesla1":
			return BigInteger.valueOf(300_000L);
		case "qtesla3":
			return BigInteger.valueOf(400_000L);
		default:
			return _100_000;
		}
	}

	private static void printCosts(Node node, TransactionRequest<?>... requests) throws CommandException, InterruptedException, NodeException, TimeoutException {
		try {
			var gasCounter = GasCounters.of(node, requests);
			String result = "Total gas consumed: " + gasCounter.total() + "\n";
			result += "  for CPU: " + gasCounter.forCPU() + "\n";
			result += "  for RAM: " + gasCounter.forRAM() + "\n";
			result += "  for storage: " + gasCounter.forStorage() + "\n";
			result += "  for penalty: " + gasCounter.forPenalty();
			System.out.println(result);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("Cannot find the creation request in the store of the node, maybe a sudden history change has occurred", e);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("A cryptographic algorithm is not available");
		}
	}

	/*private class Run {
		private final Node node;
		private final PublicKey publicKey;
		private final AccountCreationHelper accountCreationHelper;
		private final SignatureAlgorithm signatureAlgorithmOfNewAccount;

		private Run() throws Exception {
			passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", interactive, "faucet".equals(payer));
			if (keyOfNewAccount != null)
				checkPublicKey(keyOfNewAccount);
			else
				passwordOfNewAccount = ensurePassword(passwordOfNewAccount, "the new account", interactive, false);
	
			try (var node = this.node = RemoteNodes.of(uri, 10_000)) {
				String nameOfSignatureAlgorithmOfNewAccount = "default".equals(signature) ? node.getConfig().getSignatureForRequests().getName() : signature;
				signatureAlgorithmOfNewAccount = SignatureAlgorithms.of(nameOfSignatureAlgorithmOfNewAccount);

				Entropy entropy;
				if (keyOfNewAccount == null) {
					entropy = Entropies.random();
					publicKey = entropy.keys(passwordOfNewAccount, signatureAlgorithmOfNewAccount).getPublic();
				}
				else {
					entropy = Entropies.load(Paths.get(keyOfNewAccount + ".pem"));
					publicKey = signatureAlgorithmOfNewAccount.publicKeyFromEncoding(Base58.fromBase58String(keyOfNewAccount));
				}

				accountCreationHelper = AccountCreationHelpers.of(node);
				StorageReference accountReference = "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
				var account = Accounts.of(entropy, accountReference);
	            System.out.println("A new account " + account + " has been created.");
	            System.out.println("Its entropy has been saved into the file \"" + account.dump() + "\".");
			}
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation will succeed only if the gamete of the node supports an open unsigned faucet.");
			
			try {
				return accountCreationHelper.paidByFaucet(signatureAlgorithmOfNewAccount, publicKey, balance, this::printCosts);
			}
			catch (TransactionRejectedException e) {
				if (e.getMessage().contains("invalid request signature"))
					throw new IllegalStateException("invalid request signature: is the unsigned faucet of the node open?");

				throw e;
			}
		}
	}*/
}