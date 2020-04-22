package io.hotmoka.memory.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.AbstractNode;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 */
public abstract class AbstractMemoryBlockchain extends AbstractNode {

	/**
	 * The name used for the file containing the serialized header of a block.
	 */
	private static final Path HEADER_NAME = Paths.get("header");

	/**
	 * The name used for the file containing the textual header of a block.
	 */
	private final static Path HEADER_TXT_NAME = Paths.get("header.txt");

	/**
	 * The name used for the file containing the serialized request of a transaction.
	 */
	private final static Path REQUEST_NAME = Paths.get("request");

	/**
	 * The name used for the file containing the serialized response of a transaction.
	 */
	private final static Path RESPONSE_NAME = Paths.get("response");

	/**
	 * The name used for the file containing the textual request of a transaction.
	 */
	private final static Path REQUEST_TXT_NAME = Paths.get("request.txt");

	/**
	 * The name used for the file containing the textual response of a transaction.
	 */
	private final static Path RESPONSE_TXT_NAME = Paths.get("response.txt");

	/**
	 * The number of transactions that fit inside a block.
	 */
	public final static int TRANSACTIONS_PER_BLOCK = 5;

	/**
	 * The root path where the blocks are stored.
	 */
	private final Path root;

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	private final Classpath takamakaCode;

	private final Mempool mempool;

	/**
	 * The reference that identifies the next transaction, that will run for the next request.
	 */
	private MemoryTransactionReference next;

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final Map<StorageReference, TransactionReference[]> histories = new HashMap<>();

	/**
	 * A map from the identifier created at time of posting a request to
	 * the transaction that has been computed, if any.
	 */
	private final ConcurrentMap<String, TransactionReference> transactions = new ConcurrentHashMap<>();

	/**
	 * A map from the identifier created at time of posting a request to
	 * the error message that resulted from the post, if any.
	 */
	private final ConcurrentMap<String, String> transactionErrors = new ConcurrentHashMap<>();

	/**
	 * True if and only if this node doesn't allow initial transactions anymore.
	 */
	private boolean initialized;

