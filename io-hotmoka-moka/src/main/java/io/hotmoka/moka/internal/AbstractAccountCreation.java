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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.GasCounters;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.GasCounter;
import io.hotmoka.moka.api.AccountCreationOutput;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.moka.internal.json.AccountCreationOutputJson;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shared code for the creation of an account.
 */
public abstract class AbstractAccountCreation extends AbstractMokaRpcCommand {

	@Parameters(description = "the initial balance of the new account; this will be deduced from the balance of the payer", defaultValue = "0")
	private BigInteger balance;

	@Option(names = "--payer", description = "the account that pays for the creation; if missing, the faucet of the network will be used, if it is open", converter = StorageReferenceOfAccountOptionConverter.class)
	private StorageReference payer;

	@Option(names = "--dir", description = "the path of the directory where the key pair of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--output-dir", description = "the path of the directory where the key pair of the new account will be written", defaultValue = "")
	private Path outputDir;

	@Option(names = "--password-of-payer", description = "the password of the payer; this is not used if the payer is the faucet", interactive = true, defaultValue = "")
	private char[] passwordOfPayer;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private PublicKeyIdentifier publicKeyIdentifier;

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
	
		@Option(names = "--keys", description = "the key pair file of the new account")
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
	protected final void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		if (payer != null)
			new CreationFromPayer(remote);
		else
			new CreationFromFaucet(remote);
	}

	/**
	 * Yields the signature algorithm to use for the new account that is going to be created.
	 * 
	 * @param remote the node for which the account is being created
	 * @return the signature algorithm
	 */
	protected abstract SignatureAlgorithm getSignatureAlgorithmOfNewAccount(RemoteNode remote) throws CommandException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the method of the faucet to call for creating the new account.
	 * 
	 * @param signatureOfNewAccount the signature algorithm of the new account
	 * @param eoaType the type of account to create
	 * @return the method
	 */
	protected abstract NonVoidMethodSignature getFaucetMethod(SignatureAlgorithm signatureOfNewAccount, ClassType eoaType);

	/**
	 * Yields the class of the externally-owned account that is being created.
	 * 
	 * @param signatureOfNewAccount the signature algorithm of the new account
	 * @return the class of the externally-owned account that is being created
	 * @throws CommandException if the operation fails
	 */
	protected abstract ClassType getEOAType(SignatureAlgorithm signatureOfNewAccount) throws CommandException;

	/**
	 * Reports the output of this command to the user.
	 * 
	 * @param transaction the transaction that created the new account
	 * @param referenceOfNewAccount the reference of the new account
	 * @param file the file where the key pair of the new account has been saved, if any
	 * @param gasCosts the gas costs incurred for the creation of the new account
	 * @throws CommandException if the report fails
	 */
	protected abstract void reportOutput(TransactionReference transaction, StorageReference referenceOfNewAccount, Optional<Path> file, GasCounter gasCosts) throws CommandException;

	private class CreationFromPayer {
		private final RemoteNode remote;
		private final Account payerAccount;
		private final SignatureAlgorithm signatureOfPayer;
		private final String publicKeyOfNewAccountBase64;
		private final ClassType eoaType;
		private final BigInteger proposedGas;
		private final BigInteger nonce;
		private final BigInteger gasPrice;
		private final ConstructorCallTransactionRequest request;

		private CreationFromPayer(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;

			String passwordOfNewAccountAsString = new String(password);
			String passwordOfPayerAsString = new String(passwordOfPayer);

			try {
				SignatureAlgorithm signatureOfNewAccount = getSignatureAlgorithmOfNewAccount(remote);
				PublicKey publicKeyOfNewAccount = publicKeyIdentifier.getPublicKey(signatureOfNewAccount, passwordOfNewAccountAsString);
				this.payerAccount = mkPayerAccount();
				this.signatureOfPayer = determineSignatureOfPayer();
				this.publicKeyOfNewAccountBase64 = mkPublicKeyOfNewAccountBase64(signatureOfNewAccount, publicKeyOfNewAccount);
				this.eoaType = getEOAType(signatureOfNewAccount);
				this.proposedGas = computeProposedGas(signatureOfNewAccount, signatureOfPayer);
				askForConfirmation(proposedGas);
				this.nonce = determineNonceOfPayer();
				this.gasPrice = determineGasPrice(remote);
				Signer<SignedTransactionRequest<?>> signer = signatureOfPayer.getSigner(payerAccount.keys(passwordOfPayerAsString, signatureOfPayer).getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
				this.request = mkRequest(payer, signer, balance);
				StorageReference referenceOfNewAccount = executeRequest();
				TransactionReference transaction = computeTransaction(request);
				Optional<Path> file = dealWithBindingOfKeysToNewAccount(referenceOfNewAccount);
				GasCounter gasCosts = computeGasCosts(remote, request);
				reportOutput(transaction, referenceOfNewAccount, file, gasCosts);
			}
			finally {
				passwordOfNewAccountAsString = null;
				passwordOfPayerAsString = null;
				Arrays.fill(password, ' ');
				Arrays.fill(passwordOfPayer, ' ');
			}
		}

		private StorageReference executeRequest() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return remote.addConstructorCallTransaction(request);
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("The creation transaction failed! are the key pair of the payer and its password correct?", e);
			}
		}

		private ConstructorCallTransactionRequest mkRequest(StorageReference payer, Signer<SignedTransactionRequest<?>> signer, BigInteger balance) throws NodeException, TimeoutException, InterruptedException {
			try {
				return TransactionRequests.constructorCall
						(signer, payer, nonce, remote.getConfig().getChainId(), proposedGas, gasPrice, remote.getTakamakaCode(),
								ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
								StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyOfNewAccountBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same signature algorithm, it cannot be invalid
				throw new RuntimeException(e);
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

		private Account mkPayerAccount() throws CommandException {
			try {
				return Accounts.of(payer, dir);
			}
			catch (IOException e) {
				throw new CommandException("Cannot read the key pair of the account: it was expected to be in file \"" + dir.resolve(payer.toString()) + ".pem\"", e);
			}
		}
	}

	private class CreationFromFaucet {
		private final RemoteNode remote;
		private final StorageReference gamete;
		private final String publicKetOfNewAccountBase64;
		private final SignatureAlgorithm signatureOfFaucet;
		private final BigInteger proposedGas;
		private final BigInteger gasPrice;
		private final InstanceMethodCallTransactionRequest request;
		private final NonVoidMethodSignature faucetMethod;

		private CreationFromFaucet(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;

			String passwordOfNewAccountAsString = new String(password);

			try {
				this.gamete = getGamete();
				SignatureAlgorithm signatureOfNewAccount = getSignatureAlgorithmOfNewAccount(remote);
				PublicKey publicKeyOfNewAccount = publicKeyIdentifier.getPublicKey(signatureOfNewAccount, passwordOfNewAccountAsString);
				ClassType eoaType = getEOAType(signatureOfNewAccount);
				this.publicKetOfNewAccountBase64 = mkPublicKeyOfNewAccountBase64(signatureOfNewAccount, publicKeyOfNewAccount);
				this.signatureOfFaucet = SignatureAlgorithms.empty(); // we use an empty signature algorithm, since the faucet is unsigned
				this.faucetMethod = getFaucetMethod(signatureOfNewAccount, eoaType);
				this.proposedGas = computeProposedGas(signatureOfNewAccount, signatureOfFaucet);
				this.gasPrice = determineGasPrice(remote);
				this.request = mkRequest(balance);
				StorageReference referenceOfNewAccount = executeRequest();
				TransactionReference transaction = computeTransaction(request);
				Optional<Path> file = dealWithBindingOfKeysToNewAccount(referenceOfNewAccount);
				GasCounter gasCosts = computeGasCosts(remote, request);
				reportOutput(transaction, referenceOfNewAccount, file, gasCosts);
			}
			finally {
				passwordOfNewAccountAsString = null;
				Arrays.fill(password, ' ');
			}
		}

		private InstanceMethodCallTransactionRequest mkRequest(BigInteger balance) throws NodeException, TimeoutException, InterruptedException {
			try {
				// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
				KeyPair keyPair = signatureOfFaucet.getKeyPair();
				Signer<SignedTransactionRequest<?>> signer = signatureOfFaucet.getSigner(keyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

				// we use a random nonce: although the nonce is not checked for calls to the faucet,
				// this avoids the risk of the request being rejected because it is repeated
				return TransactionRequests.instanceMethodCall
						(signer, gamete, new BigInteger(64, new SecureRandom()), remote.getConfig().getChainId(), proposedGas, gasPrice, remote.getTakamakaCode(),
						faucetMethod, gamete, StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKetOfNewAccountBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same (empty!) signature algorithm, thus it cannot be invalid
				throw new RuntimeException(e);
			}
		}

		private StorageReference executeRequest() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return remote.addInstanceMethodCallTransaction(request)
						.orElseThrow(() -> new CommandException(faucetMethod + " should not return void"))
						.asReturnedReference(faucetMethod, CommandException::new);
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("The creation transaction failed! Is the unsigned faucet open?", e);
			}
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

	private static BigInteger determineGasPrice(RemoteNode remote) throws CommandException, NodeException, TimeoutException, InterruptedException {
		try {
			return GasHelpers.of(remote).getGasPrice();
		}
		catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
			throw new CommandException("Cannot determine the nonce of the payer and the current gas price!", e);
		}
	}

	private static BigInteger computeProposedGas(SignatureAlgorithm signatureOfNewAccount, SignatureAlgorithm signatureOfPayer) throws CommandException {
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

	private static TransactionReference computeTransaction(TransactionRequest<?> request) throws CommandException {
		try {
			Hasher<TransactionRequest<?>> hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
			return TransactionReferences.of(hasher.hash(request));
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The sha256 hashing algorithm is not available");
		}
	}

	private static String mkPublicKeyOfNewAccountBase64(SignatureAlgorithm signatureOfNewAccount, PublicKey publicKeyOfNewAccount) {
		try {
			return Base64.toBase64String(signatureOfNewAccount.encodingOf(publicKeyOfNewAccount));
		}
		catch (InvalidKeyException e) {
			// the key has been created with the same signature algorithm, it cannot be invalid
			throw new RuntimeException(e);
		}
	}

	private Optional<Path> dealWithBindingOfKeysToNewAccount(StorageReference referenceOfNewAccount) throws CommandException {
		if (publicKeyIdentifier.keys != null) {
			Entropy entropy;

			try {
				entropy = Entropies.load(publicKeyIdentifier.keys);
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + publicKeyIdentifier.keys + "\"!", e);
			}

			var newAccount = Accounts.of(entropy, referenceOfNewAccount);
			Path file = outputDir.resolve(newAccount + ".pem");
			try {
				newAccount.dump(file);
			}
			catch (IOException e) {
				throw new CommandException("Cannot save the key pair of the account in file \"" + newAccount + ".pem\"!");
			}

			return Optional.of(file);
		}
		else
			return Optional.empty();
	}

	private static GasCounter computeGasCosts(RemoteNode remote, TransactionRequest<?> request) throws CommandException, InterruptedException, NodeException, TimeoutException {
		try {
			return GasCounters.of(remote, new TransactionRequest<?>[] { request });
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("Cannot find the creation request in the store of the node, maybe a sudden history change has occurred?", e);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("A cryptographic algorithm is not available", e);
		}
	}

	/**
	 * The output of this command.
	 */
	protected static abstract class AbstractAccountCreationOutput implements AccountCreationOutput {

		/**
		 * The transaction that created the account.
		 */
		private final TransactionReference transaction;

		/**
		 * The reference of the bound account.
		 */
		private final StorageReference account;

		/**
		 * The path of the created key pair file for the account that has been bound.
		 * This is missing if the account has been created for a public key, not for a key pair,
		 * so that it remains to be bound to the key pair.
		 */
		private final Optional<Path> file;

		/**
		 * The amount of gas consumed for the CPU cost for creating the account.
		 */
		private final BigInteger gasConsumedForCPU;

		/**
		 * The amount of gas consumed for the RAM cost for creating the account.
		 */
		private final BigInteger gasConsumedForRAM;

		/**
		 * The amount of gas consumed for the storage cost for creating the account.
		 */
		private final BigInteger gasConsumedForStorage;

		/**
		 * Builds the output of the command.
		 */
		protected AbstractAccountCreationOutput(TransactionReference transaction, StorageReference account, Optional<Path> file, GasCounter gasCounter) {
			this.transaction = transaction;
			this.account = account;
			this.file = file;
			this.gasConsumedForCPU = gasCounter.forCPU();
			this.gasConsumedForRAM = gasCounter.forRAM();
			this.gasConsumedForStorage = gasCounter.forStorage();
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public AbstractAccountCreationOutput(AccountCreationOutputJson json) throws InconsistentJsonException {
			this.transaction = Objects.requireNonNull(json.getTransaction(), "transaction cannot be null", InconsistentJsonException::new).unmap();
			this.account = Objects.requireNonNull(json.getAccount(), "account cannot be null", InconsistentJsonException::new).unmap()
					.asReference(value -> new InconsistentJsonException("The reference of the created account must be a storage reference, not a " + value.getClass().getName()));

			try {
				this.file = Optional.ofNullable(json.getFile()).map(Paths::get);
			}
			catch (InvalidPathException e) {
				throw new InconsistentJsonException(e);
			}

			this.gasConsumedForCPU = Objects.requireNonNull(json.getGasConsumedForCPU(), "gasConsumedForCPU cannot be null", InconsistentJsonException::new);
			this.gasConsumedForRAM = Objects.requireNonNull(json.getGasConsumedForRAM(), "gasConsumedForRAM cannot be null", InconsistentJsonException::new);
			this.gasConsumedForStorage = Objects.requireNonNull(json.getGasConsumedForStorage(), "gasConsumedForStorage cannot be null", InconsistentJsonException::new);
		}

		@Override
		public TransactionReference getTransaction() {
			return transaction;
		}

		@Override
		public StorageReference getAccount() {
			return account;
		}

		@Override
		public Optional<Path> getFile() {
			return file;
		}

		@Override
		public BigInteger getGasConsumedForCPU() {
			return gasConsumedForCPU;
		}

		@Override
		public BigInteger getGasConsumedForRAM() {
			return gasConsumedForRAM;
		}

		@Override
		public BigInteger getGasConsumedForStorage() {
			return gasConsumedForStorage;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("A new account " + account + " has been created by transaction " + asTransactionReference(transaction) + ".\n");

			if (file.isPresent())
				sb.append("Its key pair has been saved into the file " + asPath(file.get()) + ".\n");
			else {
				sb.append("The owner of the key pair can bind it now to its address with:\n");
				sb.append("\n");
				sb.append(asCommand("  moka keys bind file_containing_the_key_pair_of_the_account --password --reference " + account + "\n"));
			}

			sb.append("\n");

			sb.append("Gas consumption:\n");
			sb.append(" * total: " + gasConsumedForCPU.add(gasConsumedForRAM).add(gasConsumedForStorage) + "\n");
			sb.append(" * for CPU: " + gasConsumedForCPU + "\n");
			sb.append(" * for RAM: " + gasConsumedForRAM + "\n");
			sb.append(" * for storage: " + gasConsumedForStorage + "\n");

			return sb.toString();
		}
	}
}