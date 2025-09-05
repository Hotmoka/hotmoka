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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.Transaction;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.api.StoreCache;

/**
 * The store of a disk blockchain. It is not transactional and just writes
 * everything immediately into files. It keeps requests and responses into
 * persistent memory, while object histories are kept in RAM.
 */
@Immutable
class DiskStore extends AbstractStore<DiskNodeImpl, DiskNodeConfig, DiskStore, DiskStoreTransformation> {

	/**
	 * The path where the database of the store gets created.
	 */
	private final Path dir;

	/**
	 * The previous store for looking up the transactions, from which this is derived by difference.
	 */
	private final Optional<DiskStore> previousForTransactions;

	/**
	 * The previous store for looking up the histories, from which this is derived by difference.
	 */
	private final Optional<DiskStore> previousForHistories;

	/**
	 * The difference of transactions added in this store.
	 */
	private final Map<TransactionReference, Transaction> deltaTransactions;

	/**
	 * The difference of histories of the objects added in this store.
	 */
	private final Map<StorageReference, TransactionReference[]> deltaHistories;

	/**
	 * The storage reference of the manifest in this store, if any.
	 */
	private final Optional<StorageReference> manifest;

	/**
	 * The storage reference of the takamaka code in this store, if any.
	 */
	private final Optional<TransactionReference> takamakaCode;

	/**
	 * The height of the block having this store.
	 */
	private final int height;

	/**
     * Creates an empty disk store for a node, with empty caches.
	 * 
	 * @param node the node for which the store is created
	 * @param dir the path where the blocks of the node must be saved on disk
	 */
    DiskStore(DiskNodeImpl node, Path dir) {
    	super(node);

    	this.dir = dir;
    	this.previousForTransactions = Optional.empty();
    	this.previousForHistories = Optional.empty();
    	this.deltaTransactions = new ConcurrentHashMap<>();
    	this.deltaHistories = new ConcurrentHashMap<>();
    	this.manifest = Optional.empty();
    	this.takamakaCode = Optional.empty();
    	this.height = 0;
    }

    /**
     * Clones a disk store, setting its cache.
     */
    private DiskStore(DiskStore toClone, StoreCache cache) {
    	super(toClone, cache);

    	this.dir = toClone.dir;
    	this.previousForTransactions = toClone.previousForTransactions;
    	this.previousForHistories = toClone.previousForHistories;
    	// no need to clone these sets, since we are not modifying them
    	this.deltaTransactions = toClone.deltaTransactions;
    	this.deltaHistories = toClone.deltaHistories;
    	this.manifest = toClone.manifest;
    	this.takamakaCode = toClone.takamakaCode;
    	this.height = toClone.height;
    }

    /**
     * Clones a disk store, using the given cache and adding the given
     * delta information to the result.
     */
    private DiskStore(DiskStore toClone, StoreCache cache,
    		LinkedHashMap<TransactionReference, Transaction> addedTransactions,
    		Map<StorageReference, TransactionReference[]> addedHistories,
    		Optional<StorageReference> addedManifest,
    		Optional<TransactionReference> addedTakamakaCode) {

    	super(toClone, cache);

    	this.dir = toClone.dir;

    	boolean historiesAreFew = toClone.deltaHistories.size() + addedHistories.size() < 5000;
    	boolean transactionsAreFew = toClone.deltaTransactions.size() + addedTransactions.size() < 5000;

    	// we apply two strategies: either the delta set of the previous store is small, and we clone it;
    	// or we point to the cloned store as previous and only report the delta information in this store;
    	// this avoids cloning big hashsets, but creates a long list of stores that make the
    	// search for requests and responses longer

    	if (transactionsAreFew) {
    		this.previousForTransactions = toClone.previousForTransactions;
    		this.deltaTransactions = new HashMap<>(toClone.deltaTransactions);
    		deltaTransactions.putAll(addedTransactions);
    	}
    	else {
    		this.previousForTransactions = Optional.of(toClone);
    		this.deltaTransactions = new HashMap<>(addedTransactions);
    	}

    	if (historiesAreFew) {
    		this.previousForHistories = toClone.previousForHistories;
    		this.deltaHistories = new HashMap<>(toClone.deltaHistories);
    		deltaHistories.putAll(addedHistories);
    	}
    	else {
    		this.previousForHistories = Optional.of(toClone);
    		this.deltaHistories = new HashMap<>(addedHistories);
    	}

    	this.manifest = addedManifest.or(() -> toClone.manifest);
    	this.takamakaCode = addedTakamakaCode.or(() -> toClone.takamakaCode);
    	this.height = toClone.height + 1;

		int progressive = 0;
		// by iterating on the transactions, we get them in order of addition to the transformation,
		// so that they are reported in the block files with their correct progressive inside the block
		for (var entry: addedTransactions.entrySet()) {
			TransactionReference reference = entry.getKey();
			Transaction transaction = entry.getValue();

			try {
				dumpRequest(progressive, reference, transaction.getRequest());
				dumpResponse(progressive++, reference, transaction.getResponse());
			}
			catch (IOException e) {
				throw new LocalNodeException(e);
			}
		}
    }

