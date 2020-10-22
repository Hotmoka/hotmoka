package io.hotmoka.tendermint.internal;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.stores.PartialTrieBasedFlatHistoryStore;
import io.hotmoka.xodus.ByteIterable;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@ThreadSafe
class Store extends PartialTrieBasedFlatHistoryStore<TendermintBlockchainImpl> {

	/**
	 * The Xodus store that holds configuration data.
	 */
	private final io.hotmoka.xodus.env.Store storeOfConfig;

	/**
	 * The constant used in {@link #storeOfConfig} to hold the Tendermint chain id.
	 */
	private final static ByteIterable CHAIN_ID = ByteIterable.fromByte((byte) 0);

	/**
	 * The hashing algorithm used to merge the hashes of the many tries.
	 */
	private final HashingAlgorithm<byte[]> hashOfHashes;

	/**
     * Creates a store for the Tendermint blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node for which the store is being built
     */
    Store(TendermintBlockchainImpl node) {
    	super(node);

    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfConfig = new AtomicReference<>();

    	recordTime(() -> env.executeInTransaction(txn -> {
    		storeOfConfig.set(env.openStoreWithoutDuplicates("config", txn));
    	}));

    	this.storeOfConfig = storeOfConfig.get();

    	setRootsAsCheckedOut();

    	try {
    		this.hashOfHashes = HashingAlgorithm.sha256((byte[] bytes) -> bytes);
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw InternalFailureException.of(e);
    	}
    }

    @Override
	public Optional<String> getError(TransactionReference reference) {
		try {
			// error messages are held inside the Tendermint blockchain
			return node.getTendermint().getErrorMessage(reference.getHash());
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		try {
			// requests are held inside the Tendermint blockchain
			return node.getTendermint().getRequest(reference.getHash());
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		// nothing to do, since Tendermint keeps error messages inside the blockchain, in the field "data" of its transactions
	}

	/**
	 * Sets the chain id of the node, so that it can be recovered if the node is restarted.
	 * 
	 * @param chainId the chain id
	 */
	void setChainId(String chainId) {
		recordTime(() -> env.executeInTransaction(txn -> storeOfConfig.put(txn, CHAIN_ID, ByteIterable.fromBytes(chainId.getBytes()))));
	}

	/**
	 * Sets information about the {@code index}th validator of the underlying Tendermint
	 * blockchain, at its beginning.
	 * 
	 * @param index the index number of the validator
	 * @param address the Tendermint address of the validator
	 * @param power the power of the validator
	 */
	void setOriginalValidator(int index, String address, long power) {
		recordTime(() -> env.executeInTransaction(txn -> {
			storeOfConfig.put(txn, originalValidatorKey(index), ByteIterable.fromBytes(address.getBytes()));
			storeOfConfig.put(txn, originalValidatorPowerKey(index), ByteIterable.fromBytes(String.valueOf(power).getBytes()));
		}));

		logger.info("Stored Tendermint's original validator #" + index + ": " + address + " with power " + power);
	}

	/**
	 * Yields the chain id of the node.
	 * 
	 * @return the chain id
	 */
 	Optional<String> getChainId() {
		return recordTime(() -> {
			ByteIterable chainIdAsByteIterable = env.computeInReadonlyTransaction(txn -> storeOfConfig.get(txn, CHAIN_ID));
			if (chainIdAsByteIterable == null)
				return Optional.empty();
			else
				return Optional.of(new String(chainIdAsByteIterable.getBytes()));
		});
	}

 	/**
 	 * Yields the Tendermint address of the {@code index}th original validator of the
 	 * Tendermint blockchain.
 	 * 
 	 * @param index the index of the validator, from 0 onwards
 	 * @return the address of the validator, if any. Note that this might not be a validator
 	 *         anymore, since the set of validators changes dynamically
 	 */
 	Optional<String> getOriginalValidatorAddress(int index) {
 		return recordTime(() -> {
			ByteIterable originalValidatorAddressAsByteIterable = env.computeInReadonlyTransaction(txn -> storeOfConfig.get(txn, originalValidatorKey(index)));
			if (originalValidatorAddressAsByteIterable == null)
				return Optional.empty();
			else
				return Optional.of(new String(originalValidatorAddressAsByteIterable.getBytes()));
		});
 	}

 	/**
 	 * Yields the power of the {@code index}th original validator of the
 	 * Tendermint blockchain.
 	 * 
 	 * @param index the index of the validator, from 0 onwards
 	 * @return the power of the validator, if any. Note that it might not be a validator
 	 *         anymore, since the set of validators changes dynamically
 	 */
 	Optional<Long> getOriginalValidatorPower(int index) {
 		return recordTime(() -> {
			ByteIterable originalValidatorPowerAsByteIterable = env.computeInReadonlyTransaction(txn -> storeOfConfig.get(txn, originalValidatorPowerKey(index)));
			if (originalValidatorPowerAsByteIterable == null)
				return Optional.empty();
			else
				return Optional.of(Long.parseLong(new String(originalValidatorPowerAsByteIterable.getBytes())));
		});
 	}

 	/**
	 * Yields the hash of this store. It is computed from the roots of its tries.
	 * 
	 * @return the hash. If the store is currently empty, it yields an empty array of bytes
	 */
	synchronized byte[] getHash() {
		return isEmpty() ?
			new byte[0] : // Tendermint requires an empty array at the beginning, for consensus
			hashOfHashes.hash(mergeRootsOfTries()); // we hash the result into 32 bytes
	}

	/**
	 * The constant used in {@link #storeOfConfig} to hold the
	 * Tendermint address of an original validator of the Tendermint blockchain.
	 * 
	 * @param index the validator index, from 0 onwards
	 */
	private static ByteIterable originalValidatorKey(int index) {
		return ByteIterable.fromBytes(("validator #" + index).getBytes());
	}

	/**
	 * The constant used in {@link #storeOfConfig} to hold the power of
	 * an original validator of the Tendermint blockchain.
	 * 
	 * @param index the validator index, from 0 onwards
	 */
	private static ByteIterable originalValidatorPowerKey(int index) {
		return ByteIterable.fromBytes(("power #" + index).getBytes());
	}
}