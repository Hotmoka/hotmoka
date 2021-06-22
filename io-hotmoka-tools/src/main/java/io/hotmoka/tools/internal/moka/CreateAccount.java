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

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.views.GasHelper;
import io.hotmoka.views.NonceHelper;
import io.hotmoka.views.SignatureHelper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create-account",
	description = "Creates a new account",
	showDefaultValues = true)
public class CreateAccount extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the creation, or the string \"faucet\"", defaultValue = "faucet")
    private String payer;

	@Parameters(description = "the initial balance of the account", defaultValue = "0")
    private BigInteger balance;

	@Option(names = { "--balance-red" }, description = "the initial red balance of the account", defaultValue = "0")
    private BigInteger balanceRed;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Option(names = { "--signature" }, description = "the name of the signature algorithm to use for the new account {sha256dsa,ed25519,qtesla1,qtesla3,default}", defaultValue = "default")
	private String signature;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final KeyPair keys;
		private final String publicKey;
		private final NonceHelper nonceHelper;
		private final GasHelper gasHelper;
		private final StorageReference account;
		private final StorageReference manifest;
		private final TransactionReference takamakaCode;
		private final String chainId;
		private final SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithmOfNewAccount;
		private final String nameOfSignatureAlgorithmOfNewAccount;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				nameOfSignatureAlgorithmOfNewAccount = "default".equals(signature) ? node.getNameOfSignatureAlgorithmForRequests() : signature;
				signatureAlgorithmOfNewAccount = SignatureAlgorithmForTransactionRequests.mk(nameOfSignatureAlgorithmOfNewAccount);
				keys = signatureAlgorithmOfNewAccount.getKeyPair();
				publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
				manifest = node.getManifest();
				takamakaCode = node.getTakamakaCode();
				chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				nonceHelper = new NonceHelper(node);
				gasHelper = new GasHelper(node);
				account = createAccount();
				printOutcome();
				dumpKeysOfAccount();
			}
		}

		private void printOutcome() {
			System.out.println("A new account " + account + " has been created");
		}

		private void dumpKeysOfAccount() throws IOException, NoSuchAlgorithmException, ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException {
			dumpKeys(account, keys, node);
			System.out.println("The keys of the account have been saved into the files " + account + ".[pri|pub]");
		}

		private StorageReference createAccount() throws Exception {
			return "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation will succeed only if the gamete of the node supports an open unsigned faucet");

			StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

			String methodName;
			ClassType eoaType;
			BigInteger gas = gasForCreatingAccountWithSignature(nameOfSignatureAlgorithmOfNewAccount, node);

			switch (signature) {
			case "ed25519":
			case "sha256dsa":
			case "qtesla1":
			case "qtesla3":
				methodName = "faucet" + signature.toUpperCase();
				eoaType = new ClassType(ClassType.EOA.name + signature.toUpperCase());
				break;
			case "default":
				methodName = "faucet";
				eoaType = ClassType.EOA;
				break;
			default:
				throw new IllegalArgumentException("unknown signature algorithm " + signature);
			}

			// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
			SignatureAlgorithm<SignedTransactionRequest> signature = SignatureAlgorithmForTransactionRequests.empty();
			Signer signer = Signer.with(signature, signature.getKeyPair());
			InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
				(signer, gamete, nonceHelper.getNonceOf(gamete),
				chainId, gas, gasHelper.getGasPrice(), takamakaCode,
				new NonVoidMethodSignature(ClassType.GAMETE, methodName, eoaType, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING),
				gamete,
				new BigIntegerValue(balance), new BigIntegerValue(balanceRed), new StringValue(publicKey));

			try {
				return (StorageReference) node.addInstanceMethodCallTransaction(request);
			}
			finally {
				printCosts(node, request);
			}
		}

		private StorageReference createAccountFromPayer() throws Exception {
			StorageReference payer = new StorageReference(CreateAccount.this.payer);
			KeyPair keysOfPayer = readKeys(payer, node);

			ClassType eoaType;

			switch (signature) {
			case "ed25519":
			case "sha256dsa":
			case "qtesla1":
			case "qtesla3":
				eoaType = new ClassType(ClassType.EOA.name + signature.toUpperCase());
				break;
			case "default":
				eoaType = ClassType.EOA;
				break;
			default:
				throw new IllegalArgumentException("unknown signature algorithm " + signature);
			}

			SignatureAlgorithm<SignedTransactionRequest> signature = new SignatureHelper(node).signatureAlgorithmFor(payer);
			BigInteger gas1 = gasForCreatingAccountWithSignature(nameOfSignatureAlgorithmOfNewAccount, node);
			BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signature.getName(), node);

			askForConfirmation(balanceRed.signum() > 0 ? gas1.add(gas2).add(gas2) : gas1.add(gas2));

			Signer signer = Signer.with(signature, keysOfPayer);
			ConstructorCallTransactionRequest request1 = new ConstructorCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, gas1.add(gas2), gasHelper.getGasPrice(), takamakaCode,
				new ConstructorSignature(eoaType, ClassType.BIG_INTEGER, ClassType.STRING),
				new BigIntegerValue(balance), new StringValue(publicKey));
			StorageReference account = node.addConstructorCallTransaction(request1);

			if (balanceRed.signum() > 0) {
				InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
					(signer, payer, nonceHelper.getNonceOf(payer), chainId, gas2, gasHelper.getGasPrice(), takamakaCode,
					CodeSignature.RECEIVE_RED_BIG_INTEGER, account, new BigIntegerValue(balanceRed));
				node.addInstanceMethodCallTransaction(request2);
				printCosts(node, request1, request2);
			}
			else
				printCosts(node, request1);

			return account;
		}

		private void askForConfirmation(BigInteger gas) {
			if (!nonInteractive)
				yesNo("Do you really want to spend up to " + gas + " gas units to create a new account [Y/N] ");
		}
	}
}