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
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.api.GasCost;
import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.api.jars.JarsInstallOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
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

	@Parameters(description = "the path of the jar to install")
	private Path jar;

	@Parameters(description = "the account that pays for installing the jar", converter = StorageReferenceOfAccountOptionConverter.class)
	private StorageReference payer;

	@Option(names = "--dir", description = "the path of the directory where the key pair of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--password-of-payer", description = "the password of the key pair of the payer account", interactive = true, defaultValue = "")
    private char[] passwordOfPayer;

	@Option(names = "--libs", description = "the references of the transactions that already installed the dependencies of the jar; if missing, takamakaCode will be used", converter = TransactionReferenceOptionConverter.class)
	private List<TransactionReference> libs;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, NodeException {
		String chainId = remote.getConfig().getChainId();
		byte[] bytesOfJar = readBytesOfJar();
		String passwordOfPayerAsString = new String(passwordOfPayer);
		TransactionReference[] dependencies = computeDependencies(remote);
		var payerAccount = mkPayerAccount(payer, dir);
		var signatureOfPayer = determineSignatureOf(payer, remote);
		Signer<SignedTransactionRequest<?>> signer = signatureOfPayer.getSigner(payerAccount.keys(passwordOfPayerAsString, signatureOfPayer).getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		TransactionReference classpath = getClasspathAtCreationTimeOf(payer, remote);
		BigInteger gasLimit = determineGasLimit(() -> gasLimitHeuristic(bytesOfJar, signatureOfPayer));
		BigInteger gasPrice = determineGasPrice(remote);
		askForConfirmation("install the jar", gasLimit, gasPrice, yes || json());
		BigInteger nonce = determineNonceOf(payer, remote);
		JarStoreTransactionRequest request = mkRequest(chainId, bytesOfJar, dependencies, signer, classpath, gasLimit, gasPrice, nonce);
		TransactionReference transaction = executeRequest(remote, request);
		var gasCost = computeIncurredGasCost(remote, request);
		report(json(), new Output(transaction, gasCost, gasPrice), JarsInstallOutputs.Encoder::new);
	}

	private TransactionReference executeRequest(RemoteNode remote, JarStoreTransactionRequest request) throws NodeException, TimeoutException, InterruptedException, CommandException {
		try {
			return remote.addJarStoreTransaction(request);
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw new CommandException("The jar install transaction failed! are the key pair of the payer and its password correct?", e);
		}
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
	public static class Output extends AbstractGasCostCommand.AbstractGasCostCommandOutput implements JarsInstallOutput {
	
		/**
		 * The reference of the jar installed in the node.
		 */
		private final TransactionReference jar;
	
		/**
		 * Builds the output of the command.
		 */
		private Output(TransactionReference jar, GasCost gasCost, BigInteger gasPrice) {
			super(gasCost, gasPrice);
	
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
	
			this.jar = Objects.requireNonNull(json.getJar(), "jar cannot be null", InconsistentJsonException::new).unmap();
		}
	
		@Override
		public TransactionReference getTransaction() {
			return jar;
		}
	
		@Override
		public TransactionReference getJar() {
			return jar;
		}
	
		@Override
		public String toString() {
			var sb = new StringBuilder();
	
			sb.append("The jar has been installed at " + jar + " by transaction " + asTransactionReference(jar) + ".\n");
			sb.append("\n");
	
			toStringGasCost(sb);
	
			return sb.toString();
		}
	}
}