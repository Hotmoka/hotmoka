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

package io.hotmoka.node.mokamint.internal;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Logger;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractTrieBasedStoreTransformation;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.mokamint.nonce.api.Prolog;

/**
 * A transformation of a store of a Tendermint node.
 */
public class MokamintStoreTransformation extends AbstractTrieBasedStoreTransformation<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> {

	private final static Logger LOGGER = Logger.getLogger(MokamintStoreTransformation.class.getName());

	/**
	 * Creates a transformation whose transaction are executed with the given executors.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of transactions in the transformation
	 */
	protected MokamintStoreTransformation(MokamintStore store, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, consensus, now);
	}

	public void deliverRewardForNodeAndMiner(Prolog prolog) throws StoreException, InterruptedException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
				String publicKeyOfNodeBase58 = prolog.getPublicKeyForSigningBlocksBase58();
				String publicKeyOfNodeBase64 = Base64.toBase64String(Base58.decode(publicKeyOfNodeBase58));
				String publicKeyOfMinerBase58 = prolog.getPublicKeyForSigningDeadlinesBase58();
				String publicKeyOfMinerBase64 = Base64.toBase64String(Base58.decode(publicKeyOfMinerBase58));

				// we use the manifest as caller, since it is an externally-owned account
				StorageReference manifest = maybeManifest.get();
				BigInteger nonce = getNonce(manifest);
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
	
				// we determine how many coins have been minted during the last reward:
				// it is the price of the gas distributed minus the same price without inflation
				BigInteger coins = getCoins();
				BigInteger minted = coins.subtract(getCoinsWithoutInflation());
	
				// it might happen that the last distribution goes beyond the limit imposed
				// as final supply: in that case we truncate the minted coins so that the current
				// supply reaches the final supply, exactly; this might occur from below (positive inflation)
				// or from above (negative inflation)
				BigInteger currentSupply = getCurrentSupply(validators);
				if (minted.signum() > 0) {
					BigInteger finalSupply = getConfig().getFinalSupply();
					BigInteger extra = finalSupply.subtract(currentSupply.add(minted));
					if (extra.signum() < 0)
						minted = minted.add(extra);
				}
				else if (minted.signum() < 0) {
					BigInteger finalSupply = getConfig().getFinalSupply();
					BigInteger extra = finalSupply.subtract(currentSupply.add(minted));
					if (extra.signum() > 0)
						minted = minted.add(extra);
				}

				LOGGER.info("rewarding " + coins.divide(BigInteger.TWO) + " to a node with public key " + publicKeyOfNodeBase58 + " (" + prolog.getSignatureForBlocks() + ", base58)");
				LOGGER.info("rewarding " + coins.divide(BigInteger.TWO) + " to a miner with public key " + publicKeyOfMinerBase58 + " (" + prolog.getSignatureForDeadlines() + ", base58)");

				/*var request = TransactionRequests.instanceSystemMethodCall
					(manifest, nonce, _100_000, takamakaCode, MethodSignatures.VALIDATORS_REWARD, validators,
					StorageValues.bigIntegerOf(coins), StorageValues.bigIntegerOf(minted),
					StorageValues.stringOf(behaving), StorageValues.stringOf(misbehaving),
					StorageValues.bigIntegerOf(gasConsumed), StorageValues.bigIntegerOf(deliveredCount()));
	
				TransactionResponse response = responseBuilderFor(TransactionReferences.of(store.getHasher().hash(request)), request).getResponseCreation().getResponse();
				// if there is only one update, it is the update of the nonce of the manifest: we prefer not to expand
				// the store with the transaction, so that the state stabilizes, which might give
				// to the node the chance of suspending the generation of new blocks
				if (!(response instanceof TransactionResponseWithUpdates trwu) || trwu.getUpdates().count() > 1L)
					response = deliverTransaction(request);
	
				if (response instanceof MethodCallTransactionFailedResponse responseAsFailed)
					LOGGER.log(Level.WARNING, "could not reward the validators: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
				else {
					LOGGER.info("units of gas consumed for CPU, RAM or storage since the previous reward: " + gasConsumed);
					LOGGER.info("units of coin rewarded to the validators for their work since the previous reward: " + coins);
					LOGGER.info("units of coin minted since the previous reward: " + minted);
				}
				*/
			}
		}
		catch (/*TransactionRejectedException | */ FieldNotFoundException | UnknownReferenceException | Base58ConversionException e) {
			throw new StoreException("Could not reward the node and the miner", e);
		}
	}
}