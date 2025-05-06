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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.api.GasCost;
import io.hotmoka.moka.api.AccountCreationOutput;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.moka.internal.json.AccountCreationOutputJson;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
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
public abstract class AbstractAccountCreation extends AbstractGasCostCommand {

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

	@ArgGroup(exclusive = true, multiplicity = "1", heading = "The public key of the new account must be specified in either of these two alternative ways:\n")
	private PublicKeyIdentifier publicKeyIdentifier;

	@Option(names = "--password", description = "the password of the key pair specified through --keys", interactive = true, defaultValue = "")
	private char[] password;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

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
	 * @param referenceOfNewAccount the reference of the new account
	 * @param file the file where the key pair of the new account has been saved, if any
	 * @param gasCost the gas cost incurred for the creation of the new account
	 * @param gasPrice the gas price used for the account creation transaction
	 * @throws CommandException if the report fails
	 */
	protected abstract void reportOutput(StorageReference referenceOfNewAccount, Optional<Path> file, GasCost gasCost, BigInteger gasPrice) throws CommandException;

	private class CreationFromPayer {
		private final RemoteNode remote;
		private final String publicKeyOfNewAccountBase64;
		private final ClassType eoaType;
		private final BigInteger gasLimit;
		private final BigInteger nonce;
		private final BigInteger gasPrice;
		private final ConstructorCallTransactionRequest request;

		private CreationFromPayer(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;

			String passwordOfNewAccountAsString = new String(password);
			String passwordOfPayerAsString = new String(passwordOfPayer);

			try {
				SignatureAlgorithm signatureOfNewAccount = getSignatureAlgorithmOfNewAccount(remote);
				SignatureAlgorithm signatureOfPayer = determineSignatureOf(payer, remote);
				this.publicKeyOfNewAccountBase64 = publicKeyIdentifier.getPublicKeyBase64(signatureOfNewAccount, passwordOfNewAccountAsString);
				this.eoaType = getEOAType(signatureOfNewAccount);
				this.gasLimit = determineGasLimit(() -> gasLimitHeuristic(signatureOfNewAccount, signatureOfPayer));
				this.gasPrice = determineGasPrice(remote);
				askForConfirmation("create the new account", gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(payer, remote);
				Signer<SignedTransactionRequest<?>> signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
				this.request = mkRequest(payer, signer, balance);
				StorageReference referenceOfNewAccount = executeRequest();
				Optional<Path> file = bindKeysToAccount(publicKeyIdentifier, referenceOfNewAccount, outputDir);
				GasCost gasCost = computeIncurredGasCost(remote, referenceOfNewAccount.getTransaction());
				reportOutput(referenceOfNewAccount, file, gasCost, gasPrice);
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

		private ConstructorCallTransactionRequest mkRequest(StorageReference payer, Signer<SignedTransactionRequest<?>> signer, BigInteger balance) throws CommandException, NodeException, TimeoutException, InterruptedException {
			try {
				return TransactionRequests.constructorCall
						(signer, payer, nonce, remote.getConfig().getChainId(), gasLimit, gasPrice, remote.getTakamakaCode(),
								ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
								StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyOfNewAccountBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				throw new CommandException("The key pair of " + payer + " seems corrupted!", e);
			}
		}
	}

	private class CreationFromFaucet {
		private final RemoteNode remote;
		private final StorageReference gamete;
		private final String publicKeyOfNewAccountBase64;
		private final SignatureAlgorithm signatureOfFaucet;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final InstanceMethodCallTransactionRequest request;
		private final NonVoidMethodSignature faucetMethod;

		private CreationFromFaucet(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.remote = remote;

			String passwordOfNewAccountAsString = new String(password);

			try {
				this.gamete = getGamete();
				SignatureAlgorithm signatureOfNewAccount = getSignatureAlgorithmOfNewAccount(remote);
				this.publicKeyOfNewAccountBase64 = publicKeyIdentifier.getPublicKeyBase64(signatureOfNewAccount, passwordOfNewAccountAsString);
				ClassType eoaType = getEOAType(signatureOfNewAccount);
				this.signatureOfFaucet = SignatureAlgorithms.empty(); // we use an empty signature algorithm, since the faucet is unsigned
				this.faucetMethod = getFaucetMethod(signatureOfNewAccount, eoaType);
				this.gasLimit = determineGasLimit(() -> gasLimitHeuristic(signatureOfNewAccount, signatureOfFaucet));
				this.gasPrice = determineGasPrice(remote);
				this.request = mkRequest(balance);
				StorageReference referenceOfNewAccount = executeRequest();
				Optional<Path> file = bindKeysToAccount(publicKeyIdentifier, referenceOfNewAccount, outputDir);
				GasCost gasCost = computeIncurredGasCost(remote, referenceOfNewAccount.getTransaction());
				reportOutput(referenceOfNewAccount, file, gasCost, gasPrice);
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
						(signer, gamete, new BigInteger(64, new SecureRandom()), remote.getConfig().getChainId(), gasLimit, gasPrice, remote.getTakamakaCode(),
						faucetMethod, gamete, StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyOfNewAccountBase64));
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

	private BigInteger gasLimitHeuristic(SignatureAlgorithm signatureOfNewAccount, SignatureAlgorithm signatureOfPayer) throws CommandException {
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

	/**
	 * The output of this command.
	 */
	protected static abstract class AbstractAccountCreationOutput extends AbstractGasCostCommandOutput implements AccountCreationOutput {

		/**
		 * The reference of the created account.
		 */
		private final StorageReference account;

		/**
		 * The path of the created key pair file for the account that has been created.
		 * This is missing if the account has been created for a public key, not for a key pair,
		 * so that it remains to be bound to the key pair.
		 */
		private final Optional<Path> file;

		/**
		 * Builds the output of the command.
		 */
		protected AbstractAccountCreationOutput(StorageReference account, Optional<Path> file, GasCost gasCost, BigInteger gasPrice) {
			super(gasCost, gasPrice);

			this.account = account;
			this.file = file;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		protected AbstractAccountCreationOutput(AccountCreationOutputJson json) throws InconsistentJsonException {
			super(json);

			this.account = Objects.requireNonNull(json.getAccount(), "account cannot be null", InconsistentJsonException::new).unmap()
					.asReference(value -> new InconsistentJsonException("The reference of the created account must be a storage reference, not a " + value.getClass().getName()));

			try {
				this.file = Optional.ofNullable(json.getFile()).map(Paths::get);
			}
			catch (InvalidPathException e) {
				throw new InconsistentJsonException(e);
			}
		}

		@Override
		public TransactionReference getTransaction() {
			return account.getTransaction();
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
		public String toString() {
			var sb = new StringBuilder();

			sb.append("A new account " + account + " has been created by transaction " + asTransactionReference(getTransaction()) + ".\n");

			if (file.isPresent())
				sb.append("Its key pair has been saved into the file " + asPath(file.get()) + ".\n");
			else {
				sb.append("The owner of the key pair can bind it now to its address with:\n");
				sb.append("\n");
				sb.append(asCommand("  moka keys bind file_containing_the_key_pair_of_the_account --password --reference " + account + "\n"));
			}

			sb.append("\n");

			toStringGasCost(sb);

			return sb.toString();
		}
	}
}