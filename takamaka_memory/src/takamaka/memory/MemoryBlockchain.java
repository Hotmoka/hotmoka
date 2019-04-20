package takamaka.memory;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.FieldSignature;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.TransactionRequest;
import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
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
import takamaka.lang.Event;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 */
public class MemoryBlockchain extends AbstractBlockchain {

	/**
	 * The name used for the instrumented jars stored in the chain.
	 */
	public final static Path INSTRUMENTED_JAR_NAME = Paths.get("instrumented.jar");

	/**
	 * The name used for the file describing the dependencies of a jar.
	 */
	public final static Path DEPENDENCIES_NAME = Paths.get("deps.txt");

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
	 * The name used for the file describing the specification of a transaction.
	 */
	public final static Path SPEC_NAME = Paths.get("specification.txt");

	/**
	 * The name used for the file containing the updates performed by a transaction.
	 */
	public final static Path UPDATES_NAME = Paths.get("updates.txt");

	/**
	 * The name used for the file containing the events triggered during a transaction.
	 */
	public final static Path EVENTS_NAME = Paths.get("events.txt");

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
	protected TransactionRequest getRequestAt(TransactionReference reference) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected TransactionResponse getResponseAt(TransactionReference reference) throws Exception {
		// TODO Auto-generated method stub
		return null;
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
	protected void addGameteCreationTransactionInternal(Classpath takamakaBase, BigInteger initialAmount, StorageReference gamete, SortedSet<Update> updates) throws Exception {
		String spec = "Gamete creation transaction\n";
		spec += "Classpath: " + takamakaBase.toString() + "\n";
		spec += "Initial amount: " + initialAmount + "\n";
		spec += "Gamete: " + gamete + "\n";

		dumpTransactionSpec(spec);
		dumpTransactionUpdates(updates);
	}

	@Override
	protected void addJarStoreTransactionInternal(StorageReference caller, Classpath classpath, String jarName, Path jar, Path instrumented, SortedSet<Update> updates, BigInteger gas, BigInteger consumedGas, Classpath... dependencies) throws Exception {
		String spec;
		if (caller != null)
			spec = "Jar installation\n";
		else
			spec = "Initial jar installation\n";

		if (caller != null)
			spec += "Caller: " + caller + "\n";

		if (classpath != null)
			spec += "Class path: " + classpath + "\n";

		spec += "Dependencies: " + Arrays.toString(dependencies) + "\n";

		if (caller != null) {
			spec += "Gas provided: " + gas + "\n";
			spec += "Gas consumed: " + consumedGas + "\n";
		}

		dumpTransactionSpec(spec);
		dumpJarDependencies(dependencies);
		dumpTransactionUpdates(updates);

		Path original = getCurrentPathFor(Paths.get(jarName));
		Path dir = original.getParent();
		Files.createDirectories(dir);
		Files.copy(jar, original);
		Files.copy(instrumented, dir.resolve(INSTRUMENTED_JAR_NAME));
	}

	@Override
	protected void addConstructorCallTransactionInternal(CodeExecutor executor) throws Exception {
		String spec = "Constructor execution\n";
		spec += "Caller: " + executor.caller + "\n";
		spec += "Class path: " + executor.classpath + "\n";
		spec += "Constructor: " + executor.methodOrConstructor + "\n";
		spec += "Actuals: " + Arrays.toString(executor.getActuals()) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Constructed object: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas provided: " + executor.gas + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	@Override
	protected void addInstanceMethodCallTransactionInternal(CodeExecutor executor) throws Exception {
		String spec = "Instance method execution\n";
		spec += "Caller: " + executor.caller + "\n";
		spec += "Class path: " + executor.classpath + "\n";
		spec += "Method: " + executor.methodOrConstructor + "\n";
		spec += "Receiver: " + executor.receiver + "\n";
		spec += "Actuals: " + Arrays.toString(executor.getActuals()) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Result: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas provided: " + executor.gas + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	@Override
	protected void addStaticMethodCallTransactionInternal(CodeExecutor executor) throws Exception {
		String spec = "Static method execution\n";
		spec += "Caller: " + executor.caller + "\n";
		spec += "Class path: " + executor.classpath + "\n";
		spec += "Method: " + executor.methodOrConstructor + "\n";
		spec += "Actuals: " + Arrays.toString(executor.getActuals()) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Result: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas provided: " + executor.gas + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	@Override
	protected void extractPathsRecursively(Classpath classpath, List<Path> paths) throws Exception {
		if (classpath.recursive) {
			Path path = getPathFor((MemoryTransactionReference) classpath.transaction, DEPENDENCIES_NAME);
			if (Files.exists(path))
				for (String line: Files.readAllLines(path))
					extractPathsRecursively(classpathFromString(line), paths);
		}
	
		Path path = getPathFor((MemoryTransactionReference) classpath.transaction, INSTRUMENTED_JAR_NAME);
		if (!Files.exists(path))
			throw new IOException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");
	
		paths.add(path);
	}

	@Override
	protected void stepToNextTransactionReference() {
		if (++currentTransaction == transactionsPerBlock) {
			currentTransaction = 0;
			currentBlock = currentBlock.add(BigInteger.ONE);
		}
	}

	@Override
	protected BlockchainClassLoader mkBlockchainClassLoader(Classpath classpath) throws Exception {
		return new MemoryBlockchainClassLoader(classpath);
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
	 * Dumps the data for a code execution transaction.
	 * 
	 * @param spec the specification of the transaction
	 * @param updates the updates induced by the transaction
	 * @param events the events generated during the transaction
	 * @throws IOException if an error occurred while accessing the disk
	 */
	private void addCodeExecutionTransactionInternal(String spec, SortedSet<Update> updates, List<Event> events) throws IOException {
		dumpTransactionSpec(spec);
		dumpTransactionUpdates(updates);
		dumpTransactionEvents(events);
	}

	/**
	 * Dumps the dependencies of a jar installed in blockchain.
	 * 
	 * @param dependencies the dependencies
	 * @throws IOException if an error occurred while accessing the disk
	 */
	private void dumpJarDependencies(Classpath... dependencies) throws IOException {
		if (dependencies.length > 0) {
			Path dependenciesPath = getCurrentPathFor(DEPENDENCIES_NAME);
			try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(dependenciesPath.toFile())))) {
				for (Classpath dependency: dependencies)
					output.println(classpathAsString(dependency));
			}
		}
	}

	/**
	 * Dumps the events occurred during a transaction.
	 * 
	 * @param events the events
	 * @throws IOException if an error occurred while accessing the disk
	 */	
	private void dumpTransactionEvents(List<Event> events) throws IOException {
		Path eventsPath = getCurrentPathFor(EVENTS_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(eventsPath.toFile())))) {
			events.forEach(output::println);
		}
	}

