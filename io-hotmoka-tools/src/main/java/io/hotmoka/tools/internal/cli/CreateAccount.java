package io.hotmoka.tools.internal.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Base64;

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
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.remote.RemoteNode;
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

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final SignatureAlgorithm<SignedTransactionRequest> signature;
		private final KeyPair keys;
		private final String publicKey;
		private final NonceHelper nonceHelper;
		private final GasHelper gasHelper;
		private final StorageReference account;
		private final StorageReference manifest;
		private final TransactionReference takamakaCode;
		private final String chainId;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				signature = node.getSignatureAlgorithmForRequests();
				keys = signature.getKeyPair();
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

		private void dumpKeysOfAccount() throws FileNotFoundException, IOException {
			String fileName = dumpKeys(account, keys);
			System.out.println("The keys of the account have been saved into the file " + fileName);
		}

		private StorageReference createAccount() throws Exception {
			return "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation will succeed only if the gamete of the node supports an open unsigned faucet");

			StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

			InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
				(Signer.with(signature, keys), gamete, nonceHelper.getNonceOf(gamete),
				chainId, _100_000, gasHelper.getGasPrice(), takamakaCode,
				new NonVoidMethodSignature(ClassType.GAMETE, "faucet", ClassType.EOA, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING),
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
			askForConfirmation();

			StorageReference payer = new StorageReference(CreateAccount.this.payer);
			KeyPair keysOfPayer = readKeys(payer);
			Signer signer = Signer.with(signature, keysOfPayer);

			ConstructorCallTransactionRequest request1 = new ConstructorCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, _100_000, gasHelper.getGasPrice(), takamakaCode,
				new ConstructorSignature(ClassType.EOA, ClassType.BIG_INTEGER, ClassType.STRING),
				new BigIntegerValue(balance), new StringValue(publicKey));
			StorageReference account = node.addConstructorCallTransaction(request1);

			if (balanceRed.signum() > 0) {
				InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
					(signer, payer, nonceHelper.getNonceOf(payer), chainId, _100_000, gasHelper.getGasPrice(), takamakaCode,
					CodeSignature.RECEIVE_RED_BIG_INTEGER, account, new BigIntegerValue(balanceRed));
				node.addInstanceMethodCallTransaction(request2);
				printCosts(node, request1, request2);
			}
			else
				printCosts(node, request1);

			return account;
		}

		private void askForConfirmation() {
			if (!nonInteractive) {
				int gas = balanceRed.signum() > 0 ? 200_000 : 100_000;
				yesNo("Do you really want to spend up to " + gas + " gas units to create a new account [Y/N] ");
			}
		}
	}
}