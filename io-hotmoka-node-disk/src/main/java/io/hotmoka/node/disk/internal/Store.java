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

package io.hotmoka.node.disk.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.stores.AbstractStore;

/**
 * The store of the memory blockchain. It is not transactional and just writes
 * everything immediately into files. It keeps responses into persistent memory,
 * while the histories are kept in RAM.
 */
@ThreadSafe
class Store extends AbstractStore {

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final ConcurrentMap<StorageReference, TransactionReference[]> histories;

	/**
	 * The errors generated by each transaction (if any). In a real implementation, this must
	 * be stored in a persistent memory such as a blockchain.
	 */
	private final ConcurrentMap<TransactionReference, String> errors;

	/**
	 * The storage reference of the manifest stored inside the node, if any.
	 */
	private final AtomicReference<StorageReference> manifest = new AtomicReference<>();

	/**
	 * The number of transactions added to the store. This is used to associate
	 * each transaction to its progressive number.
	 */
	private final AtomicInteger transactionsCount = new AtomicInteger();

	/**
	 * A map from the transactions added to the store to their progressive number.
	 * This is needed in order to give a nice presentation of transactions, inside a
	 * directory for its block.
	 */
	private final ConcurrentMap<TransactionReference, Integer> progressive;

	/**
	 * The number of transactions that fit inside a block.
	 */
	private final long transactionsPerBlock;

	private final static Logger logger = Logger.getLogger(Store.class.getName());

	/**
     * Creates a state for a node.
     * 
	 * @param getResponseUncommittedCached a function that yields the transaction response for the given transaction reference, if any, using a cache
	 * @param dir the path where the database of the store gets created
     * @param transactionsPerBlock the number of transactions that fit inside a block
     */
    Store(Function<TransactionReference, Optional<TransactionResponse>> getResponseUncommittedCached, Path dir, long transactionsPerBlock) {
    	super(getResponseUncommittedCached, dir);

    	this.transactionsPerBlock = transactionsPerBlock;
    	this.histories = new ConcurrentHashMap<>();
    	this.errors = new ConcurrentHashMap<>();
    	this.progressive = new ConcurrentHashMap<>();
    }

	@Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
		synchronized (lock) {
    		try {
    			Path response = getPathFor(reference, "response");
    			try (var context = NodeUnmarshallingContexts.of(Files.newInputStream(response))) {
    				return Optional.of(TransactionResponses.from(context));
    			}
    		}
    		catch (IOException e) {
    			return Optional.empty();
    		}
    	}
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		return getResponse(reference);
	}

	@Override
	public Optional<String> getError(TransactionReference reference) {
		return Optional.ofNullable(errors.get(reference));
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		TransactionReference[] history = histories.get(object);
		return history == null ? Stream.empty() : Stream.of(history);
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) {
		return getHistory(object);
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return Optional.ofNullable(manifest.get());
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		return getManifest();
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		try {
			Path response = getPathFor(reference, "request");
			try (var context = NodeUnmarshallingContexts.of(Files.newInputStream(response))) {
				return Optional.of(TransactionRequests.from(context));
			}
		}
		catch (IOException e) {
			return Optional.empty();
		}
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		try {
			progressive.computeIfAbsent(reference, _reference -> transactionsCount.getAndIncrement());
			Path requestPath = getPathFor(reference, "request");
			Path parent = requestPath.getParent();
			ensureDeleted(parent);
			Files.createDirectories(parent);

			Files.writeString(getPathFor(reference, "response.txt"), response.toString(), StandardCharsets.UTF_8);
			Files.writeString(getPathFor(reference, "request.txt"), request.toString(), StandardCharsets.UTF_8);

			try (var context = NodeMarshallingContexts.of(Files.newOutputStream(requestPath))) {
				request.into(context);
			}

			try (var context = NodeMarshallingContexts.of(Files.newOutputStream(getPathFor(reference, "response")))) {
				response.into(context);
			}
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "unexpected exception", e);
			throw new RuntimeException("unexpected exception", e);
		}
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		histories.put(object, history.toArray(TransactionReference[]::new));
	}

	@Override
	protected void setManifest(StorageReference manifest) {
		this.manifest.set(manifest);
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		try {
			progressive.computeIfAbsent(reference, _reference -> transactionsCount.getAndIncrement());
			Path requestPath = getPathFor(reference, "request");
			Path parent = requestPath.getParent();
			ensureDeleted(parent);
			Files.createDirectories(parent);

			Files.writeString(getPathFor(reference, "request.txt"), request.toString(), StandardCharsets.UTF_8);

			try (var context = NodeMarshallingContexts.of(Files.newOutputStream(requestPath))) {
				request.into(context);
			}
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "unexpected exception", e);
			throw new RuntimeException("Unexpected exception", e);
		}

		errors.put(reference, errorMessage);
	}

	/**
	 * Yields the path for a file inside the directory for the given transaction.
	 * 
	 * @param reference the transaction reference
	 * @param name the name of the file
	 * @return the resulting path
	 * @throws FileNotFoundException if the reference is unknown
	 */
	private Path getPathFor(TransactionReference reference, String name) throws FileNotFoundException {
		Integer progressive = this.progressive.get(reference);
		if (progressive == null)
			throw new FileNotFoundException("Unknown transaction reference " + reference);

		return getDir().resolve("b" + progressive / transactionsPerBlock).resolve(progressive % transactionsPerBlock + "-" + reference).resolve(name);
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