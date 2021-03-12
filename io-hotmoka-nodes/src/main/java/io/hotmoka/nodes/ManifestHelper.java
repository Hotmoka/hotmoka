package io.hotmoka.nodes;

import java.math.BigInteger;
import java.util.NoSuchElementException;

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

/**
 * An object that helps with the access to the manifest of a node.
 */
public class ManifestHelper {
	private final Node node;
	private final static BigInteger _10_000 = BigInteger.valueOf(10_000);
	public final StorageReference gasStation;
	public final TransactionReference takamakaCode;
	public final StorageReference manifest;
	public final StorageReference versions;
	public final StorageReference validators;
	public final StorageReference gamete;

	/**
	 * Creates an object that helps with the access to the manifest of a node.
	 * 
	 * @param node the node whose manifest is considered
	 */
	public ManifestHelper(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.takamakaCode = node.getTakamakaCode();
		this.manifest = node.getManifest();
		this.validators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));
		this.gasStation = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_GAS_STATION, manifest));
		this.versions = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_VERSIONS, manifest));
		this.gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));
	}

	public String getChainId() throws NoSuchElementException, TransactionRejectedException, TransactionException, CodeExecutionException {
		return ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		try {
			builder.append("Info about the node:\n");
			builder.append("├─ takamakaCode: " + takamakaCode + "\n");
			builder.append("└─ manifest: " + manifest + "\n");

			String chainId = getChainId();

			builder.append("   ├─ chainId: " + chainId + "\n");

			int maxErrorLength = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_ERROR_LENGTH, manifest))).value;

			builder.append("   ├─ maxErrorLength: " + maxErrorLength + "\n");

			int maxDependencies = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_DEPENDENCIES, manifest))).value;

			builder.append("   ├─ maxDependencies: " + maxDependencies + "\n");

			long maxCumulativeSizeOfDependencies = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))).value;

			builder.append("   ├─ maxCumulativeSizeOfDependencies: " + maxCumulativeSizeOfDependencies + "\n");

			boolean allowsSelfCharged = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.ALLOWS_SELF_CHARGED, manifest))).value;

			builder.append("   ├─ allowsSelfCharged: " + allowsSelfCharged + "\n");

			boolean allowsUnsignedFaucet = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.ALLOWS_UNSIGNED_FAUCET, manifest))).value;

			builder.append("   ├─ allowsUnsignedFaucet: " + allowsUnsignedFaucet + "\n");

			boolean skipsVerification = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.SKIPS_VERIFICATION, manifest))).value;

			builder.append("   ├─ skipsVerification: " + skipsVerification + "\n");

			String signature = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_SIGNATURE, manifest))).value;

			builder.append("   ├─ signature: " + signature + "\n");

			builder.append("   ├─ gamete: " + gamete + "\n");

			BigInteger balanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.BALANCE, gamete))).value;

			builder.append("   │  ├─ balance: " + balanceOfGamete + "\n");

			BigInteger redBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.BALANCE_RED, gamete))).value;

			builder.append("   │  ├─ redBalance: " + redBalanceOfGamete + "\n");

			BigInteger maxFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_FAUCET, gamete))).value;

			builder.append("   │  ├─ maxFaucet: " + maxFaucet + "\n");

			BigInteger maxRedFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_RED_FAUCET, gamete))).value;

			builder.append("   │  └─ maxRedFaucet: " + maxRedFaucet + "\n");

			builder.append("   ├─ gasStation: " + gasStation + "\n");

			BigInteger gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation))).value;

			builder.append("   │  ├─ gasPrice: " + gasPrice + "\n");

			BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_GAS_PER_TRANSACTION, gasStation))).value;

			builder.append("   │  ├─ maxGasPerTransaction: " + maxGasPerTransaction + "\n");

			boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.IGNORES_GAS_PRICE, gasStation))).value;

			builder.append("   │  ├─ ignoresGasPrice: " + ignoresGasPrice + "\n");

			BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_TARGET_GAS_AT_REWARD, gasStation))).value;

			builder.append("   │  ├─ targetGasAtReward: " + targetGasAtReward + "\n");

			long inflation = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_INFLATION, gasStation))).value;

			builder.append(String.format("   │  ├─ inflation: %d (ie. %.2f%%)\n", inflation, inflation / 100_000.0));

			long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_OBLIVION, gasStation))).value;

			builder.append(String.format("   │  └─ oblivion: %d (ie. %.2f%%)\n", oblivion, 100.0 * oblivion / 1_000_000));

			builder.append("   ├─ validators: " + validators + "\n");

			ClassType storageMapView = new ClassType("io.takamaka.code.util.StorageMapView");
			StorageReference shares = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getShares", storageMapView), validators));

			int numOfValidators = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "size", BasicTypes.INT), shares))).value;

			if (numOfValidators == 0)
				builder.append("   │  └─ number of validators: 0\n");
			else
				builder.append("   │  ├─ number of validators: " + numOfValidators + "\n");

			for (int num = 0; num < numOfValidators; num++) {
				StorageReference validator = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "select", ClassType.OBJECT, BasicTypes.INT), shares, new IntValue(num)));

				builder.append("   │  ├─ validator #" + num + ": " + validator + "\n");

				String id = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.ID, validator))).value;

				builder.append("   │  │  ├─ id: " + id + " \n");

				BigInteger power = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "get", ClassType.OBJECT, ClassType.OBJECT), shares, validator))).value;

				builder.append("   │  │  └─ power: " + power + "\n");
			}

			BigInteger ticketForNewPoll = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_TICKET_FOR_NEW_POLL, validators))).value;

			builder.append("   │  ├─ ticketForNewPoll: " + ticketForNewPoll + "\n");

			StorageReference polls = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_POLLS, validators));

			int numOfPolls = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "size", BasicTypes.INT), polls))).value;

			if (numOfPolls == 0)
				builder.append("   │  └─ number of polls: " + numOfPolls + "\n");
			else
				builder.append("   │  ├─ number of polls: " + numOfPolls + "\n");

			for (int num = 0; num < numOfPolls; num++) {
				StorageReference poll = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "select", ClassType.OBJECT, BasicTypes.INT), polls, new IntValue(num)));

				boolean isLast = num == numOfPolls - 1;

				if (isLast)
					builder.append("   │  └─ poll #" + num + ": " + poll + "\n");
				else
					builder.append("   │  ├─ poll #" + num + ": " + poll + "\n");

				String description = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.POLL, "getDescription", ClassType.STRING), poll))).value;

				if (isLast)
					builder.append("   │     └─ description: " + description + "\n");
				else
					builder.append("   │  │  └─ description: " + description + "\n");
			}

			builder.append("   └─ versions: " + versions + "\n");

			int verificationVersion = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_VERIFICATION_VERSION, versions))).value;

			builder.append("      └─ verificationVersion: " + verificationVersion + "\n");
		}
		catch (Exception e) {
			builder.append("error while accessing the manifest of the node: " + e + "\n");
		}

		return builder.toString();
	}
}