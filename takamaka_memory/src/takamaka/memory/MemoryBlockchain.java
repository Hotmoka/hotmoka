package takamaka.memory;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.TransactionRequest;
import takamaka.blockchain.TransactionResponse;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 */
public class MemoryBlockchain extends AbstractBlockchain {

	/**
	 * The name used for the file containing the serialized request of a transaction.
	 */
	public final static Path REQUEST_NAME = Paths.get("request");

	/**
	 * The name used for the file containing the serialized response of a transaction.
	 */
	public final static Path RESPONSE_NAME = Paths.get("response");

	/**
	 * The name used for the file containing the textual request of a transaction.
	 */
	public final static Path REQUEST_TXT_NAME = Paths.get("request.txt");

	/**
	 * The name used for the file containing the textual response of a transaction.
	 */
	public final static Path RESPONSE_TXT_NAME = Paths.get("response.txt");

	/**
	 * The root path where transaction are stored.
	 */
	private final Path root;

	/**
	 * The number of transactions per block.
	 */
	private final short transactionsPerBlock;

	/**
	 * The block of the current transaction.
	 */
	private BigInteger currentBlock = BigInteger.ZERO;

	/**
	 * The progressive transaction number inside the current block,
	 * for the current transaction.
	 */
	private short currentTransaction;

	/**
	 * Builds a blockchain that stores transaction in disk memory.
	 * 
	 * @param root the directory where blocks and transactions must be stored.
	 * @param transactionsPerBlock the number of transactions inside each block
	 * @throws IOException if the root directory cannot be created
	 */
	public MemoryBlockchain(Path root, short transactionsPerBlock) throws IOException {
		if (root == null)
			throw new NullPointerException("A root path must be specified");

		if (transactionsPerBlock <= 0)
			throw new IllegalArgumentException("transactionsPerBlock must be positive");

		// cleans the directory where the blockchain lives
		ensureDeleted(root);
		Files.createDirectories(root);

		this.root = root;
		this.transactionsPerBlock = transactionsPerBlock;
	}

	@Override
	public MemoryTransactionReference getCurrentTransactionReference() {
		return new MemoryTransactionReference(currentBlock, currentTransaction);
	}

	@Override
	protected void expandBlockchainWith(TransactionRequest request, TransactionResponse response) throws Exception {
		Path requestPath = getCurrentPathFor(REQUEST_NAME);
		ensureDeleted(requestPath.getParent());
		Files.createDirectories(requestPath.getParent());

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(requestPath.toFile())))) {
			os.writeObject(request);
		}

		requestPath = getCurrentPathFor(REQUEST_TXT_NAME);
		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(requestPath.toFile())))) {
			output.print(request);
		}

		Path responsePath = getCurrentPathFor(RESPONSE_NAME);

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(responsePath.toFile())))) {
			os.writeObject(response);
		}

		responsePath = getCurrentPathFor(RESPONSE_TXT_NAME);
		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(responsePath.toFile())))) {
			output.print(response);
		}
	}

	@Override
	protected TransactionRequest getRequestAt(TransactionReference reference) throws FileNotFoundException, IOException, ClassNotFoundException {
		Path request = getPathFor((MemoryTransactionReference) reference, REQUEST_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(request.toFile()))) {
			return (TransactionRequest) in.readObject();
		}
	}

	@Override
	protected TransactionResponse getResponseAt(TransactionReference reference) throws Exception {
		Path response = getPathFor((MemoryTransactionReference) reference, RESPONSE_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(response.toFile()))) {
			return (TransactionResponse) in.readObject();
		}
	}

	@Override
	protected void stepToNextTransactionReference() {
		if (++currentTransaction == transactionsPerBlock) {
			currentTransaction = 0;
			currentBlock = currentBlock.add(BigInteger.ONE);
		}
	}

	@Override
	protected MemoryTransactionReference previousTransaction(TransactionReference transaction) {
		MemoryTransactionReference transactionAsMTR = (MemoryTransactionReference) transaction;
		if (transactionAsMTR.transactionNumber == 0)
			if (transactionAsMTR.blockNumber.signum() == 0)
				throw new IllegalStateException("Transaction has no previous transaction");
			else
				return new MemoryTransactionReference(transactionAsMTR.blockNumber.subtract(BigInteger.ONE), (short) (transactionsPerBlock - 1));
		else
			return new MemoryTransactionReference(transactionAsMTR.blockNumber, (short) (transactionAsMTR.transactionNumber - 1));
	}

	/**
	 * Yields the path for the given file name inside the directory for the current transaction.
	 * 
	 * @param fileName the name of the file
	 * @return the path
	 */
	private Path getCurrentPathFor(Path fileName) {
		return root.resolve("b" + currentBlock).resolve("t" + currentTransaction).resolve(fileName);
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
	 * Ensures the given directory, if it exists.
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