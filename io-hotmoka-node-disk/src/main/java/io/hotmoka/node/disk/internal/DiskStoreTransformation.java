/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.disk.internal;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractStoreTransformation;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.StoreException;

/**
 * A transformation of a store of a disk node.
 */
public class DiskStoreTransformation extends AbstractStoreTransformation<DiskNodeImpl, DiskNodeConfig, DiskStore, DiskStoreTransformation> {

	private final static Logger LOGGER = Logger.getLogger(DiskStoreTransformation.class.getName());

	/**
	 * Creates a transformation from an initial store.
	 * 
	 * @param store the initial store
	 * @param consensus the consensus configuration of the node having the store
	 * @param now the time to use as now for the transactions executed in the transformation
	 */
	public DiskStoreTransformation(DiskStore store, ConsensusConfig<?,?> consensus, long now) {
		super(store, consensus, now);
	}

	/**
	 * Yields the final store of this transformation, resulting from the execution of the delivered requests
	 * from the initial store.
	 * 
	 * @return the final store
	 * @throws StoreException if the final store cannot be computed correctly
	 */
	public DiskStore getFinalStore() throws StoreException {
		return getInitialStore().addDelta(getCache(), getDeltaRequests(), getDeltaResponses(), getDeltaHistories(), getDeltaManifest());
	}

	/**
	 * Takes note of the gas consumed for the execution of the requests delivered in this store transformation,
	 * consequently updates the inflation and the total supply.
	 * 
	 * @throws StoreException if the store is not able to complete the operation correctly
	 * @throws InterruptedException if the current thread is interrupted before delivering the transaction
	 */
	protected final void deliverCoinbaseTransactions() throws StoreException, InterruptedException {
		Optional<StorageReference> maybeManifest = getManifest();
		if (maybeManifest.isEmpty())
			return;

		// we use the manifest as caller, since it is an externally-owned account
		StorageReference manifest = maybeManifest.get();
		BigInteger nonce;

		try {
			nonce = getNonce(manifest);
		}
		catch (UnknownReferenceException | FieldNotFoundException e) {
			// the manifest is an account; this should not happen
			throw new StoreException(e);
		}

		StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
		TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
		BigInteger minted = getCoinsMinted(validators);
		BigInteger gasConsumed = getGasConsumed();
		LOGGER.info("coinbase: units of gas consumed for CPU, RAM or storage since the previous reward: " + gasConsumed);

		var request = TransactionRequests.instanceSystemMethodCall
				(manifest, nonce, _100_000, takamakaCode, MethodSignatures.VALIDATORS_REWARD, validators,
						StorageValues.bigIntegerOf(getReward()), StorageValues.bigIntegerOf(minted),
						StorageValues.stringOf(""), StorageValues.stringOf(""),
						StorageValues.bigIntegerOf(gasConsumed), StorageValues.bigIntegerOf(deliveredCount()));

		TransactionResponse response;

		try {
			response = responseBuilderFor(TransactionReferences.of(getHasher().hash(request)), request).getResponseCreation().getResponse();
			// if there is only one update, it is the update of the nonce of the manifest: we prefer not to expand the store with a useless transaction
			if (!(response instanceof TransactionResponseWithUpdates trwu) || trwu.getUpdates().count() > 1L)
				response = deliverTransaction(request);
		}
		catch (TransactionRejectedException e) {
			throw new StoreException("Could not perform the coinbase transaction", e);
		}

		if (response instanceof MethodCallTransactionFailedResponse responseAsFailed)
			LOGGER.log(Level.SEVERE, "coinbase: could not perform transaction: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
		else
			LOGGER.info("coinbase: units of coin minted since the previous reward: " + minted);
	}
}