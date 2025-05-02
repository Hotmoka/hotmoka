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
import io.hotmoka.moka.internal.converters.AccountOptionConverter;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
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

	@Option(names = "--payer", description = "the account that pays for the creation; if missing, the faucet of the network will be used, if it is open", converter = AccountOptionConverter.class)
    private Account payer;

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
			createFromPayer(remote);
	}

	private void createFromPayer(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		SignatureAlgorithm signatureOfNewAccount = this.signature != null ? this.signature : remote.getConfig().getSignatureForRequests();
		String passwordOfNewAccountAsString = new String(password);
		String passwordOfPayerAsString = new String(passwordOfPayer);
		PublicKey publicKeyOfNewAccount = publicKeyIdentifier.getPublicKey(signatureOfNewAccount, passwordOfNewAccountAsString);

		try {
			ClassType eoaType;
			BigInteger proposedGas;

			switch (signatureOfNewAccount.getName()) {
			case "ed25519":
				proposedGas = _100_000;
				eoaType = StorageTypes.classNamed(StorageTypes.EOA + signatureOfNewAccount.getName().toUpperCase());
				break;
			case "sha256dsa":
				proposedGas = BigInteger.valueOf(200_000L);
				eoaType = StorageTypes.classNamed(StorageTypes.EOA + signatureOfNewAccount.getName().toUpperCase());
				break;
			case "qtesla1":
				proposedGas = BigInteger.valueOf(3_000_000L);
				eoaType = StorageTypes.classNamed(StorageTypes.EOA + signatureOfNewAccount.getName().toUpperCase());
				break;
			case "qtesla3":
				proposedGas = BigInteger.valueOf(6_000_000L);
				eoaType = StorageTypes.classNamed(StorageTypes.EOA + signatureOfNewAccount.getName().toUpperCase());
				break;
			default:
				throw new CommandException("Cannot create accounts with signature algorithm " + signatureOfNewAccount);
			}

			SignatureAlgorithm signatureOfPayer;
			BigInteger nonce, gasPrice;
			try {
				signatureOfPayer = SignatureHelpers.of(remote).signatureAlgorithmFor(payer.getReference());
				nonce = NonceHelpers.of(remote).getNonceOf(payer.getReference());
				gasPrice = GasHelpers.of(remote).getGasPrice();
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException(payer + " uses a non-available signature algorithm", e);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException(payer + " cannot be found in the store of the node");
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("Cannot determine the nonce of the payer and the current gas price!", e);
			}

			proposedGas = proposedGas.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));

			String publicKeyEncoded;
			try {
				publicKeyEncoded = Base64.toBase64String(signatureOfNewAccount.encodingOf(publicKeyOfNewAccount));
			}
			catch (InvalidKeyException e) {
				// the key has been created with the same signature algorithm, it cannot be invalid
				throw new RuntimeException(e);
			}

			Signer<SignedTransactionRequest<?>> signer = signatureOfPayer.getSigner(payer.keys(passwordOfPayerAsString, signatureOfPayer).getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

			askForConfirmation(proposedGas);

			ConstructorCallTransactionRequest request;
			try {
				request = TransactionRequests.constructorCall
						(signer, payer.getReference(), nonce, remote.getConfig().getChainId(), proposedGas, gasPrice, remote.getTakamakaCode(),
							ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
							StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same signature algorithm, it cannot be invalid
				throw new RuntimeException(e);
			}

			StorageReference newAccount;
			try {
				newAccount = remote.addConstructorCallTransaction(request);
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("The creation transaction failed!", e);
			}

			System.out.println("A new account " + newAccount + " has been created.");

			Path keys = publicKeyIdentifier.keys;
			if (keys != null) {
				Entropy entropy;

				try {
					entropy = Entropies.load(keys);
				}
				catch (IOException e) {
					throw new CommandException("Cannot access file \"" + keys + "\"!", e);
				}

				var account = Accounts.of(entropy, newAccount);
				try {
					System.out.println("Its key pair has been saved into the file \"" + account.dump() + "\".");
				}
				catch (IOException e) {
					throw new CommandException("Cannot save the key pair of the account in file \"" + account + ".pem\"!");
				}
			}

			try {
				Hasher<TransactionRequest<?>> hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
				System.out.println("Creation transaction: " + asTransactionReference(TransactionReferences.of(hasher.hash(request))));
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The sha256 hashing algorithm is not available");
			}

			printCosts(remote, new TransactionRequest[] { request });
		}
		finally {
			passwordOfNewAccountAsString = null;
			passwordOfPayerAsString = null;
			Arrays.fill(password, ' ');
			Arrays.fill(passwordOfPayer, ' ');
		}
	}

	/**
	 * Asks the user about the real intention to spend some gas.
	 * 
	 * @param gas the amount of gas
	 * @throws CommandException if the user replies negatively
	 */
	protected final void askForConfirmation(BigInteger gas) throws CommandException {
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

	private void printCosts(Node node, TransactionRequest<?>... requests) {
		try {
			var gasCounter = GasCounters.of(node, requests);
			String result = "Total gas consumed: " + gasCounter.total() + "\n";
			result += "  for CPU: " + gasCounter.forCPU() + "\n";
			result += "  for RAM: " + gasCounter.forRAM() + "\n";
			result += "  for storage: " + gasCounter.forStorage() + "\n";
			result += "  for penalty: " + gasCounter.forPenalty();
			System.out.println(result);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (NodeException | TimeoutException | UnknownReferenceException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e); // TODO
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