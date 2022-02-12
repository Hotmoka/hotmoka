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
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.Account;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.helpers.GasHelper;
import io.hotmoka.helpers.NonceHelper;
import io.hotmoka.helpers.SignatureHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
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
			if (passwordOfSeller != null && interactive)
				throw new IllegalArgumentException("the password of the seller validator can be provided as command switch only in non-interactive mode");

			if (power.signum() <= 0)
				throw new IllegalArgumentException("the validation power to sell must be positive");

			if (cost.signum() < 0)
				throw new IllegalArgumentException("the cost of the sale must be non-negative");

			if (duration <= 0L)
				throw new IllegalArgumentException("the duration of the sale must be positive");

			passwordOfSeller = ensurePassword(passwordOfSeller, "the seller validator", interactive, false);

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				GasHelper gasHelper = new GasHelper(node);
				NonceHelper nonceHelper = new NonceHelper(node);
				TransactionReference takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				StorageReference validators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));
				StorageReference seller = new StorageReference(SellValidation.this.seller);
				SignatureAlgorithm<SignedTransactionRequest> algorithm = new SignatureHelper(node).signatureAlgorithmFor(seller);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				KeyPair keys = readKeys(new Account(seller), node, passwordOfSeller);
				Signer signer = Signer.with(algorithm, keys);

				askForConfirmation(gasLimit.multiply(BigInteger.TWO));

				ConstructorCallTransactionRequest request1 = new ConstructorCallTransactionRequest(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
					new ConstructorSignature(ClassType.SHARED_ENTITY_OFFER, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, BasicTypes.LONG),
					seller, new BigIntegerValue(power), new BigIntegerValue(cost), new LongValue(duration));

				StorageReference newOffer = node.addConstructorCallTransaction(request1);
				
				InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
					(signer, seller, nonceHelper.getNonceOf(seller), chainId, gasLimit, gasHelper.getSafeGasPrice(), takamakaCode,
					new VoidMethodSignature(ClassType.SHARED_ENTITY, "place", ClassType.BIG_INTEGER, ClassType.SHARED_ENTITY_OFFER),
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