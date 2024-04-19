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

import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "buy-validation",
	description = "Accept a sale offer of validation power",
	showDefaultValues = true)
public class BuyValidation extends AbstractCommand {

	@Parameters(index = "0", description = "the reference to the validator that accepts and pays the sale offer of validation power")
    private String buyer;

	@Parameters(index = "1", description = "the reference to the sale offer that gets accepted")
    private String offer;

	@Option(names = { "--password-of-buyer" }, description = "the password of the buyer validator; if not specified, it will be asked interactively")
    private String passwordOfBuyer;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for accepting the offer", defaultValue = "500000") 
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
			if (passwordOfBuyer != null && interactive)
				throw new IllegalArgumentException("the password of the buyer validator can be provided as command switch only in non-interactive mode");

			passwordOfBuyer = ensurePassword(passwordOfBuyer, "the buyer validator", interactive, false);

			try (Node node = this.node = RemoteNodes.of(uri, 10_000L)) {
				var gasHelper = GasHelpers.of(node);
				var nonceHelper = NonceHelpers.of(node);
				var takamakaCode = node.getTakamakaCode();
				var manifest = node.getManifest();
				var validators = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest));
				var buyer = StorageValues.reference(BuyValidation.this.buyer);
				var algorithm = SignatureHelpers.of(node).signatureAlgorithmFor(buyer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();
				KeyPair keys = readKeys(Accounts.of(buyer), node, passwordOfBuyer);
				var signer = algorithm.getSigner(keys.getPrivate(), SignedTransactionRequest<?>::toByteArrayWithoutSignature);				
				InstanceMethodCallTransactionRequest request;

				int buyerSurcharge = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT), validators))).getValue();

				StorageReference offer = StorageValues.reference(BuyValidation.this.offer);

				BigInteger cost = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "getCost", StorageTypes.BIG_INTEGER), offer))).getValue();
				BigInteger costWithSurcharge = cost.multiply(BigInteger.valueOf(buyerSurcharge + 100_000_000L)).divide(_100_000_000);

				askForConfirmation(gasLimit, costWithSurcharge);

				request = TransactionRequests.instanceMethodCall
					(signer, buyer, nonceHelper.getNonceOf(buyer), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
					MethodSignatures.ofVoid(StorageTypes.ABSTRACT_VALIDATORS, "accept", StorageTypes.BIG_INTEGER, StorageTypes.VALIDATOR, StorageTypes.SHARED_ENTITY_OFFER),
					validators, StorageValues.bigIntegerOf(costWithSurcharge), buyer, offer);

				node.addInstanceMethodCallTransaction(request);
				System.out.println("Offer accepted");

				printCosts(request);
			}
		}

		private void askForConfirmation(BigInteger gas, BigInteger cost) {
			if (interactive)
				yesNo("Do you really want to spend up to " + gas + " gas units and " + cost + " panareas to accept the sale of validation power [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
			if (printCosts)
				BuyValidation.this.printCosts(node, requests);
		}
	}
}