    @Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException {
		var transaction = deltaTransactions.get(reference);
		if (transaction != null)
			return transaction.getRequest();
		else
			return previousForTransactions.orElseThrow(() -> new UnknownReferenceException(reference)).getRequest(reference);
	}

	@Override
    public TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException {
    	var transaction = deltaTransactions.get(reference);
    	if (transaction != null)
    		return transaction.getResponse();
    	else
    		return previousForTransactions.orElseThrow(() -> new UnknownReferenceException(reference)).getResponse(reference);
    }

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException {
		TransactionReference[] history = deltaHistories.get(object);
		if (history != null)
			return Stream.of(history);
		else
			return previousForHistories.orElseThrow(() -> new UnknownReferenceException(object)).getHistory(object);
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return manifest;
	}

	@Override
	public Optional<TransactionReference> getTakamakaCode() {
		return takamakaCode;
	}

	@Override
	protected DiskStoreTransformation beginTransformation(ConsensusConfig<?,?> consensus, long now) {
		return new DiskStoreTransformation(this, consensus, now);
	}

	/**
	 * Yields a store derived from this by setting the given cache and adding the given extra information.
	 * 
	 * @param cache the cache for the resulting store
	 * @param addedRequests the requests to add; by iterating on them, one gets the requests
	 *                      in order of addition to the transformation
	 * @param addedResponses the responses to add
	 * @param addedHistories the histories to add
	 * @param addedManifest the manifest to add, if any
	 * @param addedTakamakaCode the takamaka code to add, if any
	 * @return the resulting store
	 */
	protected DiskStore addDelta(StoreCache cache, LinkedHashMap<TransactionReference, Transaction> addedTransactions,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest, Optional<TransactionReference> addedTakamakaCode) {

		// optimization: if we are adding no requests (and therefore no responses and no histories) then
		// we reuse the same store; moreover, the block height will remain unchanged, so that no empty blocks
		// are dumped to the file system
		if (addedTransactions.isEmpty())
			return this;
		else
			return new DiskStore(this, cache, addedTransactions, addedHistories, addedManifest, addedTakamakaCode);
	}

	@Override
	protected DiskStore withCache(StoreCache cache) {
		return new DiskStore(this, cache);
	}

	private void dumpRequest(int progressive, TransactionReference reference, TransactionRequest<?> request) throws IOException {
		Path requestPath = getPathFor(progressive, reference, "request.txt");
		Path parent = requestPath.getParent();
		ensureDeleted(parent);
		Files.createDirectories(parent);
		Files.writeString(requestPath, request.toString(), StandardCharsets.UTF_8);
	}

	private void dumpResponse(int progressive, TransactionReference reference, TransactionResponse response) throws IOException {
		Path responsePath = getPathFor(progressive, reference, "response.txt");
		Files.writeString(responsePath, response.toString(), StandardCharsets.UTF_8);
	}

	/**
	 * Yields the path for a file inside the directory for the given transaction.
	 * 
	 * @param reference the transaction reference
	 * @param name the name of the file
	 * @return the resulting path
	 * @throws FileNotFoundException if the reference is unknown
	 */
	private Path getPathFor(int progressive, TransactionReference reference, String name) throws FileNotFoundException {
		return dir.resolve("b" + height).resolve(progressive + "-" + reference).resolve(name);
	}

	/**
	 * Deletes the given directory, recursively, if it exists.
	 * 
	 * @param dir the directory
	 * @throws IOException if a disk error occurs
	 */
	private static void ensureDeleted(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}
}