	/**
	 * Builds a blockchain that stores transaction in disk memory.
	 * 
	 * @param root the directory where blocks and transactions must be stored.
	 * @throws IOException if the root directory cannot be created
	 * @throws TransactionException if the initialization of the blockchain fails
	 */
	protected AbstractMemoryBlockchain(Path takamakaCodePath) throws IOException, TransactionRejectedException {
		this.root = Paths.get("chain");
		ensureDeleted(root);  // cleans the directory where the blockchain lives
		Files.createDirectories(root);
		this.next = new MemoryTransactionReference(BigInteger.ZERO, (short) 0);
		createHeaderOfCurrentBlock();
		this.mempool = new Mempool(this);
		TransactionReference support = addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath)));
		this.takamakaCode = new Classpath(support, false);
	}

	public final Classpath takamakaCode() {
		return takamakaCode;
	}

	@Override
	public long getNow() throws Exception {
		// we access the block header where the transaction would be added
		Path headerPath = getPathInBlockFor(next.blockNumber, HEADER_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(headerPath)))) {
			return ((MemoryBlockHeader) in.readObject()).time;
		}
	}

	@Override
	public void close() throws Exception {
		mempool.stop();
		super.close();
	}

	protected void setTransactionReferenceFor(String id, TransactionReference reference) {
		transactions.put(id, reference);
	}

	protected void setTransactionErrorFor(String id, String message) {
		transactionErrors.put(id, message);
	}

	@Override
	public TransactionReference getTransactionReferenceFor(String id) throws Exception {
		int delay = POLLING_DELAY;

		for (int i = 0; i < MAX_POLLING_ATTEMPTS; i++) {
			TransactionReference result = transactions.get(id);
			if (result != null)
				return result;

			String error = transactionErrors.get(id);
			if (error != null)
				throw new IllegalStateException(error);

			Thread.sleep(delay);

			// we increase the delay, for next attempt
			delay = 110 * delay / 100;
		}

		throw new TimeoutException("cannot find transaction " + id);
	}

	@Override
	protected Stream<TransactionReference> getHistoryOf(StorageReference object) {
		TransactionReference[] history = histories.get(object);
		return history == null ? Stream.empty() : Stream.of(history);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		histories.put(object, history.toArray(TransactionReference[]::new));
	}

	@Override
	protected boolean isInitialized() {
		return initialized;
	}

	@Override
	protected void markAsInitialized() {
		initialized = true;
	}

	@Override
	protected TransactionReference next() {
		synchronized (lockGetNext) {
			return next;
		}
	}

	private final Object lockGetNext = new Object();

	@Override
	protected TransactionReference nextAndIncrement() {
		TransactionReference result;

		synchronized (lockGetNext) {
			result = next;
			next = next.getNext();

			if (next.transactionNumber == 0)
				try {
					createHeaderOfCurrentBlock();
				}
				catch (Exception e) {}
		}

		return result;
	}

	@Override
	protected Supplier<String> postTransaction(TransactionRequest<?> request) throws Exception {
		String id = mempool.add(request);
		return () -> id.toString();
	}

	@Override
	protected void expandStoreWith(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws Exception {
		Path requestPath = getPathFor((MemoryTransactionReference) reference, REQUEST_NAME);
		Path parent = requestPath.getParent();
		ensureDeleted(parent);
		Files.createDirectories(parent);

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(requestPath)))) {
			os.writeObject(request);
		}

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(getPathFor((MemoryTransactionReference) reference, RESPONSE_NAME))))) {
			os.writeObject(response);
		}

		// we write the textual request and response in a background thread, since they are not needed
		// to the blockchain itself but are only useful for the user who wants to see the transactions
		submit(() -> {
			try {
				try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor((MemoryTransactionReference) reference, RESPONSE_TXT_NAME)))) {
					output.print(response);
				}

				try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor((MemoryTransactionReference) reference, REQUEST_TXT_NAME)))) {
					output.print(request);
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		super.expandStoreWith(reference, request, response);
	}

	/**
	 * Creates the header of the current block.
	 * 
	 * @throws IOException if the header cannot be created
	 */
	private void createHeaderOfCurrentBlock() throws IOException {
		Path headerPath = getPathInBlockFor(next.blockNumber, HEADER_NAME);
		ensureDeleted(headerPath.getParent());
		Files.createDirectories(headerPath.getParent());

		MemoryBlockHeader header = new MemoryBlockHeader();

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(headerPath)))) {
			os.writeObject(header);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathInBlockFor(next.blockNumber, HEADER_TXT_NAME)))) {
			output.print(header);
		}
	}

	@Override
	protected TransactionResponse getResponseAtInternal(TransactionReference reference) throws IOException, ClassNotFoundException {
		Path response = getPathFor((MemoryTransactionReference) reference, RESPONSE_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
			return (TransactionResponse) in.readObject();
		}
	}

	/**
	 * Yields the path for the given file name inside the directory for the given transaction.
	 * 
	 * @param fileName the name of the file
	 * @return the path
	 */
	private Path getPathFor(MemoryTransactionReference reference, Path fileName) {
		return root.resolve("b" + reference.blockNumber).resolve("t" + reference.transactionNumber).resolve(fileName);
	}

	/**
	 * Yields the path for a file inside the given block.
	 * 
	 * @param blockNumber the number of the block
	 * @param fileName the file name
	 * @return the path
	 */
	private Path getPathInBlockFor(BigInteger blockNumber, Path fileName) {
		return root.resolve("b" + blockNumber).resolve(fileName);
	}

	/**
	 * Deletes the given directory, if it exists.
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

	/**
	 * The header of a block. It contains the time that must be used
	 * as {@code now} by the transactions that will be added to the block.
	 */
	private static class MemoryBlockHeader implements Serializable {
		private static final long serialVersionUID = 6163345302977772036L;
		private final static DateFormat formatter;

		static {
			formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
			formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		/**
		 * The time of creation of the block, as returned by {@link java.lang.System#currentTimeMillis()}.
		 */
		private final long time;

		/**
		 * Builds block header.
		 */
		private MemoryBlockHeader() {
			this.time = System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return "block creation time: " + time + " [" + formatter.format(new Date(time)) + " UTC]";
		}
	}
}