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

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.moka.NodesTendermintValidatorsSellOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.nodes.tendermint.validators.NodesTendermintValidatorsSellOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.NodesTendermintValidatorsSellOutputJson;
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
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "sell",
	description = "Place a sale offer of validation power.",
	showDefaultValues = true)
public class Sell extends AbstractGasCostCommand {

	@Parameters(index = "0", paramLabel = "<seller>", description = "the storage reference of the validator that sells part or all of its validation power; it also pays for the transaction", converter = StorageReferenceOptionConverter.class)
    private StorageReference payer;

	@Parameters(index = "1", description = "the amount of validation power that is placed on sale")
    private BigInteger power;

	@Parameters(index = "2", description = "the total price of the validation power that is placed on sale")
    private BigInteger price;

	@Parameters(index = "3", paramLabel = "<milliseconds>", description = "the duration of validity of the offer from time of placement", defaultValue = "300_000L")
    private long duration;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the key pair of the seller can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--buyer", paramLabel = "<storage reference>", description = "the only buyer allowed to accept the sale offer; if missing, everybody can accept the sale offer", converter = StorageReferenceOptionConverter.class)
    private StorageReference buyer;

	@Option(names = { "--password-of-payer" , "--password-of-seller" }, description = "the password of the seller", interactive = true, defaultValue = "")
	private char[] passwordOfPayer;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected final void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Body(remote);
	}

	private class Body {
		private final RemoteNode remote;
		private final String chainId;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final BigInteger gasPrice;
		private final TransactionReference classpath;
		private final BigInteger nonce;
		private final StorageReference validators;
		private final InstanceMethodCallTransactionRequest request;
		private final NonVoidMethodSignature method;
		private final SignatureAlgorithm signatureOfPayer;

		private Body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			/*if (power.signum() <= 0)
			throw new IllegalArgumentException("the validation power to sell must be positive");

		if (cost.signum() < 0)
			throw new IllegalArgumentException("the cost of the sale must be non-negative");

		if (duration <= 0L)
			throw new IllegalArgumentException("the duration of the sale must be positive");*/

			String passwordOfPayerAsString = new String(passwordOfPayer);
			
			try {
				this.remote = remote;
				this.chainId = remote.getConfig().getChainId();
				this.signatureOfPayer = determineSignatureOf(payer, remote);
				this.signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
				this.validators = getValidators();
				this.gasLimit = determineGasLimit(this::gasLimitHeuristic);
				this.classpath = getClasspathAtCreationTimeOf(payer, remote);
				this.method = mkMethod();
				this.gasPrice = determineGasPrice(remote);
				askForConfirmation("place a sale offer of " + power + " units of validation power at the price of " + panas(price), gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(payer, remote);
				this.request = mkRequest();
				report(json(), executeRequest(), NodesTendermintValidatorsSellOutputs.Encoder::new);
			}
			finally {
				passwordOfPayerAsString = null;
				Arrays.fill(passwordOfPayer, ' ');
			}
		}

		private NonVoidMethodSignature mkMethod() {
			if (buyer == null)
				return MethodSignatures.ofNonVoid(StorageTypes.ABSTRACT_VALIDATORS, "place", StorageTypes.SHARED_ENTITY_OFFER, StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG);
			else
				return MethodSignatures.ofNonVoid(StorageTypes.ABSTRACT_VALIDATORS, "place", StorageTypes.SHARED_ENTITY_OFFER, StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG, StorageTypes.VALIDATOR);
		}

		private Output executeRequest() throws CommandException, NodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<GasCost> gasCost = Optional.empty();
			Optional<String> errorMessage = Optional.empty();
			Optional<StorageReference> offer = Optional.empty();

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
						StorageReference result = remote.addInstanceMethodCallTransaction(request)
							.orElseThrow(() -> new CommandException(method + " should not return void"))
							.asReturnedReference(method, CommandException::new);

						if (!json())
							System.out.println("done.");

						offer = Optional.of(result);
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
				throw new CommandException("Transaction " + transaction + " has been rejected!", e);
			}

			return new Output(transaction, offer, gasCost, errorMessage);
		}

		private InstanceMethodCallTransactionRequest mkRequest() throws CommandException {
			try {
				if (buyer == null)
					return TransactionRequests.instanceMethodCall
						(signer, payer, nonce, chainId, gasLimit, gasPrice, classpath, method,
								validators, StorageValues.bigIntegerOf(BigInteger.ZERO), payer, StorageValues.bigIntegerOf(power), StorageValues.bigIntegerOf(price), StorageValues.longOf(duration));
				else
					return TransactionRequests.instanceMethodCall
						(signer, payer, nonce, chainId, gasLimit, gasPrice, classpath, method,
								validators, StorageValues.bigIntegerOf(BigInteger.ZERO), payer, StorageValues.bigIntegerOf(power), StorageValues.bigIntegerOf(price), StorageValues.longOf(duration), buyer);
			}
			catch (InvalidKeyException | SignatureException e) {
				throw new CommandException("The current key pair of " + payer + " seems corrupted!", e);
			}
		}

		private BigInteger gasLimitHeuristic() throws CommandException {
			return BigInteger.TWO.multiply(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
		}

		private StorageReference getValidators() throws NodeException, TimeoutException, InterruptedException, CommandException {
			var manifest = remote.getManifest();
			var takamakaCode = remote.getTakamakaCode();

			try {
				return remote.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_VALIDATORS + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_VALIDATORS, CommandException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Cannot access the set of validators of the network");
			}
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractGasCostCommandOutput implements NodesTendermintValidatorsSellOutput {

		/**
		 * The sale offer that has been placed, if any.
		 */
		private final Optional<StorageReference> offer;

		/**
		 * Builds the output of the command.
		 */
		private Output(TransactionReference transaction, Optional<StorageReference> offer, Optional<GasCost> gasCost, Optional<String> errorMessage) {
			super(transaction, gasCost, errorMessage);

			this.offer = offer;
		}
	
		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTendermintValidatorsSellOutputJson json) throws InconsistentJsonException {
			super(json);

			var offer = json.getOffer();
			if (offer.isEmpty())
				this.offer = Optional.empty();
			else
				this.offer = Optional.of(offer.get().unmap().asReference(value -> new InconsistentJsonException("The reference to the offer must be a storage reference, not a " + value.getClass().getName())));
		}

		@Override
		public Optional<StorageReference> getOffer() {
			return offer;
		}

		@Override
		protected void toString(StringBuilder sb) {
			offer.ifPresent(o -> sb.append("Sale offer of validation power " + o + " has been placed and is ready to be accepted.\n")); // TODO: maybe add acceptance command
		}
	}
}