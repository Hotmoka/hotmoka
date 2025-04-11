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

import io.hotmoka.helpers.api.ManifestHelper;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * Implementation of an object that helps with the access to the manifest of a node.
 */
public class ManifestHelperImpl implements ManifestHelper {
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);
	private final static BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);
	private final Node node;
	private final TransactionReference takamakaCode;
	private final StorageReference manifest;
	private final StorageReference gasStation;
	private final StorageReference versions;
	private final StorageReference validators;
	private final StorageReference initialValidators;
	private final StorageReference accountsLedger;
	private final StorageReference gamete;
	private final String toString;

	/**
	 * Creates an object that helps with the access to the manifest of a node.
	 * 
	 * @param node the node whose manifest is considered
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 */
	public ManifestHelperImpl(Node node) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.takamakaCode = node.getTakamakaCode();
		this.manifest = node.getManifest();
		this.validators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_VALIDATORS + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_VALIDATORS, NodeException::new);
		this.initialValidators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_VALIDATORS, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_VALIDATORS + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_INITIAL_VALIDATORS, NodeException::new);
		this.gasStation = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_STATION, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAS_STATION + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_GAS_STATION, NodeException::new);
		this.versions = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERSIONS, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_VERSIONS + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_VERSIONS, NodeException::new);
		this.accountsLedger = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_ACCOUNTS_LEDGER + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, NodeException::new);
		this.gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAMETE + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_GAMETE, NodeException::new);
		this.toString = asString();
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
	public String getChainId() throws NodeException, TimeoutException, InterruptedException {
		// this cannot be precomputed as, for instance, the manifest, since it might change
		return node.getConfig().getChainId();
	}

	@Override
	public String toString() {
		return toString;
	}

	private String asString() throws NodeException, TransactionRejectedException, TransactionException, CodeExecutionException, TimeoutException, InterruptedException {
		var builder = new StringBuilder();

		builder.append("├─ takamakaCode: ").append(takamakaCode).append("\n");
		builder.append("└─ manifest: ").append(manifest).append("\n");

		String genesisTime = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(gamete, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GENESIS_TIME + " should not return void"))
				.asReturnedString(MethodSignatures.GET_GENESIS_TIME, NodeException::new);
		builder.append("   ├─ genesisTime: ").append(genesisTime).append("\n");

		builder.append("   ├─ chainId: ").append(getChainId()).append("\n");

		int maxErrorLength = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_ERROR_LENGTH + " should not return void"))
				.asReturnedInt(MethodSignatures.GET_MAX_ERROR_LENGTH, NodeException::new);

		builder.append("   ├─ maxErrorLength: ").append(maxErrorLength).append("\n");

		int maxDependencies = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_DEPENDENCIES + " should not return void"))
				.asReturnedInt(MethodSignatures.GET_MAX_DEPENDENCIES, NodeException::new);

		builder.append("   ├─ maxDependencies: ").append(maxDependencies).append("\n");

		long maxCumulativeSizeOfDependencies = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES + " should not return void"))
				.asReturnedLong(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, NodeException::new);

		builder.append("   ├─ maxCumulativeSizeOfDependencies: ").append(maxCumulativeSizeOfDependencies).append("\n");

		boolean allowsUnsignedFaucet = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET + " should not return void"))
				.asReturnedBoolean(MethodSignatures.ALLOWS_UNSIGNED_FAUCET, NodeException::new);

		builder.append("   ├─ allowsUnsignedFaucet: ").append(allowsUnsignedFaucet).append("\n");

		boolean skipsVerification = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.SKIPS_VERIFICATION + " should not return void"))
				.asReturnedBoolean(MethodSignatures.SKIPS_VERIFICATION, NodeException::new);

		builder.append("   ├─ skipsVerification: ").append(skipsVerification).append("\n");

		String signature = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_SIGNATURE + " should not return void"))
				.asReturnedString(MethodSignatures.GET_SIGNATURE, NodeException::new);

		builder.append("   ├─ signature: ").append(signature).append("\n");

		builder.append("   ├─ gamete: ").append(gamete).append("\n");

		BigInteger balanceOfGamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, gamete))
				.orElseThrow(() -> new NodeException(MethodSignatures.BALANCE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.BALANCE, NodeException::new);

		builder.append("   │  ├─ balance: ").append(balanceOfGamete).append("\n");

		BigInteger maxFaucet = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_FAUCET, gamete))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_FAUCET + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_MAX_FAUCET, NodeException::new);

		builder.append("   │  └─ maxFaucet: ").append(maxFaucet).append("\n");

		builder.append("   ├─ gasStation: ").append(gasStation).append("\n");

		BigInteger initialGasPrice = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_GAS_PRICE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_INITIAL_GAS_PRICE, NodeException::new);

		builder.append("   │  ├─ initialGasPrice: ").append(initialGasPrice).append("\n");

		BigInteger gasPrice = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_PRICE, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAS_PRICE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_GAS_PRICE, NodeException::new);

		builder.append("   │  ├─ gasPrice: ").append(gasPrice).append("\n");

		BigInteger maxGasPerTransaction = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, NodeException::new);

		builder.append("   │  ├─ maxGasPerTransaction: ").append(maxGasPerTransaction).append("\n");

		boolean ignoresGasPrice = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.IGNORES_GAS_PRICE + " should not return void"))
				.asReturnedBoolean(MethodSignatures.IGNORES_GAS_PRICE, NodeException::new);

		builder.append("   │  ├─ ignoresGasPrice: ").append(ignoresGasPrice).append("\n");

		BigInteger targetGasAtReward = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_TARGET_GAS_AT_REWARD + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_TARGET_GAS_AT_REWARD, NodeException::new);

		builder.append("   │  ├─ targetGasAtReward: ").append(targetGasAtReward).append("\n");

		long oblivion = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_OBLIVION + " should not return void"))
				.asReturnedLong(MethodSignatures.GET_OBLIVION, NodeException::new);

		builder.append(String.format("   │  └─ oblivion: %d (ie. %.2f%%)\n", oblivion, 100.0 * oblivion / 1_000_000));

		builder.append("   ├─ validators: ").append(validators).append("\n");

		StorageReference shares;
		{
			var method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_VIEW, "getShares", StorageTypes.STORAGE_MAP_VIEW);
			shares = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, method, validators))
					.orElseThrow(() -> new NodeException(method + " should not return void"))
					.asReturnedReference(method, NodeException::new);
		}

		int numOfValidators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SIZE, shares))
				.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_MAP_VIEW_SIZE + " should not return void"))
				.asReturnedInt(MethodSignatures.STORAGE_MAP_VIEW_SIZE, NodeException::new);

		StorageReference offers;
		{
			var method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY, "getOffers", StorageTypes.STORAGE_SET_VIEW);
			offers = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, method, validators))
					.orElseThrow(() -> new NodeException(method + " should not return void"))
					.asReturnedReference(method, NodeException::new);
		}

		int numOfOffers = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_SET_VIEW_SIZE, offers))
				.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_SET_VIEW_SIZE + " should not return void"))
				.asReturnedInt(MethodSignatures.STORAGE_SET_VIEW_SIZE, NodeException::new);

		int buyerSurcharge;
		{
			NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT);
			buyerSurcharge = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, method, validators))
					.orElseThrow(() -> new NodeException(method + " should not return void"))
					.asReturnedInt(method, NodeException::new);
		}

		builder.append(String.format("   │  ├─ surcharge for buying validation power: %d (ie. %.6f%%)\n", buyerSurcharge, buyerSurcharge / 1_000_000.0));

		int slashingForMisbehaving;
		{
			NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForMisbehaving", StorageTypes.INT);
			slashingForMisbehaving = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, method, validators))
					.orElseThrow(() -> new NodeException("getSlashingForMisbehaving() should not return void"))
					.asReturnedInt(method, NodeException::new);
		}

		builder.append(String.format("   │  ├─ slashing for misbehaving validators: %d (ie. %.6f%%)\n", slashingForMisbehaving, slashingForMisbehaving / 1_000_000.0));

		int slashingForNotBehaving;
		{
			NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForNotBehaving", StorageTypes.INT);
			slashingForNotBehaving = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, method, validators))
					.orElseThrow(() -> new NodeException("getSlashingForNotBehaving() should not return void"))
					.asReturnedInt(method, NodeException::new);
		}

		builder.append(String.format("   │  ├─ slashing for not behaving validators: %d (ie. %.6f%%)\n", slashingForNotBehaving, slashingForNotBehaving / 1_000_000.0));

		int percentStaked;
		{
			NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getPercentStaked", StorageTypes.INT);
			percentStaked = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, method, validators))
					.orElseThrow(() -> new NodeException("getPercentStaked() should not return void"))
					.asReturnedInt(method, NodeException::new);
		}

		builder.append(String.format("   │  ├─ percent of validators' reward that gets staked: %d (ie. %.6f%%)\n", percentStaked, percentStaked / 1_000_000.0));

		builder.append("   │  ├─ number of validators: ").append(numOfValidators).append("\n");

		var validatorsArray = new StorageReference[numOfValidators];
		for (int num = 0; num < numOfValidators; num++)
			validatorsArray[num] = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SELECT, shares, StorageValues.intOf(num)))
			.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_MAP_VIEW_SELECT + " should not return void"))
			.asReturnedReference(MethodSignatures.STORAGE_SET_VIEW_SELECT, NodeException::new);

		Map<StorageReference, SortedSet<StorageReference>> offersPerValidator = new HashMap<>();
		NonVoidMethodSignature getSeller = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getSeller", StorageTypes.PAYABLE_CONTRACT);
		for (int num = 0; num < numOfOffers; num++) {
			var offer = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_SET_VIEW_SELECT, offers, StorageValues.intOf(num)))
					.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_SET_VIEW_SELECT + " should not return void"))
					.asReturnedReference(MethodSignatures.STORAGE_SET_VIEW_SELECT, NodeException::new);
			var seller = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, getSeller, offer))
					.orElseThrow(() -> new NodeException("getSeller() should not return void"))
					.asReturnedReference(getSeller, NodeException::new);

			// the set of offers might contain expired offers since it gets updated lazily
			boolean isOngoing;
			{
				NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "isOngoing", StorageTypes.BOOLEAN);
				isOngoing = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, method, offer))
						.orElseThrow(() -> new NodeException("isOngoing() should not return void"))
						.asReturnedBoolean(method, NodeException::new);
			}

			if (isOngoing)
				offersPerValidator.computeIfAbsent(seller, _seller -> new TreeSet<>()).add(offer);
		}

		for (int num = 0; num < numOfValidators; num++) {
			StorageReference validator = validatorsArray[num];

			builder.append("   │  ├─ validator #").append(num).append(": ").append(validator).append("\n");

			String id = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ID, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.ID + " should not return void"))
					.asReturnedString(MethodSignatures.ID, NodeException::new);

			builder.append("   │  │  ├─ id: ").append(id).append("\n");

			BigInteger balanceOfValidator = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.BALANCE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.BALANCE, NodeException::new);

			builder.append("   │  │  ├─ balance: ").append(balanceOfValidator).append("\n");

			BigInteger stakedForValidator = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_STAKE, validators, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_STAKE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_STAKE, NodeException::new);

			builder.append("   │  │  ├─ staked: ").append(stakedForValidator).append("\n");

			BigInteger power = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_GET, shares, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_MAP_VIEW_GET + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.STORAGE_MAP_VIEW_GET, NodeException::new);

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

					BigInteger powerOnSale;
					{
						NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getSharesOnSale", StorageTypes.BIG_INTEGER);
						powerOnSale = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
								(manifest, _100_000, takamakaCode, method, offer))
								.orElseThrow(() -> new NodeException(method + " should not return void"))
								.asReturnedBigInteger(method, NodeException::new);
					}

					BigInteger cost;
					{
						NonVoidMethodSignature method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getCost", StorageTypes.BIG_INTEGER);
						cost = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
								(manifest, _100_000, takamakaCode, method, offer))
								.orElseThrow(() -> new NodeException("getCost() should not return void"))
								.asReturnedBigInteger(method, NodeException::new);
					}

					BigInteger costWithSurchage = cost.multiply(BigInteger.valueOf(buyerSurcharge + 100_000_000)).divide(_100_000_000);

					Date expiration;
					{
						var method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getExpiration", StorageTypes.LONG);
						expiration = new Date(node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, method, offer))
								.orElseThrow(() -> new NodeException(method + " should not return void"))
								.asReturnedLong(method, NodeException::new));
					}

					StorageValue buyer = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
							(manifest, _100_000, takamakaCode, MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_OFFER, "getBuyer", StorageTypes.PAYABLE_CONTRACT), offer))
							.orElseThrow(() -> new NodeException("getBuyer() should not return void"));

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

		BigInteger initialSupply = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_SUPPLY + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_INITIAL_SUPPLY, NodeException::new);

		builder.append("   │  ├─ initialSupply: ").append(initialSupply).append("\n");

		BigInteger currentSupply = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CURRENT_SUPPLY, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_CURRENT_SUPPLY + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_CURRENT_SUPPLY, NodeException::new);

		builder.append("   │  ├─ currentSupply: ").append(currentSupply).append("\n");

		BigInteger finalSupply = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_FINAL_SUPPLY + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_FINAL_SUPPLY, NodeException::new);

		builder.append("   │  ├─ finalSupply: ").append(finalSupply).append("\n");

		long initialInflation = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_INFLATION + " should not return void"))
				.asReturnedLong(MethodSignatures.GET_INITIAL_INFLATION, NodeException::new);

		builder.append(String.format("   │  ├─ initialInflation: %d (ie. %.6f%%)\n", initialInflation, initialInflation / 1_000_000.0));

		long currentInflation = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CURRENT_INFLATION, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_CURRENT_INFLATION + " should not return void"))
				.asReturnedLong(MethodSignatures.GET_CURRENT_INFLATION, NodeException::new);

		builder.append(String.format("   │  ├─ currentInflation: %d (ie. %.6f%%)\n", currentInflation, currentInflation / 1_000_000.0));

		BigInteger height = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_HEIGHT, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_HEIGHT + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_HEIGHT, NodeException::new);

		builder.append("   │  ├─ height: ").append(height).append("\n");

		BigInteger numberOfTransactions = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_NUMBER_OF_TRANSACTIONS, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_NUMBER_OF_TRANSACTIONS + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_NUMBER_OF_TRANSACTIONS, NodeException::new);

		builder.append("   │  ├─ numberOfTransactions: ").append(numberOfTransactions).append("\n");

		BigInteger ticketForNewPoll = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_TICKET_FOR_NEW_POLL + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.GET_TICKET_FOR_NEW_POLL, NodeException::new);

		builder.append("   │  ├─ ticketForNewPoll: ").append(ticketForNewPoll).append("\n");

		StorageReference polls = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_POLLS, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_POLLS + " should not return void"))
				.asReturnedReference(MethodSignatures.GET_POLLS, NodeException::new);

		int numOfPolls = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_SET_VIEW_SIZE, polls))
				.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_SET_VIEW_SIZE + " should not return void"))
				.asReturnedInt(MethodSignatures.STORAGE_SET_VIEW_SIZE, NodeException::new);

		if (numOfPolls == 0)
			builder.append("   │  └─ number of polls: ").append(numOfPolls).append("\n");
		else
			builder.append("   │  ├─ number of polls: ").append(numOfPolls).append("\n");

		for (int num = 0; num < numOfPolls; num++) {
			var poll = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_SET_VIEW_SELECT, polls, StorageValues.intOf(num)))
					.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_SET_VIEW_SELECT + " should not return void"))
					.asReturnedReference(MethodSignatures.STORAGE_SET_VIEW_SELECT, NodeException::new);

			boolean isLast = num == numOfPolls - 1;

			if (isLast)
				builder.append("   │  └─ poll #").append(num).append(": ").append(poll).append("\n");
			else
				builder.append("   │  ├─ poll #").append(num).append(": ").append(poll).append("\n");

			String description;
			{
				var method = MethodSignatures.ofNonVoid(StorageTypes.POLL, "getDescription", StorageTypes.STRING);
				description = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, method, poll))
						.orElseThrow(() -> new NodeException(method + " should not return void"))
						.asReturnedString(method, NodeException::new);
			}

			if (isLast)
				builder.append("   │     └─ description: ").append(description).append("\n");
			else
				builder.append("   │  │  └─ description: ").append(description).append("\n");
		}

		builder.append("   ├─ initial validators: ").append(initialValidators).append("\n");

		{
			var method = MethodSignatures.ofNonVoid(StorageTypes.SHARED_ENTITY_VIEW, "getShares", StorageTypes.STORAGE_MAP_VIEW);
			shares = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, method, initialValidators))
					.orElseThrow(() -> new NodeException(method + " should not return void"))
					.asReturnedReference(method, NodeException::new);
		}

		int numOfInitialValidators = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SIZE, shares))
				.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_MAP_VIEW_SIZE + " should not return void"))
				.asReturnedInt(MethodSignatures.STORAGE_MAP_VIEW_SIZE, NodeException::new);

		if (numOfInitialValidators == 0)
			builder.append("   │  └─ number of initial validators: 0\n");
		else
			builder.append("   │  ├─ number of initial validators: ").append(numOfInitialValidators).append("\n");

		for (int num = 0; num < numOfInitialValidators; num++) {
			var validator = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SELECT, shares, StorageValues.intOf(num)))
					.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_MAP_VIEW_SELECT + " should not return void"))
					.asReturnedReference(MethodSignatures.STORAGE_MAP_VIEW_SELECT, NodeException::new);

			boolean isLast = num == numOfInitialValidators - 1;

			if (isLast)
				builder.append("   │  └─ initial validator #").append(num).append(": ").append(validator).append("\n");
			else
				builder.append("   │  ├─ initial validator #").append(num).append(": ").append(validator).append("\n");

			String id = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ID, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.ID + " should not return void"))
					.asReturnedString(MethodSignatures.ID, NodeException::new);

			if (isLast)
				builder.append("   │     ├─ id: ").append(id).append("\n");
			else
				builder.append("   │  │  ├─ id: ").append(id).append("\n");

			BigInteger balanceOfValidator = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.BALANCE, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.BALANCE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.BALANCE, NodeException::new);

			if (isLast)
				builder.append("   │     ├─ balance: ").append(balanceOfValidator).append("\n");
			else
				builder.append("   │  │  ├─ balance: ").append(balanceOfValidator).append("\n");

			BigInteger power = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_GET, shares, validator))
					.orElseThrow(() -> new NodeException(MethodSignatures.STORAGE_MAP_VIEW_GET + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.STORAGE_MAP_VIEW_GET, NodeException::new);

			if (isLast)
				builder.append("   │     └─ power: ").append(power).append("\n");
			else
				builder.append("   │  │  └─ power: ").append(power).append("\n");
		}

		builder.append("   ├─ accountsLedger: ").append(accountsLedger).append("\n");

		builder.append("   └─ versions: ").append(versions).append("\n");

		long verificationVersion = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_VERIFICATION_VERSION + " should not return void"))
				.asReturnedLong(MethodSignatures.GET_VERIFICATION_VERSION, NodeException::new);

		builder.append("      └─ verificationVersion: ").append(verificationVersion).append("\n");

		return builder.toString();
	}
}