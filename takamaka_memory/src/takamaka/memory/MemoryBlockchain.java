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
import takamaka.blockchain.FieldReference;
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
	public takamaka.blockchain.TransactionReference mkTransactionReferenceFrom(String s) {
		return new MemoryTransactionReference(s);
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
	protected Update getLastLazyUpdateFor(StorageReference reference, FieldReference field) throws Exception {
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

	private Update getLastUpdateFor(StorageReference reference, FieldReference field, MemoryTransactionReference cursor) throws IOException {
		Path updatesPath = getPathFor((MemoryTransactionReference) reference.transaction, UPDATES_NAME);
		if (Files.exists(updatesPath)) {
			Optional<Update> result = Files.lines(updatesPath)
				.map(line -> Update.mkFromString(this, line))
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
				.map(line -> Update.mkFromString(this, line))
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
		spec += "Classpath: " + takamakaBase + "\n";
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
			Path path = getPathFor((MemoryTransactionReference) classpath.transaction, DEPENDENCIES_NAME);
			if (Files.exists(path))
				for (String line: Files.readAllLines(path))
					extractPathsRecursively(new Classpath(this, line), result);
		}

		Path path = getPathFor((MemoryTransactionReference) classpath.transaction, INSTRUMENTED_JAR_NAME);
		if (!Files.exists(path))
			throw new IOException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");

		result.add(path);
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
						addURLs(new Classpath(MemoryBlockchain.this, line));
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