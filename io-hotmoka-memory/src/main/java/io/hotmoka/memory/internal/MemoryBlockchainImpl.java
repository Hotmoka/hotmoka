package io.hotmoka.memory.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
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
public class MemoryBlockchainImpl extends AbstractNode<Config> implements MemoryBlockchain {

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

	private final Map<TransactionReference, String> errors = new HashMap<>();

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

	private final static Logger logger = LoggerFactory.getLogger(MemoryBlockchainImpl.class);

	/**
	 * Builds a blockchain in disk memory, installs some jars in blockchain
	 * and initializes user accounts with the given initial funds.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@linkplain #takamakaCode()}
	 * @param jar the path of a jar that must be further installed after the creation of the gamete. This is optional and mainly
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
	public long getNow() {
		return System.currentTimeMillis();
	}

	@Override
	public void close() throws Exception {
		mempool.stop();
		super.close();
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
	protected void postTransaction(TransactionRequest<?> request) {
		mempool.add(request);
	}

	@Override
	protected void expandStore(TransactionRequest<?> request, TransactionResponse response) {
		try {
			TransactionReference reference = referenceOf(request);
			Path requestPath = getPathFor(reference, REQUEST_NAME);
			Path parent = requestPath.getParent();
			ensureDeleted(parent);
			Files.createDirectories(parent);

			// we write the textual request and response in a background thread, since they are not needed
			// to the blockchain itself but are only useful for the user who wants to see the transactions
			submit(() -> {
				try {
					try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(reference, RESPONSE_TXT_NAME)))) {
						output.print(response);
					}

					try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(reference, REQUEST_TXT_NAME)))) {
						output.print(request);
					}
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});

			try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(requestPath))) {
				request.into(oos);
			}

			try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(getPathFor((LocalTransactionReference) reference, RESPONSE_NAME)))) {
				response.into(oos);
			}
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}

		super.expandStore(request, response);
	}

	@Override
	protected TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
		try {
			Path response = getPathFor(reference, REQUEST_NAME);
			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
				return TransactionRequest.from(in);
			}
		}
		catch (FileNotFoundException e) {
			throw new NoSuchElementException("unknown transaction reference " + reference);
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		try {
			String error = errors.get(reference);
			if (error != null)
				throw new TransactionRejectedException(error);

			Path response = getPathFor(reference, RESPONSE_NAME);
			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
				return TransactionResponse.from(in);
			}
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (NoSuchFileException e) {
			throw new NoSuchElementException("unknown transaction reference " + reference);
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void notifyTransactionUndelivered(TransactionRequest<?> request, String errorMessage) {
		try {
			errors.put(referenceOf(request), errorMessage);
			super.notifyTransactionUndelivered(request, errorMessage);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the path for a file inside the directory for the given transaction.
	 * 
	 * @param reference the transaction reference
	 * @param path the relative path of the file
	 * @return the resulting path
	 */
	private Path getPathFor(TransactionReference reference, Path path) {
		return getConfig().dir.resolve(reference.getHash()).resolve(path);
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
}