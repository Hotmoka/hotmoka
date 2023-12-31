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

package io.hotmoka.tools.internal.moka;

import java.math.BigInteger;
import java.security.KeyPair;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.StorageTypes;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Node;
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

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

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

			try (var node = this.node = RemoteNodes.of(remoteNodeConfig(url))) {
				var gasHelper = GasHelpers.of(node);
				var nonceHelper = NonceHelpers.of(node);
				TransactionReference takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				var validators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));
				var seller = new StorageReference(SellValidation.this.seller);
				var algorithm = SignatureHelpers.of(node).signatureAlgorithmFor(seller);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				KeyPair keys = readKeys(Accounts.of(seller), node, passwordOfSeller);
				var signer = algorithm.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

				askForConfirmation(gasLimit.multiply(BigInteger.TWO));

				ConstructorCallTransactionRequest request1;
				if (buyer == null)
					request1 = new ConstructorCallTransactionRequest(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
						new ConstructorSignature(StorageTypes.SHARED_ENTITY_OFFER, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, BasicTypes.LONG),
						seller, new BigIntegerValue(power), new BigIntegerValue(cost), new LongValue(duration));
				else
					// the reserved buyer is specified as well
					request1 = new ConstructorCallTransactionRequest(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
						new ConstructorSignature(StorageTypes.SHARED_ENTITY_OFFER, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, BasicTypes.LONG, StorageTypes.PAYABLE_CONTRACT),
						seller, new BigIntegerValue(power), new BigIntegerValue(cost), new LongValue(duration), new StorageReference(buyer));

				StorageReference newOffer = node.addConstructorCallTransaction(request1);
				
				InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
					(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
					new VoidMethodSignature(StorageTypes.SHARED_ENTITY, "place", StorageTypes.BIG_INTEGER, StorageTypes.SHARED_ENTITY_OFFER),
					validators, new BigIntegerValue(BigInteger.ZERO), newOffer);

				node.addInstanceMethodCallTransaction(request2);
				System.out.println("Offer " + newOffer + " placed");

				printCosts(request1, request2);
			}
		}

		private void askForConfirmation(BigInteger gas) {
			if (interactive)
				yesNo("Do you really want to spend up to " + gas + " gas units to place on sale the validation power [Y/N] ");
		}

		private void printCosts(TransactionRequest<?>... requests) {
			if (printCosts)
				SellValidation.this.printCosts(node, requests);
		}
	}
}