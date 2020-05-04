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
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.Config;
import io.hotmoka.memory.MemoryBlockchain;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.Initialization;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 */
public class MemoryBlockchainImpl extends AbstractNode implements MemoryBlockchain {

	/**
	 * The number of transactions that fit inside a block.
	 */
	public final static BigInteger TRANSACTIONS_PER_BLOCK = BigInteger.valueOf(5);

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
	 * The mempool where transaction requests are stored and eventually executed.
	 */
	private final Mempool mempool;

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
	 * The path of the basic Takamaka classes installed in blockchain.
	 */
	private final Classpath takamakaCode;

	/**
	 * The classpath of a user jar that has been installed, if any.
	 * This jar is typically referred to at construction time of the node.
	 */
	private final Classpath jar;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * True if and only if this node doesn't allow initial transactions anymore.
	 */
	private boolean initialized;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts with the given initial funds.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param jar the path of a jar that must be installed after the creation of the gamete. This is optional and mainly
	 *            useful to simplify the implementation of tests
	 * @param redGreen true if a red/green blockchain must be created
	 * @param funds the initial funds of the accounts that are created; if {@code redGreen} is true,
	 *              they must be understood in pairs, each pair for the green/red initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	public MemoryBlockchainImpl(Config config, Path takamakaCodePath, Optional<Path> jar, boolean redGreen, BigInteger... funds) throws Exception {
		super(config);

		ensureDeleted(config.dir);  // cleans the directory where the blockchain lives
		Files.createDirectories(config.dir);
		createHeaderOfCurrentBlock();
		this.mempool = new Mempool(this);

		Initialization init = new Initialization(this, takamakaCodePath, jar.isPresent() ? jar.get() : null, redGreen, funds);

		this.jar = init.jar;
		this.accounts = init.accounts().toArray(StorageReference[]::new);
		this.takamakaCode = init.takamakaCode;
	}

	@Override
	public Classpath takamakaCode() {
		return takamakaCode;
	}

	@Override
	public Optional<Classpath> jar() {
		return Optional.ofNullable(jar);
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}

	@Override
	public long getNow() throws Exception {
		// we access the block header where the transaction would be added
		Path headerPath = getPathInBlockFor(blockNumber(next()), HEADER_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(headerPath)))) {
			return ((MemoryBlockHeader) in.readObject()).time;
		}
	}

	@Override
	protected void setNext(TransactionReference next) {
		super.setNext(next);

		if (next.getNumber().remainder(TRANSACTIONS_PER_BLOCK).signum() == 0)
			try {
				createHeaderOfCurrentBlock();
			}
			catch (Exception e) {}
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
	protected Optional<TransactionReference> getTransactionReference(String id) throws TimeoutException, InterruptedException {
		String error = transactionErrors.get(id);
		if (error != null)
			throw new IllegalStateException(error);

		return Optional.ofNullable(transactions.get(id));
	}

	@Override
	protected Stream<TransactionReference> getHistory(StorageReference object) {
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
	protected Supplier<String> postTransaction(TransactionRequest<?> request) throws Exception {
		String id = mempool.add(request);
		return () -> id.toString();
	}

	@Override
	protected void expandStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws Exception {
		Path requestPath = getPathFor((LocalTransactionReference) reference, REQUEST_NAME);
		Path parent = requestPath.getParent();
		ensureDeleted(parent);
		Files.createDirectories(parent);

		try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(requestPath)))) {
			request.into(oos);
		}

		try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(getPathFor((LocalTransactionReference) reference, RESPONSE_NAME))))) {
			response.into(oos);
		}

		// we write the textual request and response in a background thread, since they are not needed
		// to the blockchain itself but are only useful for the user who wants to see the transactions
		submit(() -> {
			try {
				try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor((LocalTransactionReference) reference, RESPONSE_TXT_NAME)))) {
					output.print(response);
				}

				try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor((LocalTransactionReference) reference, REQUEST_TXT_NAME)))) {
					output.print(request);
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		super.expandStore(reference, request, response);
	}

	@Override
	protected TransactionResponse getResponse(TransactionReference reference) throws IOException, ClassNotFoundException {
		Path response = getPathFor((LocalTransactionReference) reference, RESPONSE_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
			return TransactionResponse.from(in);
		}
	}

	private static BigInteger blockNumber(TransactionReference reference) {
		return reference.getNumber().divide(TRANSACTIONS_PER_BLOCK);
	}

	private static BigInteger transactionNumber(TransactionReference reference) {
		return reference.getNumber().remainder(TRANSACTIONS_PER_BLOCK);
	}

	/**
	 * Creates the header of the current block.
	 * 
	 * @throws IOException if the header cannot be created
	 */
	private void createHeaderOfCurrentBlock() throws IOException {
		BigInteger blockNumber = blockNumber(next());
		Path headerPath = getPathInBlockFor(blockNumber, HEADER_NAME);
		ensureDeleted(headerPath.getParent());
		Files.createDirectories(headerPath.getParent());
	
		MemoryBlockHeader header = new MemoryBlockHeader();
	
		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(headerPath)))) {
			os.writeObject(header);
		}
	
		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathInBlockFor(blockNumber, HEADER_TXT_NAME)))) {
			output.print(header);
		}
	}

	/**
	 * Yields the path for the given file name inside the directory for the given transaction.
	 * 
	 * @param fileName the name of the file
	 * @return the path
	 */
	private Path getPathFor(LocalTransactionReference reference, Path fileName) {
		return getConfig().dir.resolve("b" + blockNumber(reference)).resolve("t" + transactionNumber(reference)).resolve(fileName);
	}

	/**
	 * Yields the path for a file inside the given block.
	 * 
	 * @param blockNumber the number of the block
	 * @param fileName the file name
	 * @return the path
	 */
	private Path getPathInBlockFor(BigInteger blockNumber, Path fileName) {
		return getConfig().dir.resolve("b" + blockNumber).resolve(fileName);
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
		private final long time = System.currentTimeMillis();

		@Override
		public String toString() {
			return "block creation time: " + time + " [" + formatter.format(new Date(time)) + " UTC]";
		}
	}
}