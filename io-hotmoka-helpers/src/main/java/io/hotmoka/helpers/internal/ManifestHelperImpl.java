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

package io.hotmoka.helpers.internal;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.helpers.api.ManifestHelper;
import io.hotmoka.node.api.Node;

/**
 * Implementation of an object that helps with the access to the manifest of a node.
 */
public class ManifestHelperImpl implements ManifestHelper {
	private final Node node;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);
	private final static BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);
	private final StorageReference gasStation;
	private final TransactionReference takamakaCode;
	private final StorageReference manifest;
	private final StorageReference versions;
	private final StorageReference validators;
	private final StorageReference initialValidators;
	private final StorageReference accountsLedger;
	private final StorageReference gamete;

	/**
	 * Creates an object that helps with the access to the manifest of a node.
	 * 
	 * @param node the node whose manifest is considered
	 */
	public ManifestHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.takamakaCode = node.getTakamakaCode();
		this.manifest = node.getManifest();
		this.validators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));
		this.initialValidators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_INITIAL_VALIDATORS, manifest));
		this.gasStation = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAS_STATION, manifest));
		this.versions = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_VERSIONS, manifest));
		this.accountsLedger = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_ACCOUNTS_LEDGER, manifest));
		this.gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));
	}

	@Override
	public TransactionReference getTakamakaCode() {
		return takamakaCode;
	}

	@Override
	public StorageReference getAccountsLedger() {
		return accountsLedger;
	}

	@Override
	public StorageReference getInitialValidators() {
		return initialValidators;
	}

	@Override
	public StorageReference getValidators() {
		return validators;
	}

	@Override
	public StorageReference getManifest() {
		return manifest;
	}

	@Override
	public StorageReference getVersions() {
		return versions;
	}

	@Override
	public StorageReference getGamete() {
		return gamete;
	}

	@Override
	public StorageReference getGasStation() {
		return gasStation;
	}

	@Override
	public String getChainId() throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();

		try {
			builder.append("├─ takamakaCode: ").append(takamakaCode).append("\n");
			builder.append("└─ manifest: ").append(manifest).append("\n");

			String genesisTime = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(gamete, _100_000, takamakaCode, CodeSignature.GET_GENESIS_TIME, manifest))).value;
			builder.append("   ├─ genesisTime: ").append(genesisTime).append("\n");

			String chainId = getChainId();
			builder.append("   ├─ chainId: ").append(chainId).append("\n");

			long maxErrorLength = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_MAX_ERROR_LENGTH, manifest))).value;

			builder.append("   ├─ maxErrorLength: ").append(maxErrorLength).append("\n");

			long maxDependencies = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_MAX_DEPENDENCIES, manifest))).value;

			builder.append("   ├─ maxDependencies: ").append(maxDependencies).append("\n");

			long maxCumulativeSizeOfDependencies = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))).value;

			builder.append("   ├─ maxCumulativeSizeOfDependencies: ").append(maxCumulativeSizeOfDependencies).append("\n");

			boolean allowsSelfCharged = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.ALLOWS_SELF_CHARGED, manifest))).value;

			builder.append("   ├─ allowsSelfCharged: ").append(allowsSelfCharged).append("\n");

			boolean allowsUnsignedFaucet = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.ALLOWS_UNSIGNED_FAUCET, manifest))).value;

			builder.append("   ├─ allowsUnsignedFaucet: ").append(allowsUnsignedFaucet).append("\n");

			boolean skipsVerification = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.SKIPS_VERIFICATION, manifest))).value;

			builder.append("   ├─ skipsVerification: ").append(skipsVerification).append("\n");

			String signature = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_SIGNATURE, manifest))).value;

			builder.append("   ├─ signature: ").append(signature).append("\n");

			builder.append("   ├─ gamete: ").append(gamete).append("\n");

			BigInteger balanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.BALANCE, gamete))).value;

			builder.append("   │  ├─ balance: ").append(balanceOfGamete).append("\n");

			BigInteger redBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.BALANCE_RED, gamete))).value;

			builder.append("   │  ├─ redBalance: ").append(redBalanceOfGamete).append("\n");

			BigInteger maxFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_MAX_FAUCET, gamete))).value;

			builder.append("   │  ├─ maxFaucet: ").append(maxFaucet).append("\n");

			BigInteger maxRedFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_MAX_RED_FAUCET, gamete))).value;

			builder.append("   │  └─ maxRedFaucet: ").append(maxRedFaucet).append("\n");

			builder.append("   ├─ gasStation: ").append(gasStation).append("\n");

			BigInteger initialGasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_INITIAL_GAS_PRICE, gasStation))).value;

			builder.append("   │  ├─ initialGasPrice: ").append(initialGasPrice).append("\n");

			BigInteger gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation))).value;

			builder.append("   │  ├─ gasPrice: ").append(gasPrice).append("\n");

			BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_MAX_GAS_PER_TRANSACTION, gasStation))).value;

			builder.append("   │  ├─ maxGasPerTransaction: ").append(maxGasPerTransaction).append("\n");

			boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.IGNORES_GAS_PRICE, gasStation))).value;

			builder.append("   │  ├─ ignoresGasPrice: ").append(ignoresGasPrice).append("\n");

			BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_TARGET_GAS_AT_REWARD, gasStation))).value;

			builder.append("   │  ├─ targetGasAtReward: ").append(targetGasAtReward).append("\n");

			long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_OBLIVION, gasStation))).value;

			builder.append(String.format("   │  └─ oblivion: %d (ie. %.2f%%)\n", oblivion, 100.0 * oblivion / 1_000_000));
			
			builder.append("   ├─ validators: ").append(validators).append("\n");

			var shares = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_VIEW, "getShares", ClassType.STORAGE_MAP_VIEW), validators));

			int numOfValidators = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_MAP_VIEW, "size", BasicTypes.INT), shares))).value;

			var offers = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY, "getOffers", ClassType.STORAGE_SET_VIEW), validators));

			int numOfOffers = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "size", BasicTypes.INT), offers))).value;

			int buyerSurcharge = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getBuyerSurcharge", BasicTypes.INT), validators))).value;

			builder.append(String.format("   │  ├─ surcharge for buying validation power: %d (ie. %.6f%%)\n", buyerSurcharge, buyerSurcharge / 1_000_000.0));

			int slashingForMisbehaving = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getSlashingForMisbehaving", BasicTypes.INT), validators))).value;

			builder.append(String.format("   │  ├─ slashing for misbehaving validators: %d (ie. %.6f%%)\n", slashingForMisbehaving, slashingForMisbehaving / 1_000_000.0));

			int slashingForNotBehaving = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getSlashingForNotBehaving", BasicTypes.INT), validators))).value;

			builder.append(String.format("   │  ├─ slashing for not behaving validators: %d (ie. %.6f%%)\n", slashingForNotBehaving, slashingForNotBehaving / 1_000_000.0));

			int percentStaked = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getPercentStaked", BasicTypes.INT), validators))).value;

			builder.append(String.format("   │  ├─ percent of validators' reward that gets staked: %d (ie. %.6f%%)\n", percentStaked, percentStaked / 1_000_000.0));

			builder.append("   │  ├─ number of validators: ").append(numOfValidators).append("\n");

			var validatorsArray = new StorageReference[numOfValidators];
			for (int num = 0; num < numOfValidators; num++)
				validatorsArray[num] = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_MAP_VIEW, "select", ClassType.OBJECT, BasicTypes.INT), shares, new IntValue(num)));

			Map<StorageReference, SortedSet<StorageReference>> offersPerValidator = new HashMap<>();
			for (int num = 0; num < numOfOffers; num++) {
				var offer = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "select", ClassType.OBJECT, BasicTypes.INT), offers, new IntValue(num)));
				var seller = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_OFFER, "getSeller", ClassType.PAYABLE_CONTRACT), offer));

				// the set of offers might contain expired offers since it gets updated lazily
				boolean isOngoing = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_OFFER, "isOngoing", BasicTypes.BOOLEAN), offer))).value;

				if (isOngoing)
					offersPerValidator.computeIfAbsent(seller, _seller -> new TreeSet<>()).add(offer);
			}

			for (int num = 0; num < numOfValidators; num++) {
				StorageReference validator = validatorsArray[num];

				builder.append("   │  ├─ validator #").append(num).append(": ").append(validator).append("\n");

				String id = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.ID, validator))).value;

				builder.append("   │  │  ├─ id: ").append(id).append("\n");

				BigInteger balanceOfValidator = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.BALANCE, validator))).value;

				builder.append("   │  │  ├─ balance: ").append(balanceOfValidator).append("\n");

				BigInteger stakedForValidator = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_STAKE, validators, validator))).value;

				builder.append("   │  │  ├─ staked: ").append(stakedForValidator).append("\n");

				BigInteger power = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_MAP_VIEW, "get", ClassType.OBJECT, ClassType.OBJECT), shares, validator))).value;

				SortedSet<StorageReference> saleOffers = offersPerValidator.get(validator);
				if (saleOffers == null)
					builder.append("   │  │  └─ power: ").append(power).append("\n");
				else {
					builder.append("   │  │  ├─ power: ").append(power).append("\n");
					int counter = 0, last = saleOffers.size() - 1;
					for (StorageReference offer: saleOffers) {
						boolean isLast = counter == last;

						if (isLast)
							builder.append("   │  │  └─ sale offer #" + counter + ": ").append(offer).append("\n");
						else
							builder.append("   │  │  ├─ sale offer #" + counter + ": ").append(offer).append("\n");

						BigInteger powerOnSale = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
							(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_OFFER, "getSharesOnSale", ClassType.BIG_INTEGER), offer))).value;
						BigInteger cost = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
							(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_OFFER, "getCost", ClassType.BIG_INTEGER), offer))).value;
						BigInteger costWithSurchage = cost.multiply(BigInteger.valueOf(buyerSurcharge + 100_000_000)).divide(_100_000_000);
						var expiration = new Date(((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
							(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_OFFER, "getExpiration", BasicTypes.LONG), offer))).value);
						StorageValue buyer = node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
							(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_OFFER, "getBuyer", ClassType.PAYABLE_CONTRACT), offer));

						if (isLast) {
							builder.append("   │  │     ├─ power on sale: ").append(powerOnSale).append("\n");
							if (buyer instanceof StorageReference) // it might be a NullValue instead
								builder.append("   │  │     ├─ reserved buyer: ").append(buyer).append("\n");
							builder.append("   │  │     ├─ cost: ").append(cost).append("\n");
							builder.append("   │  │     ├─ cost with surcharge: ").append(costWithSurchage).append("\n");
							builder.append("   │  │     └─ expiration: ").append(expiration).append("\n");
						}
						else {
							builder.append("   │  │  │  ├─ power on sale: ").append(powerOnSale).append("\n");
							if (buyer instanceof StorageReference)
								builder.append("   │  │  │  ├─ reserved buyer: ").append(buyer).append("\n");
							builder.append("   │  │  │  ├─ cost: ").append(cost).append("\n");
							builder.append("   │  │  │  ├─ cost with surcharge: ").append(costWithSurchage).append("\n");
							builder.append("   │  │  │  └─ expiration: ").append(expiration).append("\n");
						}

						counter++;
					}
				}
			}

			BigInteger initialSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_INITIAL_SUPPLY, validators))).value;

			builder.append("   │  ├─ initialSupply: ").append(initialSupply).append("\n");

			BigInteger currentSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_CURRENT_SUPPLY, validators))).value;

			builder.append("   │  ├─ currentSupply: ").append(currentSupply).append("\n");

			BigInteger finalSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_FINAL_SUPPLY, validators))).value;

			builder.append("   │  ├─ finalSupply: ").append(finalSupply).append("\n");

			BigInteger initialRedSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_INITIAL_RED_SUPPLY, validators))).value;

			builder.append("   │  ├─ initialRedSupply: ").append(initialRedSupply).append("\n");

			long initialInflation = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_INITIAL_INFLATION, validators))).value;

			builder.append(String.format("   │  ├─ initialInflation: %d (ie. %.6f%%)\n", initialInflation, initialInflation / 1_000_000.0));

			long currentInflation = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_CURRENT_INFLATION, validators))).value;

			builder.append(String.format("   │  ├─ currentInflation: %d (ie. %.6f%%)\n", currentInflation, currentInflation / 1_000_000.0));

			BigInteger height = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_HEIGHT, validators))).value;

			builder.append("   │  ├─ height: ").append(height).append("\n");

			BigInteger numberOfTransactions = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_NUMBER_OF_TRANSACTIONS, validators))).value;

			builder.append("   │  ├─ numberOfTransactions: ").append(numberOfTransactions).append("\n");

			BigInteger ticketForNewPoll = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_TICKET_FOR_NEW_POLL, validators))).value;

			builder.append("   │  ├─ ticketForNewPoll: ").append(ticketForNewPoll).append("\n");

			StorageReference polls = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_POLLS, validators));

			int numOfPolls = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "size", BasicTypes.INT), polls))).value;

			if (numOfPolls == 0)
				builder.append("   │  └─ number of polls: ").append(numOfPolls).append("\n");
			else
				builder.append("   │  ├─ number of polls: ").append(numOfPolls).append("\n");

			for (int num = 0; num < numOfPolls; num++) {
				var poll = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_SET_VIEW, "select", ClassType.OBJECT, BasicTypes.INT), polls, new IntValue(num)));

				boolean isLast = num == numOfPolls - 1;

				if (isLast)
					builder.append("   │  └─ poll #").append(num).append(": ").append(poll).append("\n");
				else
					builder.append("   │  ├─ poll #").append(num).append(": ").append(poll).append("\n");

				String description = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.POLL, "getDescription", ClassType.STRING), poll))).value;

				if (isLast)
					builder.append("   │     └─ description: ").append(description).append("\n");
				else
					builder.append("   │  │  └─ description: ").append(description).append("\n");
			}

			builder.append("   ├─ initial validators: ").append(initialValidators).append("\n");

			shares = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.SHARED_ENTITY_VIEW, "getShares", ClassType.STORAGE_MAP_VIEW), initialValidators));

			int numOfInitialValidators = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_MAP_VIEW, "size", BasicTypes.INT), shares))).value;

			if (numOfInitialValidators == 0)
				builder.append("   │  └─ number of initial validators: 0\n");
			else
				builder.append("   │  ├─ number of initial validators: ").append(numOfInitialValidators).append("\n");

			for (int num = 0; num < numOfInitialValidators; num++) {
				var validator = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_MAP_VIEW, "select", ClassType.OBJECT, BasicTypes.INT), shares, new IntValue(num)));

				boolean isLast = num == numOfInitialValidators - 1;

				if (isLast)
					builder.append("   │  └─ initial validator #").append(num).append(": ").append(validator).append("\n");
				else
					builder.append("   │  ├─ initial validator #").append(num).append(": ").append(validator).append("\n");

				String id = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.ID, validator))).value;

				if (isLast)
					builder.append("   │     ├─ id: ").append(id).append("\n");
				else
					builder.append("   │  │  ├─ id: ").append(id).append("\n");

				BigInteger balanceOfValidator = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.BALANCE, validator))).value;

				if (isLast)
					builder.append("   │     ├─ balance: ").append(balanceOfValidator).append("\n");
				else
					builder.append("   │  │  ├─ balance: ").append(balanceOfValidator).append("\n");

				BigInteger power = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, new NonVoidMethodSignature(ClassType.STORAGE_MAP_VIEW, "get", ClassType.OBJECT, ClassType.OBJECT), shares, validator))).value;

				if (isLast)
					builder.append("   │     └─ power: ").append(power).append("\n");
				else
					builder.append("   │  │  └─ power: ").append(power).append("\n");
			}

			builder.append("   ├─ accountsLedger: ").append(accountsLedger).append("\n");

			builder.append("   └─ versions: ").append(versions).append("\n");

			long verificationVersion = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_VERIFICATION_VERSION, versions))).value;

			builder.append("      └─ verificationVersion: ").append(verificationVersion).append("\n");
		}
		catch (Exception e) {
			builder.append("error while accessing the manifest of the node: ").append(e).append("\n");
		}

		return builder.toString();
	}
}