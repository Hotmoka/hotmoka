package io.hotmoka.runs;

import java.math.BigInteger;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.Node;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartTendermintNode
 */
abstract class Run {

	/**
	 * Initial green stake.
	 */
	protected final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	protected final static BigInteger RED = GREEN;

	protected final static BigInteger _10_000 = BigInteger.valueOf(10_000);

	protected static void printManifest(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();

		System.out.println("Info about the node:");
		System.out.println("├─ takamakaCode: " + takamakaCode);
		System.out.println("└─ manifest: " + manifest);

		String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;

		System.out.println("   ├─ chainId: " + chainId);

		int maxErrorLength = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_ERROR_LENGTH, manifest))).value;

		System.out.println("   ├─ maxErrorLength: " + maxErrorLength);

		int maxDependencies = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_DEPENDENCIES, manifest))).value;

		System.out.println("   ├─ maxDependencies: " + maxDependencies);

		long maxCumulativeSizeOfDependencies = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))).value;

		System.out.println("   ├─ maxCumulativeSizeOfDependencies: " + maxCumulativeSizeOfDependencies);

		boolean allowsSelfCharged = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.ALLOWS_SELF_CHARGED, manifest))).value;

		System.out.println("   ├─ allowsSelfCharged: " + allowsSelfCharged);

		boolean allowsUnsignedFaucet = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.ALLOWS_UNSIGNED_FAUCET, manifest))).value;

		System.out.println("   ├─ allowsUnsignedFaucet: " + allowsUnsignedFaucet);

		boolean skipsVerification = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.SKIPS_VERIFICATION, manifest))).value;

		System.out.println("   ├─ skipsVerification: " + skipsVerification);

		String signature = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_SIGNATURE, manifest))).value;

		System.out.println("   ├─ signature: " + signature);

		StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		System.out.println("   ├─ gamete: " + gamete);

		BigInteger maxFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_FAUCET, gamete))).value;

		System.out.println("   │  ├─ maxFaucet: " + maxFaucet);

		BigInteger maxRedFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_RED_FAUCET, gamete))).value;

		System.out.println("   │  └─ maxRedFaucet: " + maxRedFaucet);

		StorageReference gasStation = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_GAS_STATION, manifest));

		System.out.println("   ├─ gasStation: " + gasStation);

		BigInteger gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation))).value;

		System.out.println("   │  ├─ gasPrice: " + gasPrice);

		BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_GAS_PER_TRANSACTION, gasStation))).value;

		System.out.println("   │  ├─ maxGasPerTransaction: " + maxGasPerTransaction);

		boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.IGNORES_GAS_PRICE, gasStation))).value;

		System.out.println("   │  ├─ ignoresGasPrice: " + ignoresGasPrice);

		BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_TARGET_GAS_AT_REWARD, gasStation))).value;

		System.out.println("   │  ├─ targetGasAtReward: " + targetGasAtReward);

		long inflation = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_INFLATION, gasStation))).value;

		System.out.printf ("   │  ├─ inflation: %d (ie. %.2f%%)\n", inflation, inflation / 100_000.0);

		long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_OBLIVION, gasStation))).value;

		System.out.printf ("   │  └─ oblivion: %d (ie. %.2f%%)\n", oblivion, 100.0 * oblivion / 1_000_000);

		StorageReference validators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));

		System.out.println("   ├─ validators: " + validators);

		ClassType storageMapView = new ClassType("io.takamaka.code.util.StorageMapView");
		StorageReference shares = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getShares", storageMapView), validators));

		int numOfValidators = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "size", BasicTypes.INT), shares))).value;

		if (numOfValidators == 0)
			System.out.println("   │  └─ number of validators: 0");
		else
			System.out.println("   │  ├─ number of validators: " + numOfValidators);

		for (int num = 0; num < numOfValidators; num++) {
			StorageReference validator = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "select", ClassType.OBJECT, BasicTypes.INT), shares, new IntValue(num)));

			System.out.println("   │  ├─ validator #" + num + ": " + validator);

			String id = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.ID, validator))).value;

			System.out.println("   │  │  ├─ id: " + id);
			
			BigInteger power = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "get", ClassType.OBJECT, ClassType.OBJECT), shares, validator))).value;

			System.out.println("   │  │  └─ power: " + power);
		}

		BigInteger ticketForNewPoll = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_TICKET_FOR_NEW_POLL, validators))).value;

		System.out.println("   │  ├─ ticketForNewPoll: " + ticketForNewPoll);

		StorageReference polls = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_POLLS, validators));

		int numOfPolls = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "size", BasicTypes.INT), polls))).value;

		if (numOfPolls == 0)
			System.out.println("   │  └─ number of polls: " + numOfPolls);
		else
			System.out.println("   │  ├─ number of polls: " + numOfPolls);

		for (int num = 0; num < numOfPolls; num++) {
			StorageReference poll = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "select", ClassType.OBJECT, BasicTypes.INT), polls, new IntValue(num)));

			boolean isLast = num == numOfPolls - 1;

			if (isLast)
				System.out.println("   │  └─ poll #" + num + ": " + poll);
			else
				System.out.println("   │  ├─ poll #" + num + ": " + poll);

			String description = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.POLL, "getDescription", ClassType.STRING), poll))).value;

			if (isLast)
				System.out.println("   │     └─ description: " + description);
			else
				System.out.println("   │  │  └─ description: " + description);
		}

		StorageReference versions = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_VERSIONS, manifest));

		System.out.println("   └─ versions: " + versions);

		int verificationVersion = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_VERIFICATION_VERSION, versions))).value;

		System.out.println("      └─ verificationVersion: " + verificationVersion);
	}

	protected static void pressEnterToExit() {
		System.out.println("\nPress enter to exit this program");
		System.console().readLine();
	}
}