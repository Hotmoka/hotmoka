package io.hotmoka.runs;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.PrivateKey;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartTendermintV1N1Node0
 */
public class StartTendermintV1N1Node0 extends Start {

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = GREEN;

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder()
			.setTendermintConfigurationToClone(Paths.get("io-hotmoka-runs/tendermint_configs/v1n1/node0"))
			.build();

		try (TendermintBlockchain node = TendermintBlockchain.of(config)) {
			PrivateKey privateKeyOfGamete = initialize(node);
			printManifest(node);
			runSillyTransactions(node, privateKeyOfGamete);
		}
	}

	private static PrivateKey initialize(TendermintBlockchain node) throws Exception {
		ConsensusParams consensus = new ConsensusParams.Builder().build();
		return TendermintInitializedNode.of(node, consensus, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED).keysOfGamete().getPrivate();
	}

	private static void runSillyTransactions(Node node, PrivateKey privateKeyOfGamete) throws Exception {
		NonVoidMethodSignature takamakaNow = new NonVoidMethodSignature(ClassType.TAKAMAKA, "now", BasicTypes.LONG);

		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();

		StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		BigInteger nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.NONCE, gamete))).value;

		String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;

		Signer signer = Signer.with(node.getSignatureAlgorithmForRequests(), privateKeyOfGamete);

		System.out.println("\nCalling Takamaka.now() repeatedly:\n");

		while (true) {
			BigInteger gasPrice = getGasPrice(node);
			long now = ((LongValue) node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
				(signer, gamete, nonce, chainId, _10_000, gasPrice, takamakaCode, takamakaNow))).value;

			System.out.println("gas = " + gasPrice);
			System.out.println("nonce = " + nonce);
			System.out.println("now = " + now + "\n");

			nonce = nonce.add(ONE);
		}
	}

	private static BigInteger getGasPrice(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();

		StorageReference gasStation = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.GET_GAS_STATION, manifest));

		BigInteger minimalGasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation))).value;

		// we double the minimal price, to be sure that the transaction won't be rejected
		return TWO.multiply(minimalGasPrice);
	}
}