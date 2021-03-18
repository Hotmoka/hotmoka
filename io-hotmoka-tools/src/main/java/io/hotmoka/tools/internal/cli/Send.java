package io.hotmoka.tools.internal.cli;

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.GAMETE;
import static io.hotmoka.beans.types.ClassType.PAYABLE_CONTRACT;

import java.math.BigInteger;
import java.security.KeyPair;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
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

@Command(name = "send",
	description = "Sends units of coin to a payable contract",
	showDefaultValues = true)
public class Send extends AbstractCommand {

	@Parameters(index = "0", description = "the amount of coins sent to the contract")
    private BigInteger amount;

	@Parameters(index = "1", description = "the reference to the payable contract that receives the coins")
    private String contract;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the creation, or the string \"faucet\"", defaultValue = "faucet")
    private String payer;

	@Option(names = { "--amount-red" }, description = "the amount of red coins sent to the contract", defaultValue = "0")
    private BigInteger amountRed;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final SignatureAlgorithm<SignedTransactionRequest> signature;
		private final StorageReference contract;
		private final TransactionReference takamakaCode;
		private final GasHelper gasHelper;
		private final NonceHelper nonceHelper;
		private final String chainId;
		private final StorageReference gamete;

		private Run() throws Exception {
			contract = new StorageReference(Send.this.contract);

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				signature = node.getSignatureAlgorithmForRequests();
				takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));
				chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(gamete, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				gasHelper = new GasHelper(node);
				nonceHelper = new NonceHelper(node);
				sendCoins();
			}
		}

		private void sendCoins() throws Exception {
			if ("faucet".equals(payer))
				sendCoinsFromFaucet();
			else
				sendCoinsFromPayer();
		}

		private void sendCoinsFromPayer() throws Exception {
			askForConfirmation();

			StorageReference payer = new StorageReference(Send.this.payer);
			KeyPair keysOfPayer = readKeys(payer);
			Signer signer = Signer.with(signature, keysOfPayer);

			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signer,
				payer, nonceHelper.getNonceOf(payer),
				chainId, _10_000, gasHelper.getSafeGasPrice(), takamakaCode,
				new VoidMethodSignature(PAYABLE_CONTRACT, "receive", ClassType.BIG_INTEGER),
				contract,
				new BigIntegerValue(amount)));

			if (amountRed.signum() > 0)
				node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(signer,
					payer, nonceHelper.getNonceOf(payer),
					chainId, _10_000, gasHelper.getSafeGasPrice(), takamakaCode,
					CodeSignature.RECEIVE_RED_BIG_INTEGER,
					contract,
					new BigIntegerValue(amountRed)));
		}

		private void sendCoinsFromFaucet() throws Exception {
			// any keys work for the faucet, if it is unsigned
			KeyPair keys = signature.getKeyPair();

			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(Signer.with(signature, keys),
				gamete, nonceHelper.getNonceOf(gamete),
				chainId, _10_000, gasHelper.getSafeGasPrice(), takamakaCode,
				new VoidMethodSignature(GAMETE, "faucet", PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER),
				gamete,
				contract, new BigIntegerValue(amount), new BigIntegerValue(amountRed)));
		}

		private void askForConfirmation() {
			if (!nonInteractive) {
				int gas = amountRed.signum() > 0 ? 20_000 : 10_000;
				System.out.print("Do you really want to spend up to " + gas + " gas units to send the coins [Y/N] ");
				String answer = System.console().readLine();
				if (!"Y".equals(answer))
					throw new CommandException("stopped");
			}
		}
	}
}