package io.hotmoka.runs;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.views.InitializedNode;
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

	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = BigInteger.valueOf(999_999_999).pow(5);

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			// update version number when needed
			InitializedNode initializedView = TendermintInitializedNode.of
				(blockchain, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED);
			StorageReference gamete = initializedView.gamete();
			TransactionReference takamakaCode = blockchain.getTakamakaCode();
			NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, gamete, initializedView.keysOfGamete().getPrivate(), _200_000);
			StorageReference manifest = blockchain.getManifest();

			// we reload the gamete from the manifest, for verification
			gamete = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

			StorageReference account0 = viewWithAccounts.account(0);

			System.out.println("Info about the network:");
			System.out.println("  takamakaCode: " + takamakaCode);
			System.out.println("  gamete: " + gamete);
			System.out.println("  account #0: " + account0);
			System.out.println("  manifest: " + manifest);

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
		}
	}
}