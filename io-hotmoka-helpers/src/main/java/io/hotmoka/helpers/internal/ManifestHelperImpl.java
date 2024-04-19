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
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.helpers.api.ManifestHelper;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

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
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 */
	public ManifestHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		this.node = node;
		this.takamakaCode = node.getTakamakaCode();
		this.manifest = node.getManifest();
		this.validators = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest));
		this.initialValidators = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_VALIDATORS, manifest));
		this.gasStation = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_STATION, manifest));
		this.versions = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERSIONS, manifest));
		this.accountsLedger = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest));
		this.gamete = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest));
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
	public String getChainId() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(gamete, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();

		try {
			builder.append("├─ takamakaCode: ").append(takamakaCode).append("\n");
			builder.append("└─ manifest: ").append(manifest).append("\n");

			String genesisTime = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(gamete, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))).getValue();
			builder.append("   ├─ genesisTime: ").append(genesisTime).append("\n");

			String chainId = getChainId();
			builder.append("   ├─ chainId: ").append(chainId).append("\n");

			long maxErrorLength = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))).getValue();

			builder.append("   ├─ maxErrorLength: ").append(maxErrorLength).append("\n");

			long maxDependencies = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))).getValue();

			builder.append("   ├─ maxDependencies: ").append(maxDependencies).append("\n");

			long maxCumulativeSizeOfDependencies = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))).getValue();

			builder.append("   ├─ maxCumulativeSizeOfDependencies: ").append(maxCumulativeSizeOfDependencies).append("\n");

			boolean allowsUnsignedFaucet = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))).getValue();

			builder.append("   ├─ allowsUnsignedFaucet: ").append(allowsUnsignedFaucet).append("\n");

			boolean skipsVerification = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))).getValue();

			builder.append("   ├─ skipsVerification: ").append(skipsVerification).append("\n");

			String signature = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))).getValue();

			builder.append("   ├─ signature: ").append(signature).append("\n");

			builder.append("   ├─ gamete: ").append(gamete).append("\n");

			BigInteger balanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, gamete))).getValue();

			builder.append("   │  ├─ balance: ").append(balanceOfGamete).append("\n");

			BigInteger redBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE_RED, gamete))).getValue();

			builder.append("   │  ├─ redBalance: ").append(redBalanceOfGamete).append("\n");

			BigInteger maxFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_FAUCET, gamete))).getValue();

			builder.append("   │  ├─ maxFaucet: ").append(maxFaucet).append("\n");

			BigInteger maxRedFaucet = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_RED_FAUCET, gamete))).getValue();

			builder.append("   │  └─ maxRedFaucet: ").append(maxRedFaucet).append("\n");

			builder.append("   ├─ gasStation: ").append(gasStation).append("\n");

			BigInteger initialGasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))).getValue();

			builder.append("   │  ├─ initialGasPrice: ").append(initialGasPrice).append("\n");

			BigInteger gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_PRICE, gasStation))).getValue();

			builder.append("   │  ├─ gasPrice: ").append(gasPrice).append("\n");

			BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))).getValue();

			builder.append("   │  ├─ maxGasPerTransaction: ").append(maxGasPerTransaction).append("\n");

			boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))).getValue();

			builder.append("   │  ├─ ignoresGasPrice: ").append(ignoresGasPrice).append("\n");

			BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))).getValue();

			builder.append("   │  ├─ targetGasAtReward: ").append(targetGasAtReward).append("\n");

			long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))).getValue();

			builder.append(String.format("   │  └─ oblivion: %d (ie. %.2f%%)\n", oblivion, 100.0 * oblivion / 1_000_000));
			
			builder.append("   ├─ validators: ").append(validators).append("\n");

			var shares = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_VIEW, "getShares", StorageTypes.STORAGE_MAP_VIEW), validators));

			int numOfValidators = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_MAP_VIEW, "size", StorageTypes.INT), shares))).getValue();

			var offers = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY, "getOffers", StorageTypes.STORAGE_SET_VIEW), validators));

			int numOfOffers = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_SET_VIEW, "size", StorageTypes.INT), offers))).getValue();

			int buyerSurcharge = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT), validators))).getValue();

			builder.append(String.format("   │  ├─ surcharge for buying validation power: %d (ie. %.6f%%)\n", buyerSurcharge, buyerSurcharge / 1_000_000.0));

			int slashingForMisbehaving = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getSlashingForMisbehaving", StorageTypes.INT), validators))).getValue();

			builder.append(String.format("   │  ├─ slashing for misbehaving validators: %d (ie. %.6f%%)\n", slashingForMisbehaving, slashingForMisbehaving / 1_000_000.0));

			int slashingForNotBehaving = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getSlashingForNotBehaving", StorageTypes.INT), validators))).getValue();

			builder.append(String.format("   │  ├─ slashing for not behaving validators: %d (ie. %.6f%%)\n", slashingForNotBehaving, slashingForNotBehaving / 1_000_000.0));

			int percentStaked = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getPercentStaked", StorageTypes.INT), validators))).getValue();

			builder.append(String.format("   │  ├─ percent of validators' reward that gets staked: %d (ie. %.6f%%)\n", percentStaked, percentStaked / 1_000_000.0));

			builder.append("   │  ├─ number of validators: ").append(numOfValidators).append("\n");

			var validatorsArray = new StorageReference[numOfValidators];
			for (int num = 0; num < numOfValidators; num++)
				validatorsArray[num] = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_MAP_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT), shares, StorageValues.intOf(num)));

			Map<StorageReference, SortedSet<StorageReference>> offersPerValidator = new HashMap<>();
			for (int num = 0; num < numOfOffers; num++) {
				var offer = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_SET_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT), offers, StorageValues.intOf(num)));
				var seller = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "getSeller", StorageTypes.PAYABLE_CONTRACT), offer));

				// the set of offers might contain expired offers since it gets updated lazily
				boolean isOngoing = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "isOngoing", StorageTypes.BOOLEAN), offer))).getValue();

				if (isOngoing)
					offersPerValidator.computeIfAbsent(seller, _seller -> new TreeSet<>()).add(offer);
			}

			for (int num = 0; num < numOfValidators; num++) {
				StorageReference validator = validatorsArray[num];

				builder.append("   │  ├─ validator #").append(num).append(": ").append(validator).append("\n");

				String id = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ID, validator))).getValue();

				builder.append("   │  │  ├─ id: ").append(id).append("\n");

				BigInteger balanceOfValidator = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, validator))).getValue();

				builder.append("   │  │  ├─ balance: ").append(balanceOfValidator).append("\n");

				BigInteger stakedForValidator = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_STAKE, validators, validator))).getValue();

				builder.append("   │  │  ├─ staked: ").append(stakedForValidator).append("\n");

				BigInteger power = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT), shares, validator))).getValue();

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

						BigInteger powerOnSale = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
							(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "getSharesOnSale", StorageTypes.BIG_INTEGER), offer))).getValue();
						BigInteger cost = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
							(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "getCost", StorageTypes.BIG_INTEGER), offer))).getValue();
						BigInteger costWithSurchage = cost.multiply(BigInteger.valueOf(buyerSurcharge + 100_000_000)).divide(_100_000_000);
						var expiration = new Date(((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
							(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "getExpiration", StorageTypes.LONG), offer))).getValue());
						StorageValue buyer = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
							(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, "getBuyer", StorageTypes.PAYABLE_CONTRACT), offer));

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

			BigInteger initialSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))).getValue();

			builder.append("   │  ├─ initialSupply: ").append(initialSupply).append("\n");

			BigInteger currentSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CURRENT_SUPPLY, validators))).getValue();

			builder.append("   │  ├─ currentSupply: ").append(currentSupply).append("\n");

			BigInteger finalSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))).getValue();

			builder.append("   │  ├─ finalSupply: ").append(finalSupply).append("\n");

			BigInteger initialRedSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_RED_SUPPLY, validators))).getValue();

			builder.append("   │  ├─ initialRedSupply: ").append(initialRedSupply).append("\n");

			long initialInflation = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))).getValue();

			builder.append(String.format("   │  ├─ initialInflation: %d (ie. %.6f%%)\n", initialInflation, initialInflation / 1_000_000.0));

			long currentInflation = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CURRENT_INFLATION, validators))).getValue();

			builder.append(String.format("   │  ├─ currentInflation: %d (ie. %.6f%%)\n", currentInflation, currentInflation / 1_000_000.0));

			BigInteger height = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_HEIGHT, validators))).getValue();

			builder.append("   │  ├─ height: ").append(height).append("\n");

			BigInteger numberOfTransactions = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_NUMBER_OF_TRANSACTIONS, validators))).getValue();

			builder.append("   │  ├─ numberOfTransactions: ").append(numberOfTransactions).append("\n");

			BigInteger ticketForNewPoll = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))).getValue();

			builder.append("   │  ├─ ticketForNewPoll: ").append(ticketForNewPoll).append("\n");

			StorageReference polls = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_POLLS, validators));

			int numOfPolls = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_SET_VIEW, "size", StorageTypes.INT), polls))).getValue();

			if (numOfPolls == 0)
				builder.append("   │  └─ number of polls: ").append(numOfPolls).append("\n");
			else
				builder.append("   │  ├─ number of polls: ").append(numOfPolls).append("\n");

			for (int num = 0; num < numOfPolls; num++) {
				var poll = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_SET_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT), polls, StorageValues.intOf(num)));

				boolean isLast = num == numOfPolls - 1;

				if (isLast)
					builder.append("   │  └─ poll #").append(num).append(": ").append(poll).append("\n");
				else
					builder.append("   │  ├─ poll #").append(num).append(": ").append(poll).append("\n");

				String description = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.POLL, "getDescription", StorageTypes.STRING), poll))).getValue();

				if (isLast)
					builder.append("   │     └─ description: ").append(description).append("\n");
				else
					builder.append("   │  │  └─ description: ").append(description).append("\n");
			}

			builder.append("   ├─ initial validators: ").append(initialValidators).append("\n");

			shares = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.SHARED_ENTITY_VIEW, "getShares", StorageTypes.STORAGE_MAP_VIEW), initialValidators));

			int numOfInitialValidators = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_MAP_VIEW, "size", StorageTypes.INT), shares))).getValue();

			if (numOfInitialValidators == 0)
				builder.append("   │  └─ number of initial validators: 0\n");
			else
				builder.append("   │  ├─ number of initial validators: ").append(numOfInitialValidators).append("\n");

			for (int num = 0; num < numOfInitialValidators; num++) {
				var validator = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_MAP_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT), shares, StorageValues.intOf(num)));

				boolean isLast = num == numOfInitialValidators - 1;

				if (isLast)
					builder.append("   │  └─ initial validator #").append(num).append(": ").append(validator).append("\n");
				else
					builder.append("   │  ├─ initial validator #").append(num).append(": ").append(validator).append("\n");

				String id = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ID, validator))).getValue();

				if (isLast)
					builder.append("   │     ├─ id: ").append(id).append("\n");
				else
					builder.append("   │  │  ├─ id: ").append(id).append("\n");

				BigInteger balanceOfValidator = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, validator))).getValue();

				if (isLast)
					builder.append("   │     ├─ balance: ").append(balanceOfValidator).append("\n");
				else
					builder.append("   │  │  ├─ balance: ").append(balanceOfValidator).append("\n");

				BigInteger power = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT), shares, validator))).getValue();

				if (isLast)
					builder.append("   │     └─ power: ").append(power).append("\n");
				else
					builder.append("   │  │  └─ power: ").append(power).append("\n");
			}

			builder.append("   ├─ accountsLedger: ").append(accountsLedger).append("\n");

			builder.append("   └─ versions: ").append(versions).append("\n");

			long verificationVersion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))).getValue();

			builder.append("      └─ verificationVersion: ").append(verificationVersion).append("\n");
		}
		catch (Exception e) {
			builder.append("error while accessing the manifest of the node: ").append(e).append("\n");
		}

		return builder.toString();
	}
}