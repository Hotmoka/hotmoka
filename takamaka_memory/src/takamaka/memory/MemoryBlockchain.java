package takamaka.memory;

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
import java.util.Optional;
import java.util.Set;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.DeserializationError;
import takamaka.blockchain.FieldSignature;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.TransactionRequest;
import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
import takamaka.blockchain.UpdateOfField;
import takamaka.blockchain.response.AbstractTransactionResponseWithUpdates;
import takamaka.blockchain.values.StorageReferenceAlreadyInBlockchain;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 */
public class MemoryBlockchain extends AbstractBlockchain {

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
	 * The transaction reference after which the current transaction is being executed.
	 * This is {@code null} for the first transaction.
	 */
	private MemoryTransactionReference previous;

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
	protected void initTransaction(BigInteger gas, TransactionReference previous) throws Exception {
		super.initTransaction(gas, previous);
		this.previous = (MemoryTransactionReference) previous;

		// we access the block header where the transaction would occur
		if (previous != null) {
			MemoryTransactionReference next = this.previous.getNext();
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
	protected MemoryTransactionReference getTopmostTransactionReference() {
		return topmost;
	}

	@Override
	protected TransactionReference getTransactionReferenceFor(String toString) {
		return new MemoryTransactionReference(toString);
	}

	@Override
	protected TransactionReference expandBlockchainWith(TransactionRequest request, TransactionResponse response) throws Exception {
		MemoryTransactionReference next = topmost == null ? new MemoryTransactionReference(BigInteger.ZERO, (short) 0) : topmost.getNext();
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

	@Override
	protected void collectEagerUpdatesFor(StorageReferenceAlreadyInBlockchain object, Set<Update> updates, int eagerFields) throws Exception {
		// goes back from the transaction that precedes that being executed;
		// there is no reason to look before the transaction that created the object;
		// moreover, there is no reason to look beyond the total number of fields
		// whose update was expected to be found
		for (MemoryTransactionReference cursor = previous; updates.size() < eagerFields && !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious())
			// adds the eager updates from the cursor, if any and if they are the latest
			addEagerUpdatesFor(object, cursor, updates);
	}

	/**
	 * Adds, to the given set, the updates of eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the transaction
	 * @param updates the set where they must be added
	 * @throws IOException if there is an error while accessing the disk
	 */
	private void addEagerUpdatesFor(StorageReferenceAlreadyInBlockchain object, TransactionReference transaction, Set<Update> updates) throws Exception {
		TransactionResponse response = getResponseAt(transaction);
		if (response instanceof AbstractTransactionResponseWithUpdates) {
			((AbstractTransactionResponseWithUpdates) response).getUpdates()
				.map(update -> update.contextualizeAt(transaction))
				.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && update.isEager() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
		}
	}

	/**
	 * Determines if the given set of updates contains an update for the
	 * same object and field as the given update.
	 * 
	 * @param update the given update
	 * @param updates the set
	 * @return true if and only if that condition holds
	 */
	private static boolean isAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().anyMatch(update::isForSamePropertyAs);
	}

	@Override
	protected UpdateOfField getLastLazyUpdateFor(StorageReferenceAlreadyInBlockchain object, FieldSignature field) throws Exception {
		// goes back from the previous transaction;
		// there is no reason to look before the transaction that created the object
		for (MemoryTransactionReference cursor = previous; !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious()) {
			UpdateOfField update = getLastUpdateFor(object, field, cursor);
			if (update != null)
				return update;
		}

		throw new DeserializationError("Did not find the last update for " + field + " of " + object);
	}

	@Override
	protected UpdateOfField getLastLazyUpdateForFinal(StorageReferenceAlreadyInBlockchain object, FieldSignature field) throws Exception {
		// goes directly to the transaction that created the object
		UpdateOfField update = getLastUpdateFor(object, field, object.transaction);
		if (update != null)
			return update;

		throw new DeserializationError("Did not find the last update for " + field + " of " + object);
	}

	/**
	 * Yields the update to the given field of the object at the given reference,
	 * generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param transaction the block where the update is being looked for
	 * @return the update, if any. If the field of {@code reference} was not modified during
	 *         the {@code transaction}, this method returns {@code null}
	 */
	private UpdateOfField getLastUpdateFor(StorageReferenceAlreadyInBlockchain object, FieldSignature field, TransactionReference transaction) throws Exception {
		TransactionResponse response = getResponseAt(transaction);
		if (response instanceof AbstractTransactionResponseWithUpdates) {
			Optional<UpdateOfField> result = ((AbstractTransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> update.contextualizeAt(transaction))
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findAny();
		
			if (result.isPresent())
				return result.get();
		}

		return null;
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