	/**
	 * Dumps the updates induced by a transaction.
	 * 
	 * @param updates the updates
	 * @throws IOException if an error occurred while accessing the disk
	 */
	private void dumpTransactionUpdates(SortedSet<Update> updates) throws IOException {
		Path updatesPath = getCurrentPathFor(UPDATES_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(updatesPath.toFile())))) {
			updates.forEach(update -> output.println(updateAsString(update)));
		}
	}

	/**
	 * Dumps the specification of a transaction.
	 * 
	 * @param spec the specification
	 * @throws IOException if an error occurred while accessing the disk
	 */
	private void dumpTransactionSpec(String spec) throws IOException {
		Path specPath = getCurrentPathFor(SPEC_NAME);
		ensureDeleted(specPath.getParent());
		Files.createDirectories(specPath.getParent());

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(specPath.toFile())))) {
			output.print(spec);
		}
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
	 * Yields a string used to dump the given class path.
	 * 
	 * @param classpath the class path
	 * @return the string
	 */
	private static String classpathAsString(Classpath classpath) {
		return String.format("%s;%b", transactionReferenceAsString((MemoryTransactionReference) classpath.transaction), classpath.recursive);
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
	 * Builds a class path from its string dump.
	 * 
	 * @param s the string dump
	 * @return the class path
	 */
	private static Classpath classpathFromString(String s) {
		int semicolonPos = s.indexOf(';');
		if (semicolonPos < 0)
			throw new IllegalArgumentException("Illegal Classpath format: " + s);

		String transactionPart = s.substring(0, semicolonPos);
		String recursivePart = s.substring(semicolonPos + 1);

		return new Classpath(transactionReferenceFromString(transactionPart), Boolean.parseBoolean(recursivePart));
	}

	/**
	 * The class loader for a jar installed in blockchain.
	 * It resolves the jar from its class path, includes its dependencies and accesses the classes
	 * from the resulting resolved jars.
	 */
	private class MemoryBlockchainClassLoader extends URLClassLoader implements BlockchainClassLoader {

		/**
		 * Builds the class loader for the given class path and its dependencies.
		 * 
		 * @param classpath the class path
		 * @throws IOException if a disk access error occurs
		 */
		private MemoryBlockchainClassLoader(Classpath classpath) throws IOException {
			// we initially build it without URLs
			super(new URL[0], classpath.getClass().getClassLoader());

			// then we add the URLs corresponding to the class path and its dependencies, recursively
			addURLs(classpath);
		}

		private void addURLs(Classpath classpath) throws IOException {
			// if the class path is recursive, we consider its dependencies as well, recursively
			if (classpath.recursive) {
				Path path = getPathFor((MemoryTransactionReference) classpath.transaction, DEPENDENCIES_NAME);

				// a class path may have no dependencies
				if (Files.exists(path))
					for (String line: Files.readAllLines(path))
						addURLs(classpathFromString(line));
			}

			// we add, for class loading, the jar containing the instrumented code
			Path path = getPathFor((MemoryTransactionReference) classpath.transaction, INSTRUMENTED_JAR_NAME);
			if (!Files.exists(path))
				throw new IOException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");
	
			addURL(path.toFile().toURI().toURL());
		}
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