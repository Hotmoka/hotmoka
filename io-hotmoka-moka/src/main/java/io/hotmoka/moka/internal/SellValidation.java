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
import java.net.URI;
import java.security.KeyPair;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "sell-validation",
	description = "Place a sale offer of validation power",
	showDefaultValues = true)
public class SellValidation extends AbstractCommand {

	@Parameters(index = "0", description = "the reference to the validator that sells part or all its validation power and pays to create the sale offer")
    private String seller;

	@Parameters(index = "1", description = "the validation power that is placed on sale")
    private BigInteger power;

	@Parameters(index = "2", description = "the cost of the validation power that is placed on sale")
    private BigInteger cost;

	@Parameters(index = "3", description = "the duration of validity of the offer, in milliseconds from now")
    private long duration;

	@Option(names = { "--buyer" }, description = "the reference to the only buyer allowed to accept the sale offer; if not specified, everybody can accept the sale offer")
    private String buyer;

	@Option(names = { "--password-of-seller" }, description = "the password of the seller validator; if not specified, it will be asked interactively")
    private String passwordOfSeller;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for placing the sale", defaultValue = "100000") 
	private BigInteger gasLimit;

	@Option(names = { "--print-costs" }, description = "print the incurred gas costs", defaultValue = "true") 
	private boolean printCosts;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;

		private Run() throws Exception {
			checkStorageReference(seller);

			if (buyer != null)
				checkStorageReference(buyer);

			if (passwordOfSeller != null && interactive)
				throw new IllegalArgumentException("the password of the seller validator can be provided as command switch only in non-interactive mode");

			if (power.signum() <= 0)
				throw new IllegalArgumentException("the validation power to sell must be positive");

			if (cost.signum() < 0)
				throw new IllegalArgumentException("the cost of the sale must be non-negative");

			if (duration <= 0L)
				throw new IllegalArgumentException("the duration of the sale must be positive");

			passwordOfSeller = ensurePassword(passwordOfSeller, "the seller validator", interactive, false);

			try (var node = this.node = RemoteNodes.of(uri, 10_000)) {
				var gasHelper = GasHelpers.of(node);
				var nonceHelper = NonceHelpers.of(node);
				var takamakaCode = node.getTakamakaCode();
				var manifest = node.getManifest();
				var validators = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest, StorageValues.NO_VALUES, IllegalArgumentException::new))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_VALIDATORS + " should not return void"));
				var seller = StorageValues.reference(SellValidation.this.seller, CommandException::new);
				var algorithm = SignatureHelpers.of(node).signatureAlgorithmFor(seller);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest, StorageValues.NO_VALUES, IllegalArgumentException::new))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_CHAIN_ID + " should not return void"))).getValue();
				KeyPair keys = readKeys(Accounts.of(seller), node, passwordOfSeller);
				Signer<SignedTransactionRequest<?>> signer = algorithm.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

				askForConfirmation(gasLimit.multiply(BigInteger.TWO));

				ConstructorCallTransactionRequest request1;
				if (buyer == null)
					request1 = TransactionRequests.constructorCall(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
							ConstructorSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG),
							new StorageValue[] { seller, StorageValues.bigIntegerOf(power), StorageValues.bigIntegerOf(cost), StorageValues.longOf(duration) },
							CommandException::new);
				else
					// the reserved buyer is specified as well
					request1 = TransactionRequests.constructorCall(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
							ConstructorSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG, StorageTypes.PAYABLE_CONTRACT),
							new StorageValue[] { seller, StorageValues.bigIntegerOf(power), StorageValues.bigIntegerOf(cost), StorageValues.longOf(duration),
							StorageValues.reference(buyer, s -> new CommandException("The buyer " + buyer + " is not a valid storage reference: " + s)) },
							IllegalArgumentException::new);

				StorageReference newOffer = node.addConstructorCallTransaction(request1);
				
				InstanceMethodCallTransactionRequest request2 = TransactionRequests.instanceMethodCall
					(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
					MethodSignatures.ofVoid(StorageTypes.SHARED_ENTITY, "place", StorageTypes.BIG_INTEGER, StorageTypes.SHARED_ENTITY_OFFER),
					validators, StorageValues.bigIntegerOf(BigInteger.ZERO), newOffer);

				node.addInstanceMethodCallTransaction(request2);
				System.out.println("Offer " + newOffer + " placed");

				printCosts(request1, request2);
			}
		}

		private void askForConfirmation(BigInteger gas) {
			if (interactive)
				yesNo("Do you really want to spend up to " + gas + " gas units to place on sale the validation power [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, UnknownReferenceException {
			if (printCosts)
				SellValidation.this.printCosts(node, requests);
		}
	}
}