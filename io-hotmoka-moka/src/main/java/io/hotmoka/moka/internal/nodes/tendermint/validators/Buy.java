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

package io.hotmoka.moka.internal.nodes.tendermint.validators;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.moka.NodesTendermintValidatorsBuyOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.nodes.tendermint.validators.NodesTendermintValidatorsBuyOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.NodesTendermintValidatorsBuyOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "buy",
	description = "Accept a sale offer of validation power.",
	showDefaultValues = true)
public class Buy extends AbstractGasCostCommand {

	@Parameters(index = "0", paramLabel = "<buyer>", description = "the storage reference of the validator that accepts the sale offer and buys its validation power; it also pays for the transaction", converter = StorageReferenceOptionConverter.class)
    private StorageReference payer;

	@Parameters(index = "1", description = "the storage reference of the sale offer to accept", converter = StorageReferenceOptionConverter.class)
    private StorageReference offer;

	@Option(names = { "--password-of-payer" , "--password-of-buyer" }, description = "the password of the buyer", interactive = true, defaultValue = "")
	private char[] passwordOfPayer;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the key pair of the buyer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected final void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Body(remote);
	}

	private class Body {
		private final RemoteNode remote;
		private final String chainId;
		private final SignatureAlgorithm signatureOfPayer;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final StorageReference validators;
		private final BigInteger gasLimit;
		private final TransactionReference classpath;
		private final TransactionReference takamakaCode;
		private final StorageReference manifest;
		private final int buyerSurcharge;
		private final BigInteger priceWithSurcharge;
		private final BigInteger power;
		private final BigInteger gasPrice;
		private final BigInteger nonce;
		private final InstanceMethodCallTransactionRequest request;
		private final VoidMethodSignature method;

		private Body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			String passwordOfPayerAsString = new String(passwordOfPayer);

			try {
				this.remote = remote;
				this.chainId = remote.getConfig().getChainId();
				this.signatureOfPayer = determineSignatureOf(payer, remote);
				this.takamakaCode = remote.getTakamakaCode();
				this.manifest = remote.getManifest();
				this.signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
				this.validators = getValidators();
				this.buyerSurcharge = getBuyerSurcharge();
				this.gasLimit = determineGasLimit(this::gasLimitHeuristic);
				this.classpath = getClasspathAtCreationTimeOf(offer, remote);
				this.priceWithSurcharge = getPriceOfOffer().multiply(BigInteger.valueOf(buyerSurcharge + 100_000_000L)).divide(BigInteger.valueOf(100_000_000L));
				this.power = getPowerOfOffer();
				this.method = MethodSignatures.ofVoid(StorageTypes.ABSTRACT_VALIDATORS, "accept", StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR, StorageTypes.SHARED_ENTITY_OFFER);
				this.gasPrice = determineGasPrice(remote);
				askForConfirmation("accept a sale offer of " + power + " units of validation power at the price of " + panas(priceWithSurcharge) + " including surcharge", gasLimit, gasPrice, yes || json());				
				this.nonce = determineNonceOf(payer, remote);
				this.request = mkRequest();
				report(json(), executeRequest(), NodesTendermintValidatorsBuyOutputs.Encoder::new);
			}
			finally {
				passwordOfPayerAsString = null;
				Arrays.fill(passwordOfPayer, ' ');
			}
		}

		private InstanceMethodCallTransactionRequest mkRequest() throws CommandException {
			try {
				return TransactionRequests.instanceMethodCall
					(signer, payer, nonce, chainId, gasLimit, gasPrice, classpath, method, validators, StorageValues.bigIntegerOf(priceWithSurcharge), payer, offer);
			}
			catch (InvalidKeyException | SignatureException e) {
				throw new CommandException("The current key pair of " + payer + " seems corrupted!", e);
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
							System.out.println("failed.");

						errorMessage = Optional.of(e.getMessage());
					}

					gasCost = Optional.of(computeIncurredGasCost(remote, gasPrice, transaction));
				}
			}
			catch (TransactionRejectedException e) {
				if (!json())
					System.out.println("rejected.");

				errorMessage = Optional.of(e.getMessage());
			}

			return new Output(transaction, gasCost, errorMessage);
		}

		private BigInteger gasLimitHeuristic() throws CommandException {
			return BigInteger.TWO.multiply(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
		}

		private BigInteger getPriceOfOffer() throws CommandException, NodeException, TimeoutException, InterruptedException {
			var method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getCost", StorageTypes.BIG_INTEGER);

			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, method, offer))
						.orElseThrow(() -> new CommandException(method + " should not return void"))
						.asReturnedBigInteger(method, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Cannot access the price of the sale offer", e);
			}
		}

		private BigInteger getPowerOfOffer() throws CommandException, NodeException, TimeoutException, InterruptedException {
			var method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getSharesOnSale", StorageTypes.BIG_INTEGER);

			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, method, offer))
						.orElseThrow(() -> new CommandException(method + " should not return void"))
						.asReturnedBigInteger(method, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Cannot access the number of shares offered on sale", e);
			}
		}

		private StorageReference getValidators() throws NodeException, TimeoutException, InterruptedException, CommandException {
			var manifest = remote.getManifest();

			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_VALIDATORS + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_VALIDATORS, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Cannot access the set of validators of the network", e);
			}
		}

		private int getBuyerSurcharge() throws CommandException, NodeException, TimeoutException, InterruptedException {
			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE, validators))
					.orElseThrow(() -> new CommandException(MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE + " should not return void"))
					.asReturnedInt(MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Cannot determine the buyer surcharge of the network", e);
			}
		}
	}

	/**
	 * The output of this command.
	 */
	@Immutable
	public static class Output extends AbstractGasCostCommandOutput implements NodesTendermintValidatorsBuyOutput {

		/**
		 * Builds the output of the command.
		 * 
		 * @param transaction the creation transaction
		 * @param gasCost the gas cost of the transaction, if any
		 * @param errorMessage the error message of the transaction, if any
		 */
		private Output(TransactionReference transaction, Optional<GasCost> gasCost, Optional<String> errorMessage) {
			super(transaction, gasCost, errorMessage);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTendermintValidatorsBuyOutputJson json) throws InconsistentJsonException {
			super(json);
		}

		@Override
		protected void toString(StringBuilder sb) {
		}
	}
}