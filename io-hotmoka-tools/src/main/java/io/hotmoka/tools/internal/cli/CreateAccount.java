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
import io.hotmoka.remote.RemoteNodeConfig;
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
	public void run() {
		try {
			new Run();
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
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
			checkParameters();

			RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().setURL(url).build();

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig)) {
				signature = node.getSignatureAlgorithmForRequests();
				keys = signature.getKeyPair();
				publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
				manifest = node.getManifest();
				takamakaCode = node.getTakamakaCode();
				chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
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

		private void checkParameters() {
			if (balance.signum() < 0)
				throw new IllegalArgumentException("The initial balance cannot be negative");
		
			if (balanceRed.signum() < 0)
				throw new IllegalArgumentException("The initial red balance cannot be negative");
		}

		private StorageReference createAccount() throws Exception {
			return "faucet".equals(payer) ? createAccountFromFaucet() : createAccountFromPayer();
		}

		private StorageReference createAccountFromFaucet() throws Exception {
			System.out.println("Free account creation from faucet will succeed only if the gamete of the node supports an open unsigned faucet");

			StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

			return (StorageReference) node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(Signer.with(signature, keys), gamete, nonceHelper.getNonceOf(gamete),
				chainId, _10_000, gasHelper.getSafeGasPrice(), takamakaCode,
				new NonVoidMethodSignature(ClassType.GAMETE, "faucet", ClassType.RGEOA, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING),
				gamete,
				new BigIntegerValue(balance), new BigIntegerValue(balanceRed), new StringValue(publicKey)));
		}

		private StorageReference createAccountFromPayer() throws Exception {
			askForConfirmation();

			StorageReference payer = new StorageReference(CreateAccount.this.payer);
			KeyPair keysOfPayer = readKeys(payer);
			Signer signer = Signer.with(signature, keysOfPayer);

			StorageReference account = (StorageReference) node.addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, _10_000, gasHelper.getSafeGasPrice(), takamakaCode,
				new ConstructorSignature(ClassType.RGEOA, ClassType.BIG_INTEGER, ClassType.STRING),
				new BigIntegerValue(balance), new StringValue(publicKey)));

			if (balanceRed.signum() > 0)
				// we send the red coins if required
				node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(signer, payer, nonceHelper.getNonceOf(payer), chainId, _10_000, gasHelper.getSafeGasPrice(), takamakaCode,
					CodeSignature.RECEIVE_RED_BIG_INTEGER, account, new BigIntegerValue(balanceRed)));

			return account;
		}

		private void askForConfirmation() {
			if (!nonInteractive) {
				int gas = balanceRed.signum() > 0 ? 20_000 : 10_000;
				System.out.print("Do you really want to spend up to " + gas + " gas units to create a new account [Y/N] ");
				String answer = System.console().readLine();
				if (!"Y".equals(answer))
					System.exit(0);
			}
		}
	}
}