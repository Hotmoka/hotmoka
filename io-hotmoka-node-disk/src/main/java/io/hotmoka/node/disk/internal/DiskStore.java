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
	 * The requests in this store.
	 */
	private final Map<TransactionReference, TransactionRequest<?>> requests;

	/**
	 * The responses in this store.
	 */
	private final Map<TransactionReference, TransactionResponse> responses;

	/**
	 * The histories of the objects in this store.
	 */
	private final Map<StorageReference, TransactionReference[]> histories;

	/**
	 * The storage reference of the manifest in this store, if any.
	 */
	private final Optional<StorageReference> manifest;

	/**
	 * The number of the block having this store.
	 */
	private final int blockNumber;

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
    	this.requests = new ConcurrentHashMap<>();
    	this.responses = new ConcurrentHashMap<>();
    	this.histories = new ConcurrentHashMap<>();
    	this.manifest = Optional.empty();
    	this.blockNumber = 0;
    }

	/**
     * Clones a disk store, using the given cache.
     */
    private DiskStore(DiskStore toClone, StoreCache cache) {
    	super(toClone, cache);

    	this.dir = toClone.dir;
    	this.requests = new HashMap<>(toClone.requests);
    	this.responses = new HashMap<>(toClone.responses);
    	this.histories = new HashMap<>(toClone.histories);
    	this.manifest = toClone.manifest;
    	this.blockNumber = toClone.blockNumber;
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
    	this.requests = new HashMap<>(toClone.requests);
    	this.responses = new HashMap<>(toClone.responses);
    	this.histories = new HashMap<>(toClone.histories);
    	this.manifest = addedManifest.or(() -> toClone.manifest);
    	this.blockNumber = addedRequests.isEmpty() ? toClone.blockNumber : toClone.blockNumber + 1;

		int progressive = 0;
		for (var entry: addedRequests.entrySet())
			dumpRequest(progressive++, entry.getKey(), entry.getValue());

		progressive = 0;
		for (var entry: addedResponses.entrySet())
			dumpResponse(progressive++, entry.getKey(), entry.getValue());

		for (var entry: addedHistories.entrySet())
			histories.put(entry.getKey(), entry.getValue());
    }

    @Override
    public TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException {
    	var response = responses.get(reference);
    	if (response != null)
    		return response;
    	else
    		throw new UnknownReferenceException(reference);
    }

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException {
		TransactionReference[] history = histories.get(object);
		if (history != null)
			return Stream.of(history);
		else
			throw new UnknownReferenceException(object);
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return manifest;
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException {
		var request = requests.get(reference);
    	if (request != null)
    		return request;
    	else
    		throw new UnknownReferenceException(reference);
	}

	@Override
	protected DiskStoreTransformation beginTransformation(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		return new DiskStoreTransformation(this, executors, consensus, now);
	}

	@Override
	protected DiskStore addDelta(StoreCache cache, Map<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest) throws StoreException {
	
		return new DiskStore(this, cache, addedRequests, addedResponses, addedHistories, addedManifest);
	}

	@Override
	protected DiskStore setCache(StoreCache cache) {
		return new DiskStore(this, cache);
	}

	private void dumpRequest(int progressive, TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		requests.put(reference, request);

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
		responses.put(reference, response);

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
		return dir.resolve("b" + blockNumber).resolve(progressive + "-" + reference).resolve(name);
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

	@Override
	public void close() {}
}