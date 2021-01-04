package io.hotmoka.runs;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.UpdateOfBigInteger;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.Subscription;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartTendermintNode
 */
public class StartTendermintNode {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final BigInteger _1_000_000 = BigInteger.valueOf(1_000_000);

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = GREEN;

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			// update version number when needed
			TendermintInitializedNode initialized = TendermintInitializedNode.of(blockchain, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED);
			NodeWithAccounts accounts = NodeWithAccounts.of(blockchain, initialized.gamete(), initialized.keysOfGamete().getPrivate(), _1_000_000);
			TransactionReference takamakaCode = blockchain.getTakamakaCode();
			StorageReference manifest = blockchain.getManifest();

			System.out.println("Info about the network:");
			System.out.println("  takamakaCode: " + takamakaCode);
			System.out.println("  manifest: " + manifest);

			StorageReference gamete = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

			System.out.println("    gamete: " + gamete);

			StorageReference gasStation = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_GAS_STATION, manifest));

			System.out.println("    gasStation: " + gasStation);

			String chainId = ((StringValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;

			System.out.println("    chainId: " + chainId);

			StorageReference validators = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));

			System.out.println("    validators: " + validators);

			ClassType storageMapView = new ClassType("io.takamaka.code.util.StorageMapView");
			StorageReference shares = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getShares", storageMapView), validators));

			int numOfValidators = ((IntValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "size", BasicTypes.INT), shares))).value;

			System.out.println("    number of validators: " + numOfValidators);

			for (int num = 0; num < numOfValidators; num++) {
				StorageReference validator = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "select", ClassType.OBJECT, BasicTypes.INT), shares, new IntValue(num)));

				System.out.println("      validator #" + num + ": " + validator);

				String id = ((StringValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(manifest, _10_000, takamakaCode, CodeSignature.ID, validator))).value;

				System.out.println("        id: " + id);

				BigInteger power = ((BigIntegerValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "get", ClassType.OBJECT, ClassType.OBJECT), shares, validator))).value;

				System.out.println("        power: " + power);
			}

			try (Subscription subscrition = blockchain.subscribeToEvents(gasStation, (_gasStation, event) -> printGasPrice(blockchain, event))) {
				Signer signer = Signer.with(blockchain.getSignatureAlgorithmForRequests(), accounts.privateKey(0));
				BigInteger nonce = ZERO;

				while (true) {
					blockchain.postStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
						(signer, accounts.account(0), nonce, chainId, _10_000, BigInteger.TEN, takamakaCode,
						new NonVoidMethodSignature(ClassType.TAKAMAKA, "now", BasicTypes.LONG)));

					System.out.print("*");
					Thread.sleep(100);

					nonce = nonce.add(ONE);
				}
			}
		}
	}

	private static void printGasPrice(Node node, StorageReference event) {
		System.out.println("event: " + event);
		node.getState(event)
			.filter(update -> update instanceof UpdateOfBigInteger)
			.map(update -> (UpdateOfBigInteger) update)
			.filter(update -> "newGasPrice".equals(update.getField().name))
			.forEach(newGasPrice -> System.out.println("gas price is now: " + newGasPrice.value));
	}
}