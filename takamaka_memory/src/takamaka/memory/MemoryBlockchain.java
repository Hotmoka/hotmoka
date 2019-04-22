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
import java.util.Optional;
import java.util.Set;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.FieldSignature;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.TransactionRequest;
import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
import takamaka.blockchain.response.AbstractTransactionResponseWithUpdates;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.types.StorageType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.ByteValue;
import takamaka.blockchain.values.CharValue;
import takamaka.blockchain.values.DoubleValue;
import takamaka.blockchain.values.FloatValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.NullValue;
import takamaka.blockchain.values.ShortValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.blockchain.values.StringValue;

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
	 * The name used for the file containing the updates performed by a transaction.
	 */
	public final static Path UPDATES_NAME = Paths.get("updates.txt");

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

		// TODO: remove at the end
		if (response instanceof AbstractTransactionResponseWithUpdates) {
			Path updatesPath = getCurrentPathFor(UPDATES_NAME);

			try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(updatesPath.toFile())))) {
				for (Update update: ((AbstractTransactionResponseWithUpdates) response).getUpdates().toArray(Update[]::new))
					output.println(updateAsString(update));
			}
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
	protected void collectEagerUpdatesFor(StorageReference object, Set<Update> where) throws Exception {
		// goes back from the current transaction
		MemoryTransactionReference cursor = getCurrentTransactionReference();

		do {
			cursor = previousTransaction(cursor);
			// adds the eager updates from the cursor, if any and if they are the latest
			addEagerUpdatesFor(object, cursor, where);
		}
		// no reason to look before the transaction that created the object
		while (!cursor.equals(object.transaction));
	}

	@Override
	protected Update getLastLazyUpdateFor(StorageReference object, FieldSignature field) throws Exception {
		// goes back from the current transaction
		MemoryTransactionReference cursor = getCurrentTransactionReference();

		do {
			cursor = previousTransaction(cursor);
			Update update = getLastUpdateFor(object, field, cursor);
			if (update != null)
				return update;
		}
		// no reason to look before the transaction that created the object
		while (!cursor.equals(object.transaction));

		throw new IllegalStateException("Did not find the last update for " + field + " of " + object);
	}

	@Override
	protected void stepToNextTransactionReference() {
		if (++currentTransaction == transactionsPerBlock) {
			currentTransaction = 0;
			currentBlock = currentBlock.add(BigInteger.ONE);
		}
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
	 * @throws IOException if there is an error while accessing the disk
	 */
	private Update getLastUpdateFor(StorageReference object, FieldSignature field, MemoryTransactionReference transaction) throws IOException {
		Path updatesPath = getPathFor(transaction, UPDATES_NAME);
		if (Files.exists(updatesPath)) {
			Optional<Update> result = Files.lines(updatesPath)
				.map(MemoryBlockchain::updateFromString)
				.filter(update -> update.object.equals(object) && update.field.equals(field))
				.findAny();
	
			if (result.isPresent())
				return result.get();
		}
	
		return null;
	}

	/**
	 * Adds, to the given set, the updates of eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param where the transaction
	 * @param updates the set where they must be added
	 * @throws IOException if there is an error while accessing the disk
	 */
	private void addEagerUpdatesFor(StorageReference object, MemoryTransactionReference where, Set<Update> updates) throws IOException {
		Path updatesPath = getPathFor(where, UPDATES_NAME);
		if (Files.exists(updatesPath))
			Files.lines(updatesPath)
				.map(MemoryBlockchain::updateFromString)
				.filter(update -> update.object.equals(object) && !update.field.type.isLazy() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	/**
	 * Determines if the given set of updates contains an update for the
	 * same object and field of the given update.
	 * 
	 * @param update the given update
	 * @param updates the set
	 * @return true if and only if that condition holds
	 */
	private static boolean isAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().anyMatch(other -> other.object.equals(update.object) && other.field.equals(update.field));
	}

	/**
	 * Yields the previous transaction of the given one.
	 * 
	 * @param transaction the transaction whose previous transaction is being looked for
	 * @return the previous transaction of {@code transaction}, if any
	 * @throws IllegalStateException if there is no previous transaction
	 */
	private MemoryTransactionReference previousTransaction(MemoryTransactionReference transaction) {
		if (transaction.transactionNumber == 0)
			if (transaction.blockNumber.signum() == 0)
				throw new IllegalStateException("Transaction has no previous transaction");
			else
				return new MemoryTransactionReference(transaction.blockNumber.subtract(BigInteger.ONE), (short) (transactionsPerBlock - 1));
		else
			return new MemoryTransactionReference(transaction.blockNumber, (short) (transaction.transactionNumber - 1));
	}

	/**
	 * Yields a string used to dump the given transaction reference.
	 * 
	 * @param reference the reference
	 * @return the string
	 */
	private static String transactionReferenceAsString(MemoryTransactionReference reference) {
		return String.format("%s.%x", reference.blockNumber.toString(16), reference.transactionNumber);
	}

	/**
	 * Yields a string used to dump the given storage reference.
	 * 
	 * @param reference the reference
	 * @return the string
	 */
	private static String storageReferenceAsString(StorageReference reference) {
		return String.format("%s#%s", transactionReferenceAsString((MemoryTransactionReference) reference.transaction), reference.progressive.toString(16));
	}

	/**
	 * Yields a string used to dump the given update.
	 * 
	 * @param update the update
	 * @return the string
	 */
	private static String updateAsString(Update update) {
		return storageReferenceAsString(update.object) + "&" + update.field.definingClass + "&" + update.field.name + "&" + update.field.type + "&" + storageValueAsString(update.value);
	}

	/**
	 * Yields a string used to dump the given storage value.
	 * 
	 * @param value the value
	 * @return the string
	 */
	private static String storageValueAsString(StorageValue value) {
		if (value instanceof StorageReference)
			return storageReferenceAsString((StorageReference) value);
		else
			return value.toString();
	}

	/**
	 * Builds a storage reference from its string dump.
	 * 
	 * @param s the string dump
	 * @return the storage reference
	 */
	private static StorageReference storageReferenceFromString(String s) {
		int index;
	
		if (s == null || (index = s.indexOf('#')) < 0)
			throw new NumberFormatException("Illegal transaction reference format: " + s);
	
		String transactionPart = s.substring(0, index);
		String progressivePart = s.substring(index + 1);
		
		return new StorageReference(transactionReferenceFromString(transactionPart), new BigInteger(progressivePart, 16));
	}

	/**
	 * Builds a transaction reference from its string dump.
	 * 
	 * @param s the string dump
	 * @return the transaction reference
	 */
	private static TransactionReference transactionReferenceFromString(String s) {
		int dollarPos;
		if (s == null || (dollarPos = s.indexOf('.')) < 0)
			throw new NumberFormatException("Illegal transaction reference format: " + s);
	
		String blockPart = s.substring(0, dollarPos);
		String transactionPart = s.substring(dollarPos + 1);
		
		return new MemoryTransactionReference(new BigInteger(blockPart, 16), Short.decode("0x" + transactionPart));
	}

	/**
	 * Builds an update from its string dump.
	 * 
	 * @param s the string dump
	 * @return the update
	 */
	private static Update updateFromString(String s) {
		String[] parts = s.split("&");
		if (parts.length != 5)
			throw new IllegalArgumentException("Illegal string format " + s);
	
		StorageType type = StorageType.of(parts[3]);
	
		return new Update(storageReferenceFromString(parts[0]),
			new FieldSignature(new ClassType(parts[1]), parts[2], type),
			storageValueFromString(type, parts[4]));
	}

	/**
	 * Builds a storage value from its string dump.
	 * 
	 * @param s the string dump
	 * @return the storage value
	 */
	private static StorageValue storageValueFromString(StorageType type, String s) {
		if (type instanceof BasicTypes) {
			switch ((BasicTypes) type) {
			case BOOLEAN:
				if (s.equals("true"))
					return new BooleanValue(true);
				else if (s.equals("false"))
					return new BooleanValue(false);
				else
					throw new IllegalArgumentException("The string to convert is not a boolean");
			case BYTE:
				return new ByteValue(Byte.parseByte(s));
			case CHAR:
				if (s.length() != 1)
					throw new IllegalArgumentException("The string to convert is not a character");
				else
					return new CharValue(s.charAt(0));
			case DOUBLE:
				return new DoubleValue(Double.parseDouble(s));
			case FLOAT:
				return new FloatValue(Float.parseFloat(s));
			case INT:
				return new IntValue(Integer.parseInt(s));
			case LONG:
				return new LongValue(Long.parseLong(s));
			case SHORT:
				return new ShortValue(Short.parseShort(s));
			default:
				throw new RuntimeException("Unexpected basic type " + type);
			}
		}
		else if (type instanceof ClassType) {
			if (s.equals("null"))
				return NullValue.INSTANCE;
			else if (type.equals(ClassType.STRING))
				return new StringValue(s);
			else if (type.equals(ClassType.BIG_INTEGER))
				return new BigIntegerValue(new BigInteger(s, 10));
			else
				return storageReferenceFromString(s);
		}

		throw new IllegalArgumentException("Unexpected type " + type);
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