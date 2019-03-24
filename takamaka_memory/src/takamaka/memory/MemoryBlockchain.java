package takamaka.memory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
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
import takamaka.blockchain.ConstructorReference;
import takamaka.blockchain.FieldReference;
import takamaka.blockchain.MethodReference;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

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
	protected void collectUpdatesFor(StorageReference reference, Set<Update> where) throws TransactionException {
		try {
			TransactionReference cursor = getCurrentTransactionReference();

			do {
				cursor = previousTransaction(cursor);
				addPrimitiveUpdatesFor(reference, cursor, where);
			}
			while (!cursor.equals(reference.transaction));
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot deserialize storage reference " + reference, t);
		}
	}

	@Override
	protected Update getLastUpdateFor(StorageReference reference, FieldReference field) throws TransactionException {
		try {
			TransactionReference cursor = reference.transaction;

			do {
				Update update = getLastUpdateFor(reference, field, cursor);
				if (update != null)
					return update;

				cursor = previousTransaction(cursor);
			}
			while (!cursor.isOlderThan(reference.transaction));
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot deserialize storage reference " + reference, t);
		}

		throw new TransactionException("Did not find the last update for " + field + " of " + reference);
	}

	private Update getLastUpdateFor(StorageReference reference, FieldReference field, TransactionReference cursor) throws IOException {
		Path updatesPath = getPathFor(reference.transaction, UPDATES_NAME);
		if (Files.exists(updatesPath)) {
			Optional<Update> result = Files.lines(updatesPath)
				.map(Update::mkFromString)
				.filter(update -> update.object.equals(reference) && update.field.equals(field))
				.findAny();

			if (result.isPresent())
				return result.get();
		}

		return null;
	}

	private void addPrimitiveUpdatesFor(StorageReference object, TransactionReference where, Set<Update> updates) throws IOException {
		Path updatesPath = getPathFor(where, UPDATES_NAME);
		if (Files.exists(updatesPath))
			Files.lines(updatesPath)
				.map(Update::mkFromString)
				.filter(update -> update.object.equals(object) && !update.field.type.isLazilyLoaded() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	private static boolean isAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().anyMatch(other -> other.object.equals(update.object) && other.field.equals(update.field));
	}

	private TransactionReference previousTransaction(TransactionReference cursor) throws IllegalArgumentException {
		if (cursor.transactionNumber == 0)
			if (cursor.blockNumber == 0)
				throw new IllegalArgumentException("Transaction has no previous transaction");
			else
				return new TransactionReference(cursor.blockNumber - 1, (short) (transactionsPerBlock - 1));
		else
			return new TransactionReference(cursor.blockNumber, (short) (cursor.transactionNumber - 1));
	}

	@Override
	protected void addGameteCreationTransactionInternal(Classpath takamakaBase, BigInteger initialAmount, StorageReference gamete, SortedSet<Update> updates) throws Exception {
		String spec = "Gamete creation transaction\n";
		spec += "Classpath: " + takamakaBase + "\n";
		spec += "Initial amount: " + initialAmount + "\n";
		spec += "Gamete: " + gamete + "\n";
		
		dumpTransactionSpec(spec);
		dumpTransactionUpdates(updates);
	}

	@Override
	protected void addJarStoreTransactionInternal(StorageReference caller, Classpath classpath, Path jar, Path instrumented, SortedSet<Update> updates, long consumedGas, Classpath... dependencies) throws Exception {
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
		if (caller != null)
			spec += "Gas consumed: " + consumedGas + "\n";

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
	protected void addConstructorCallTransactionInternal
		(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue[] actuals, CodeExecutor executor)
		throws Exception {

		String spec = "Constructor execution\n";
		spec += "Caller: " + caller + "\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Constructor: " + constructor + "\n";
		spec += "Actuals: " + Arrays.toString(actuals) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Constructed object: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	@Override
	protected void addEntryConstructorCallTransactionInternal(StorageReference caller, Classpath classpath, ConstructorReference constructor,
			StorageValue[] actuals, CodeExecutor executor) throws Exception {
		
		String spec = "@Entry Constructor execution\n";
		spec += "Caller: " + caller + "\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Constructor: " + constructor + "\n";
		spec += "Actuals: " + Arrays.toString(actuals) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Constructed object: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	@Override
	protected void addInstanceMethodCallTransactionInternal(StorageReference caller, Classpath classpath, MethodReference method,
			StorageValue receiver, StorageValue[] actuals, CodeExecutor executor) throws Exception {

		String spec = "Instance method execution\n";
		spec += "Caller: " + caller + "\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Method: " + method + "\n";
		spec += "Receiver: " + receiver + "\n";
		spec += "Actuals: " + Arrays.toString(actuals) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Result: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	@Override
	protected void addEntryInstanceMethodCallTransactionInternal(StorageReference caller, Classpath classpath, MethodReference method,
			StorageValue receiver, StorageValue[] actuals, CodeExecutor executor) throws Exception {

		String spec = "@Entry instance method execution\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Method: " + method + "\n";
		spec += "Caller: " + caller + "\n";
		spec += "Receiver: " + receiver + "\n";
		spec += "Actuals: " + Arrays.toString(actuals) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Result: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());		
	}

	@Override
	protected void addStaticMethodCallTransactionInternal(StorageReference caller, Classpath classpath, MethodReference method, StorageValue[] actuals, CodeExecutor executor) throws Exception {
		String spec = "Static method execution\n";
		spec += "Caller: " + caller + "\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Method: " + method + "\n";
		spec += "Actuals: " + Arrays.toString(actuals) + "\n";
		StorageValue result = executor.getResult();
		if (result != null)
			spec += "Result: " + result + "\n";
		else
			spec += "Exception: " + executor.getException().getClass().getName() + "\n";
		spec += "Gas consumed: " + executor.gasConsumed() + "\n";

		addCodeExecutionTransactionInternal(spec, executor.updates(), executor.events());
	}

	private void addCodeExecutionTransactionInternal(String spec, SortedSet<Update> updates, List<String> events) throws IOException {
		dumpTransactionSpec(spec);
		dumpTransactionUpdates(updates);
		dumpTransactionEvents(events);
	}

	private void dumpJarDependencies(Classpath... dependencies) throws IOException {
		if (dependencies.length > 0) {
			Path dependenciesPath = getCurrentPathFor(DEPENDENCIES_NAME);
			try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(dependenciesPath.toFile())))) {
				for (Classpath dependency: dependencies)
					output.println(dependency);
			}
		}
	}

	private void dumpTransactionEvents(List<String> events) throws IOException {
		Path eventsPath = getCurrentPathFor(EVENTS_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(eventsPath.toFile())))) {
			events.forEach(output::println);
		}
	}

	private void dumpTransactionUpdates(SortedSet<Update> updates) throws IOException {
		Path updatesPath = getCurrentPathFor(UPDATES_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(updatesPath.toFile())))) {
			updates.forEach(output::println);
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
	protected void extractPathsRecursively(Classpath classpath, List<Path> result) throws IOException {
		if (classpath.recursive) {
			Path path = getPathFor(classpath.transaction, DEPENDENCIES_NAME);
			if (Files.exists(path))
				for (String line: Files.readAllLines(path))
					extractPathsRecursively(new Classpath(line), result);
		}

		Path path = getPathFor(classpath.transaction, INSTRUMENTED_JAR_NAME);
		if (!Files.exists(path))
			throw new IOException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");

		result.add(path);
	}

	@Override
	protected boolean blockchainIsFull() {
		return currentBlock == Long.MAX_VALUE && currentTransaction == transactionsPerBlock;
	}

	@Override
	protected void commitCurrentTransaction() {
		if (++currentTransaction == transactionsPerBlock) {
			currentTransaction = 0;
			currentBlock++;
		}
	}

	@Override
	protected BlockchainClassLoader mkBlockchainClassLoader(Classpath classpath) throws TransactionException {
		return new MemoryBlockchainClassLoader(classpath);
	}

	private class MemoryBlockchainClassLoader extends URLClassLoader implements BlockchainClassLoader {
		private MemoryBlockchainClassLoader(Classpath classpath) throws TransactionException {
			super(new URL[0], classpath.getClass().getClassLoader());
			addURLs(classpath);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			return super.loadClass(name);
		}

		private void addURLs(Classpath classpath) throws TransactionException {
			if (classpath.recursive) {
				Path path = getPathFor(classpath.transaction, DEPENDENCIES_NAME);
				if (Files.exists(path))
					try {
						for (String line: Files.readAllLines(path))
							addURLs(new Classpath(line));
					}
					catch (IOException e) {
						throw new TransactionException("Cannot read dependencies from " + path);
					}
			}
	
			Path path = getPathFor(classpath.transaction, INSTRUMENTED_JAR_NAME);
			if (!Files.exists(path))
				throw new TransactionException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");
	
			try {
				addURL(path.toFile().toURI().toURL());
			}
			catch (MalformedURLException e) {
				throw new TransactionException("Found illegal classpath: " + e.getMessage());
			}
		}
	}

	private Path getCurrentPathFor(Path fileName) {
		return Paths.get(root.toString(), "b" + currentBlock, "t" + currentTransaction).resolve(fileName);
	}

	private Path getPathFor(TransactionReference ref, Path fileName) {
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