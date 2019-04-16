package takamaka.memory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.FieldSignature;
import takamaka.blockchain.TransactionReference;
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
	 * The name used for the file describing the specification of a code execution transaction.
	 */
	public final static Path SPEC_NAME = Paths.get("specification.txt");

	/**
	 * The name used for the file containing the updates performed during a code execution transaction.
	 */
	public final static Path UPDATES_NAME = Paths.get("updates.txt");

	/**
	 * The name used for the file containing the events triggered during a code execution transaction.
	 */
	public final static Path EVENTS_NAME = Paths.get("events.txt");

	private final Path root;
	private final short transactionsPerBlock;
	private BigInteger currentBlock = BigInteger.ZERO;
	private short currentTransaction;

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
	protected void collectEagerUpdatesFor(StorageReference reference, Set<Update> where) throws Exception {
		MemoryTransactionReference cursor = getCurrentTransactionReference();

		do {
			cursor = previousTransaction(cursor);
			addPrimitiveUpdatesFor(reference, cursor, where);
		}
		while (!cursor.equals(reference.transaction));
	}

	@Override
	protected Update getLastLazyUpdateFor(StorageReference reference, FieldSignature field) throws Exception {
		MemoryTransactionReference cursor = (MemoryTransactionReference) reference.transaction;

		do {
			Update update = getLastUpdateFor(reference, field, cursor);
			if (update != null)
				return update;

			cursor = previousTransaction(cursor);
		}
		while (!cursor.isOlderThan(reference.transaction));

		throw new IllegalStateException("Did not find the last update for " + field + " of " + reference);
	}

	private Update getLastUpdateFor(StorageReference reference, FieldSignature field, MemoryTransactionReference cursor) throws IOException {
		Path updatesPath = getPathFor((MemoryTransactionReference) reference.transaction, UPDATES_NAME);
		if (Files.exists(updatesPath)) {
			Optional<Update> result = Files.lines(updatesPath)
				.map(MemoryBlockchain::updateFromString)
				.filter(update -> update.object.equals(reference) && update.field.equals(field))
				.findAny();

			if (result.isPresent())
				return result.get();
		}

		return null;
	}

	private void addPrimitiveUpdatesFor(StorageReference object, MemoryTransactionReference where, Set<Update> updates) throws IOException {
		Path updatesPath = getPathFor(where, UPDATES_NAME);
		if (Files.exists(updatesPath))
			Files.lines(updatesPath)
				.map(MemoryBlockchain::updateFromString)
				.filter(update -> update.object.equals(object) && !update.field.type.isLazilyLoaded() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	private static boolean isAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().anyMatch(other -> other.object.equals(update.object) && other.field.equals(update.field));
	}

	private MemoryTransactionReference previousTransaction(MemoryTransactionReference cursor) throws IllegalArgumentException {
		if (cursor.transactionNumber == 0)
			if (cursor.blockNumber.signum() == 0)
				throw new IllegalArgumentException("Transaction has no previous transaction");
			else
				return new MemoryTransactionReference(cursor.blockNumber.subtract(BigInteger.ONE), (short) (transactionsPerBlock - 1));
		else
			return new MemoryTransactionReference(cursor.blockNumber, (short) (cursor.transactionNumber - 1));
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
	protected void addJarStoreTransactionInternal(StorageReference caller, Classpath classpath, Path jar, Path instrumented, SortedSet<Update> updates, BigInteger gas, BigInteger consumedGas, Classpath... dependencies) throws Exception {
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

		Path original = getCurrentPathFor(jar.getFileName());
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

	private void addCodeExecutionTransactionInternal(String spec, SortedSet<Update> updates, List<Event> events) throws IOException {
		dumpTransactionSpec(spec);
		dumpTransactionUpdates(updates);
		dumpTransactionEvents(events);
	}

	private void dumpJarDependencies(Classpath... dependencies) throws IOException {
		if (dependencies.length > 0) {
			Path dependenciesPath = getCurrentPathFor(DEPENDENCIES_NAME);
			try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(dependenciesPath.toFile())))) {
				for (Classpath dependency: dependencies)
					output.println(classpathAsString(dependency));
			}
		}
	}

	private void dumpTransactionEvents(List<Event> events) throws IOException {
		Path eventsPath = getCurrentPathFor(EVENTS_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(eventsPath.toFile())))) {
			events.forEach(output::println);
		}
	}

	private void dumpTransactionUpdates(SortedSet<Update> updates) throws IOException {
		Path updatesPath = getCurrentPathFor(UPDATES_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(updatesPath.toFile())))) {
			updates.forEach(update -> output.println(updateAsString(update)));
		}
	}

	private void dumpTransactionSpec(String spec) throws IOException {
		Path specPath = getCurrentPathFor(SPEC_NAME);
		ensureDeleted(specPath.getParent());
		Files.createDirectories(specPath.getParent());

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(specPath.toFile())))) {
			output.print(spec);
		}
	}

	@Override
	protected void extractPathsRecursively(Classpath classpath, List<Path> result) throws Exception {
		if (classpath.recursive) {
			Path path = getPathFor((MemoryTransactionReference) classpath.transaction, DEPENDENCIES_NAME);
			if (Files.exists(path))
				for (String line: Files.readAllLines(path))
					extractPathsRecursively(classpathFromString(line), result);
		}

		Path path = getPathFor((MemoryTransactionReference) classpath.transaction, INSTRUMENTED_JAR_NAME);
		if (!Files.exists(path))
			throw new IOException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");

		result.add(path);
	}

	private static String transactionReferenceAsString(MemoryTransactionReference ref) {
		return String.format("%s.%x", ref.blockNumber.toString(16), ref.transactionNumber);
	}

	private static String storageReferenceAsString(StorageReference ref) {
		return String.format("%s#%s", transactionReferenceAsString((MemoryTransactionReference) ref.transaction), ref.progressive.toString(16));
	}

	private static String updateAsString(Update update) {
		return storageReferenceAsString(update.object) + "&" + update.field.definingClass + "&" + update.field.name + "&" + update.field.type + "&" + storageValueAsString(update.value);
	}

	private static String storageValueAsString(StorageValue value) {
		if (value instanceof StorageReference)
			return storageReferenceAsString((StorageReference) value);
		else
			return value.toString();
	}

	/**
	 * Serializes a class path into a string.
	 * 
	 * @param classpath the class path
	 * @return the resulting string
	 */
	private static String classpathAsString(Classpath classpath) {
		return String.format("%s;%b", transactionReferenceAsString((MemoryTransactionReference) classpath.transaction), classpath.recursive);
	}

	private static StorageReference storageReferenceFromString(String s) {
		int index;
	
		if (s == null || (index = s.indexOf('#')) < 0)
			throw new NumberFormatException("Illegal transaction reference format: " + s);
	
		String transactionPart = s.substring(0, index);
		String progressivePart = s.substring(index + 1);
		
		return new StorageReference(transactionReferenceFromString(transactionPart), new BigInteger(progressivePart, 16));
	}

	private static TransactionReference transactionReferenceFromString(String s) {
		int dollarPos;
		if (s == null || (dollarPos = s.indexOf('.')) < 0)
			throw new NumberFormatException("Illegal transaction reference format: " + s);
	
		String blockPart = s.substring(0, dollarPos);
		String transactionPart = s.substring(dollarPos + 1);
		
		return new MemoryTransactionReference(new BigInteger(blockPart, 16), Short.decode("0x" + transactionPart));
	}

	/**
	 * Builds an update from its string representation. It must hold that
	 * {@code update.equals(Update.mkFromString(blockchain, update.toString()))}.
	 * 
	 * @param s the string representation of the update
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
	 * Yields a storage value of a given type, from its string representation.
	 * 
	 * @param type the type of the value
	 * @param s the string representation of the value
	 * @return the value
	 * @throws IllegalArgumentException if booleans or characters cannot be converted or if an unexpected type is provided
	 * @throws NumberFormatException if numerical values cannot be converted
	 */
	private static StorageValue storageValueFromString(StorageType type, String s) {
		if (s == null)
			throw new IllegalArgumentException("The string to convert cannot be null");

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
	 * Deserializes a class path from a string. It is the inverse of
	 * {@link takamaka.memory.MemoryBlockchain#classpathAsString(Classpath)}.
	 * 
	 * @param s the string
	 * @return the resulting class path
	 */
	private static Classpath classpathFromString(String s) {
		int semicolonPos;
		if (s == null || (semicolonPos = s.indexOf(';')) < 0)
			throw new IllegalArgumentException("Illegal Classpath format: " + s);

		String transactionPart = s.substring(0, semicolonPos);
		String recursivePart = s.substring(semicolonPos + 1);

		return new Classpath(transactionReferenceFromString(transactionPart), Boolean.parseBoolean(recursivePart));
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

	private class MemoryBlockchainClassLoader extends URLClassLoader implements BlockchainClassLoader {
		private MemoryBlockchainClassLoader(Classpath classpath) throws Exception {
			super(new URL[0], classpath.getClass().getClassLoader());
			addURLs(classpath);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			return super.loadClass(name);
		}

		private void addURLs(Classpath classpath) throws Exception {
			if (classpath.recursive) {
				Path path = getPathFor((MemoryTransactionReference) classpath.transaction, DEPENDENCIES_NAME);
				if (Files.exists(path))
					for (String line: Files.readAllLines(path))
						addURLs(classpathFromString(line));
			}
	
			Path path = getPathFor((MemoryTransactionReference) classpath.transaction, INSTRUMENTED_JAR_NAME);
			if (!Files.exists(path))
				throw new IOException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");
	
			addURL(path.toFile().toURI().toURL());
		}
	}

	private Path getCurrentPathFor(Path fileName) {
		return Paths.get(root.toString(), "b" + currentBlock, "t" + currentTransaction).resolve(fileName);
	}

	private Path getPathFor(MemoryTransactionReference ref, Path fileName) {
		return Paths.get(root.toString(), "b" + ref.blockNumber, "t" + ref.transactionNumber).resolve(fileName);
	}

	private static void ensureDeleted(Path root) throws IOException {
		if (Files.exists(root))
			Files.walk(root)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}
}