package io.takamaka.code.memory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.engine.AbstractSequentialBlockchain;
import io.takamaka.code.engine.SequentialTransactionReference;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 */
public class MemoryBlockchain extends AbstractSequentialBlockchain {

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
	 * The number of transactions per block.
	 */
	protected final static short TRANSACTIONS_PER_BLOCK = 5;

	/**
	 * The root path where transaction are stored.
	 */
	private final Path root;

	/**
	 * The reference to the topmost transaction reference.
	 * This is {@code null} if the blockchain is empty.
	 */
	private MemoryTransactionReference topmost;

	/**
	 * The time used for <em>now</em> during the execution of the current transaction.
	 */
	private long now;

	/**
	 * Builds a blockchain that stores transaction in disk memory.
	 * 
	 * @param root the directory where blocks and transactions must be stored.
	 * @throws IOException if the root directory cannot be created
	 */
	public MemoryBlockchain(Path root) throws IOException {
		ensureDeleted(root);  // cleans the directory where the blockchain lives
		Files.createDirectories(root);

		this.root = root;

		createHeaderOfBlock(BigInteger.ZERO);
	}

	@Override
	public long getNow() {
		return now;
	}

	@Override
	protected SequentialTransactionReference getNextTransaction() {
		return topmost == null ? new MemoryTransactionReference(BigInteger.ZERO, (short) 0) : topmost.getNext();
	}

	@Override
	protected void initTransaction(BigInteger gas, TransactionReference current) throws Exception {
		super.initTransaction(gas, current);

		// we access the block header where the transaction would occur
		MemoryTransactionReference previous = topmost;
		if (previous != null) {
			MemoryTransactionReference next = previous.getNext();
			Path headerPath = getPathInBlockFor(next.blockNumber, HEADER_NAME);
			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(headerPath)))) {
				MemoryBlockHeader header = (MemoryBlockHeader) in.readObject();
				this.now = header.time;
			}
		}
		else
			// the first transaction does not use the time anyway
			this.now = 0L;
	}

	@Override
	protected SequentialTransactionReference getTopmostTransactionReference() {
		return topmost;
	}

	@Override
	protected TransactionReference getTransactionReferenceFor(String toString) {
		return new MemoryTransactionReference(toString);
	}

	@Override
	protected TransactionReference expandBlockchainWith(TransactionRequest request, TransactionResponse response) throws Exception {
		MemoryTransactionReference next = (MemoryTransactionReference) getNextTransaction();
		Path requestPath = getPathFor(next, REQUEST_NAME);
		ensureDeleted(requestPath.getParent());
		Files.createDirectories(requestPath.getParent());

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(requestPath)))) {
			os.writeObject(request);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(next, REQUEST_TXT_NAME)))) {
			output.print(request);
		}

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(getPathFor(next, RESPONSE_NAME))))) {
			os.writeObject(response);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(next, RESPONSE_TXT_NAME)))) {
			output.print(response);
		}

		topmost = next;
		if (next.isLastInBlock())
			createHeaderOfBlock(next.blockNumber.add(BigInteger.ONE));

		return next;
	}

	private void createHeaderOfBlock(BigInteger blockNumber) throws IOException {
		Path headerPath = getPathInBlockFor(blockNumber, HEADER_NAME);
		ensureDeleted(headerPath.getParent());
		Files.createDirectories(headerPath.getParent());

		MemoryBlockHeader header = new MemoryBlockHeader(System.currentTimeMillis());

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(headerPath)))) {
			os.writeObject(header);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathInBlockFor(blockNumber, HEADER_TXT_NAME)))) {
			output.print(header);
		}
	}

	@Override
	protected TransactionRequest getRequestAt(TransactionReference reference) throws FileNotFoundException, IOException, ClassNotFoundException {
		Path request = getPathFor((MemoryTransactionReference) reference, REQUEST_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(request)))) {
			return (TransactionRequest) in.readObject();
		}
	}

	@Override
	protected TransactionResponse getResponseAt(TransactionReference reference) throws Exception {
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
}