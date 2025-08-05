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
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractTrieBasedStoreTransformation;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.mokamint.nonce.api.Prolog;

/**
 * A transformation of a store of a Mokamint node.
 */
public class MokamintStoreTransformation extends AbstractTrieBasedStoreTransformation<HotmokaApplicationImpl<?>.MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> {

	private final static Logger LOGGER = Logger.getLogger(MokamintStoreTransformation.class.getName());

	/**
	 * Creates a transformation whose transaction from the given initial store.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of transactions in the transformation
	 */
	protected MokamintStoreTransformation(MokamintStore store, ConsensusConfig<?,?> consensus, long now) {
		super(store, consensus, now);
	}

	@Override
	protected StoreCache getCache() {
		return super.getCache();
	}

	/**
	 * Rewards the node that created a block and the miner that provided the deadline
	 * for that block. The rewards is computed from the gas consumed in the block.
	 * Takes note of the gas consumed for the execution of the requests delivered
	 * in this store transformation, consequently updates the inflation and the total supply.
	 * 
	 * @param prolog the prolog of the block; this contains information about the public keys
	 *               that identify the creator of the node and the miner of the deadline of the block
	 * @throws InterruptedException if the current thread is interrupted before delivering the transaction
	 */
	protected void deliverCoinbaseTransactions(Prolog prolog) throws InterruptedException {
		Optional<StorageReference> maybeManifest = getManifest();
		if (maybeManifest.isEmpty())
			return;

		String publicKeyOfNodeBase58 = prolog.getPublicKeyForSigningBlocksBase58();
		// the Prolog should provide actual Base58-encoded keys, otherwise there is a bug
		String publicKeyOfNodeBase64 = Base64.toBase64String(Base58.fromBase58String(publicKeyOfNodeBase58, LocalNodeException::new));
		String publicKeyOfMinerBase58 = prolog.getPublicKeyForSigningDeadlinesBase58();
		String publicKeyOfMinerBase64 = Base64.toBase64String(Base58.fromBase58String(publicKeyOfMinerBase58, LocalNodeException::new));

		// we use the manifest as caller, since it is an externally-owned account
		StorageReference manifest = maybeManifest.get();
		BigInteger nonce;

		try {
			nonce = getNonce(manifest);
		}
		catch (UnknownReferenceException | FieldNotFoundException e) {
			// the manifest is an account; this should not happen
			throw new LocalNodeException(e);
		}

		StorageReference validators = getValidators().orElseThrow(() -> new LocalNodeException("The manifest is set but the validators are not set"));
		TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new LocalNodeException("The manifest is set but the Takamaka code reference is not set"));
		BigInteger minted = getCoinsMinted(manifest, validators);
		BigInteger gasConsumed = getGasConsumed();

		LOGGER.info("coinbase: units of gas consumed for CPU, RAM or storage since the previous reward: " + gasConsumed);

		// we split the rewarding in two calls, so that the accounts created inside the accounts ledger have a #0 progressive index
		long percentForNode = 50_000_000L;
		BigInteger percentForNodeAsBI = BigInteger.valueOf(percentForNode);
		BigInteger reward = getReward().add(minted);
		BigInteger rewardForNode = reward.multiply(percentForNodeAsBI).divide(_100_000_000);

		var request = TransactionRequests.instanceSystemMethodCall
				(manifest, nonce, _100_000, takamakaCode, MethodSignatures.VALIDATORS_REWARD_MOKAMINT_NODE, validators,
						StorageValues.bigIntegerOf(rewardForNode),
						StorageValues.bigIntegerOf(minted),
						StorageValues.stringOf(publicKeyOfNodeBase64),
						StorageValues.bigIntegerOf(gasConsumed),
						StorageValues.bigIntegerOf(deliveredCount()));

		TransactionResponse response;

		try {
			response = deliverTransaction(request);
			LOGGER.info("coinbase: rewarded " + rewardForNode + " to a node with public key " + publicKeyOfNodeBase58 + " (" + prolog.getSignatureForBlocks() + ", base58)");
		}
		catch (TransactionRejectedException e) {
			LOGGER.log(Level.SEVERE, "the coinbase transaction for rewarding the node that created the new block has been rejected", e);
			throw new LocalNodeException("The coinbase transaction for rewarding the node that created the new block has been rejected", e);
		}

		if (response instanceof MethodCallTransactionFailedResponse responseAsFailed)
			LOGGER.severe("coinbase: the coinbase transaction to reward the node that created the new block failed: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
		else {
			LOGGER.info("coinbase: units of coin minted since the previous reward: " + minted);
			BigInteger rewardForMiner = reward.subtract(rewardForNode);
			nonce = nonce.add(BigInteger.ONE);

			request = TransactionRequests.instanceSystemMethodCall
					(manifest, nonce, _100_000, takamakaCode, MethodSignatures.VALIDATORS_REWARD_MOKAMINT_MINER, validators,
							StorageValues.bigIntegerOf(rewardForMiner),
							StorageValues.stringOf(publicKeyOfMinerBase64));

			try {
				response = deliverTransaction(request);
				LOGGER.info("coinbase: rewarded " + rewardForMiner + " to a miner with public key " + publicKeyOfMinerBase58 + " (" + prolog.getSignatureForDeadlines() + ", base58)");
			}
			catch (TransactionRejectedException e) {
				LOGGER.log(Level.SEVERE, "the coinbase transaction for rewarding the miner that provided the deadline in the new block has been rejected", e);
				throw new LocalNodeException("The coinbase transaction for rewarding the miner that provided the deadline in the new block has been rejected", e);
			}

			if (response instanceof MethodCallTransactionFailedResponse responseAsFailed)
				LOGGER.severe("coinbase: the coinbase transaction to reward the miner that provided the deadline in the new block failed: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
		}
	}
}