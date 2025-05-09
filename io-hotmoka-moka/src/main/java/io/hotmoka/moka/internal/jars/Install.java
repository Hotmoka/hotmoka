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

package io.hotmoka.moka.internal.jars;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.jars.JarsInstallOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.converters.TransactionReferenceOptionConverter;
import io.hotmoka.moka.internal.json.JarsInstallOutputJson;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "install",
	description = "Install a jar in a node.",
	showDefaultValues = true)
public class Install extends AbstractGasCostCommand {

	@Parameters(description = "the storage reference of the account that pays for installing the jar", converter = StorageReferenceOptionConverter.class)
	private StorageReference payer;

	@Parameters(description = "the path of the jar to install")
	private Path jar;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the key pair of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--password-of-payer", description = "the password of the key pair of the payer account", interactive = true, defaultValue = "")
    private char[] passwordOfPayer;

	@Option(names = "--libs", paramLabel = "<transaction references>", description = "the already installed dependencies of the jar; use --libs repeatedly to include more dependencies; if missing, takamakaCode will be used", converter = TransactionReferenceOptionConverter.class)
	private List<TransactionReference> libs;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, NodeException {
		String passwordOfPayerAsString = new String(passwordOfPayer);

		try {
			String chainId = remote.getConfig().getChainId();
			byte[] bytesOfJar = readBytesOfJar();
			TransactionReference[] dependencies = computeDependencies(remote);
			var signatureOfPayer = determineSignatureOf(payer, remote);
			Signer<SignedTransactionRequest<?>> signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
			TransactionReference classpath = getClasspathAtCreationTimeOf(payer, remote);
			BigInteger gasLimit = determineGasLimit(() -> gasLimitHeuristic(bytesOfJar, signatureOfPayer));
			BigInteger gasPrice = determineGasPrice(remote);
			askForConfirmation("install the jar", gasLimit, gasPrice, yes || json());
			BigInteger nonce = determineNonceOf(payer, remote);
			JarStoreTransactionRequest request = mkRequest(chainId, bytesOfJar, dependencies, signer, classpath, gasLimit, gasPrice, nonce);
			report(json(), executeRequest(remote, request, gasPrice), JarsInstallOutputs.Encoder::new);
		}
		finally {
			passwordOfPayerAsString = null;
			Arrays.fill(passwordOfPayer, ' ');
		}
	}

	private Output executeRequest(RemoteNode remote, JarStoreTransactionRequest request, BigInteger gasPrice) throws CommandException, NodeException, TimeoutException, InterruptedException {
		TransactionReference transaction = computeTransaction(request);
		Optional<TransactionReference> jar = Optional.empty();
		Optional<GasCost> gasCost = Optional.empty();
		Optional<String> errorMessage = Optional.empty();

		try {
			if (post()) {
				if (!json())
					System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");

				remote.postJarStoreTransaction(request);

				if (!json())
					System.out.println("done.");
			}
			else {
				if (!json())
					System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");

				try {
					jar = Optional.of(remote.addJarStoreTransaction(request));
					if (!json())
						System.out.println("done.");
				}
				catch (TransactionException e) {
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

		return new Output(transaction, jar, gasCost, errorMessage);
	}

	private JarStoreTransactionRequest mkRequest(String chainId, byte[] bytes, TransactionReference[] dependencies,
			Signer<SignedTransactionRequest<?>> signer, TransactionReference classpath, BigInteger gasLimit,
			BigInteger gasPrice, BigInteger nonce) throws CommandException {

		try {
			return TransactionRequests.jarStore(
					signer,
					payer,
					nonce,
					chainId,
					gasLimit,
					gasPrice,
					classpath,
					bytes,
					dependencies
			);
		}
		catch (InvalidKeyException | SignatureException e) {
			throw new CommandException("The key pair of " + payer + " seems corrupted!", e);
		}
	}

	private BigInteger gasLimitHeuristic(byte[] bytes, SignatureAlgorithm signatureOfPayer) {
		return _100_000.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer)).add(BigInteger.valueOf(200).multiply(BigInteger.valueOf(bytes.length)));
	}

	private TransactionReference[] computeDependencies(RemoteNode remote) throws NodeException, TimeoutException, InterruptedException {
		if (libs != null && !libs.isEmpty())
			return libs.stream().distinct().toArray(TransactionReference[]::new);
		else
			return new TransactionReference[] { remote.getTakamakaCode() };
	}

	private byte[] readBytesOfJar() throws CommandException {
		try {
			return Files.readAllBytes(jar);
		}
		catch (IOException e) {
			throw new CommandException("Cannot access the jar file!", e);
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractGasCostCommandOutput implements JarsInstallOutput {

		/**
		 * The reference of the jar installed in the node, if any.
		 */
		private final Optional<TransactionReference> jar;

		/**
		 * Builds the output of the command.
		 * 
		 * @param transaction the creation transaction
		 * @param jar the reference to the jar that has been installed, if any
		 * @param gasCost the gas cost of the transaction, if any
		 * @param errorMessage the error message of the transaction, if any
		 */
		private Output(TransactionReference transaction, Optional<TransactionReference> jar, Optional<GasCost> gasCost, Optional<String> errorMessage) {
			super(transaction, gasCost, errorMessage);

			this.jar = jar;
		}
	
		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(JarsInstallOutputJson json) throws InconsistentJsonException {
			super(json);

			var jar = json.getJar();
			if (jar.isEmpty())
				this.jar = Optional.empty();
			else
				this.jar = Optional.of(jar.get().unmap());
		}
	
		@Override
		public Optional<TransactionReference> getJar() {
			return jar;
		}

		@Override
		protected void toString(StringBuilder sb) {
			jar.ifPresent(o -> sb.append("The jar has been installed at " + o + ".\n"));
		}
	}
}