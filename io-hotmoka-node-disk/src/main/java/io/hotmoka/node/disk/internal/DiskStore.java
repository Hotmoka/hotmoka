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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;

/**
 * The store of a disk blockchain. It is not transactional and just writes
 * everything immediately into files. It keeps requests and responses into
 * persistent memory, while the histories are kept in RAM.
 */
@Immutable
class DiskStore extends AbstractStore<DiskStore, DiskStoreTransformation> {

	/**
	 * The path where the database of the store gets created.
	 */
	private final Path dir;

	/**
	 * The previous store, from which this is derived by difference.
	 */
	private final Optional<DiskStore> previous;

	/**
	 * The difference of requests added in this store.
	 */
	private final Map<TransactionReference, TransactionRequest<?>> deltaRequests;

	/**
	 * The difference of responses added in this store.
	 */
	private final Map<TransactionReference, TransactionResponse> deltaResponses;

	/**
	 * The difference of histories of the objects added in this store.
	 */
	private final Map<StorageReference, TransactionReference[]> deltaHistories;

	/**
	 * The storage reference of the manifest in this store, if any.
	 */
	private final Optional<StorageReference> manifest;

	/**
	 * The height of the block having this store.
	 */
	private final int blockHeight;

	/**
     * Creates the starting disk store of a node.
	 * 
	 * @param executors the executors to use for running transactions
	 * @param consensus the consensus configuration of the node having the store
	 * @param config the local configuration of the node having the store
	 * @param hasher the hasher for computing the transaction reference from the requests
	 */
    DiskStore(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
    	super(executors, consensus, config, hasher);

    	this.dir = config.getDir();
    	this.previous = Optional.empty();
    	this.deltaRequests = new ConcurrentHashMap<>();
    	this.deltaResponses = new ConcurrentHashMap<>();
    	this.deltaHistories = new ConcurrentHashMap<>();
    	this.manifest = Optional.empty();
    	this.blockHeight = 0;
    }

	/**
     * Clones a disk store, using the given cache.
     */
    private DiskStore(DiskStore toClone, StoreCache cache) {
    	super(toClone, cache);

    	this.dir = toClone.dir;
    	this.previous = toClone.previous;
    	// no need to clone these sets, since we are not modifying them
    	this.deltaRequests = toClone.deltaRequests;
    	this.deltaResponses = toClone.deltaResponses;
    	this.deltaHistories = toClone.deltaHistories;
    	this.manifest = toClone.manifest;
    	this.blockHeight = toClone.blockHeight;
    }

	/**
     * Clones a disk store, using the given cache and adding the given
     * delta information to the result.
     */
    private DiskStore(DiskStore toClone, StoreCache cache,
    		Map<TransactionReference, TransactionRequest<?>> addedRequests,
    		Map<TransactionReference, TransactionResponse> addedResponses,
    		Map<StorageReference, TransactionReference[]> addedHistories,
    		Optional<StorageReference> addedManifest) throws StoreException {

    	super(toClone, cache);

    	this.dir = toClone.dir;

    	// we apply two strategies: either the delta set of the previous store is small, and we clone it
    	if (toClone.deltaHistories.size() + addedHistories.size() < 2000) {
    		this.previous = toClone.previous;
    		this.deltaRequests = new HashMap<>(toClone.deltaRequests);
    		this.deltaResponses = new HashMap<>(toClone.deltaResponses);
    		this.deltaHistories = new HashMap<>(toClone.deltaHistories);
    		deltaRequests.putAll(addedRequests);
    		deltaResponses.putAll(addedResponses);
    		deltaHistories.putAll(addedHistories);
    	}
    	else {
    		// or we point to the cloned store as previous and only report the delta information in this store;
    		// this avoids cloning big hashsets, but creates a long list of stores that make the
    		// search for requests and responses longer
    		this.previous = Optional.of(toClone);
    		this.deltaRequests = new HashMap<>(addedRequests);
    		this.deltaResponses = new HashMap<>(addedResponses);
    		this.deltaHistories = new HashMap<>(addedHistories);
    	}

    	this.manifest = addedManifest.or(() -> toClone.manifest);
    	this.blockHeight = toClone.blockHeight + 1;

		int progressive = 0;
		for (var entry: addedRequests.entrySet())
			dumpRequest(progressive++, entry.getKey(), entry.getValue());

		progressive = 0;
		for (var entry: addedResponses.entrySet())
			dumpResponse(progressive++, entry.getKey(), entry.getValue());
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException {
    	var response = deltaResponses.get(reference);
    	if (response != null)
    		return response;
    	else if (previous.isEmpty())
    		throw new UnknownReferenceException(reference);
    	else
    		return previous.get().getResponse(reference);
    }

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException {
		TransactionReference[] history = deltaHistories.get(object);
		if (history != null)
			return Stream.of(history);
		else if (previous.isEmpty())
			throw new UnknownReferenceException(object);
		else
			return previous.get().getHistory(object);
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return manifest;
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException {
		var request = deltaRequests.get(reference);
    	if (request != null)
    		return request;
    	else if (previous.isEmpty())
    		throw new UnknownReferenceException(reference);
    	else
    		return previous.get().getRequest(reference);
	}

	@Override
	protected DiskStoreTransformation beginTransformation(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		return new DiskStoreTransformation(this, executors, consensus, now);
	}

	@Override
	protected DiskStore addDelta(StoreCache cache, Map<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest) throws StoreException {

		// optimization: if we are adding no requests (and therefore no responses and no histories) then
		// we reuse the same store; moreover, the block height will remain unchanged, so that no empty blocks
		// are dumped to the file system
		if (addedRequests.isEmpty())
			return this;
		else
			return new DiskStore(this, cache, addedRequests, addedResponses, addedHistories, addedManifest);
	}

	@Override
	protected DiskStore setCache(StoreCache cache) {
		return new DiskStore(this, cache);
	}

	private void dumpRequest(int progressive, TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		try {
			Path requestPath = getPathFor(progressive, reference, "request.txt");
			Path parent = requestPath.getParent();
			ensureDeleted(parent);
			Files.createDirectories(parent);
			Files.writeString(requestPath, request.toString(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
	}

	private void dumpResponse(int progressive, TransactionReference reference, TransactionResponse response) throws StoreException {
		try {
			Path responsePath = getPathFor(progressive, reference, "response.txt");
			Files.writeString(responsePath, response.toString(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
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
		return dir.resolve("b" + blockHeight).resolve(progressive + "-" + reference).resolve(name);
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