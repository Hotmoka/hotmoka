package takamaka.memory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
import takamaka.translator.JarInstrumentation;
import takamaka.translator.Program;

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
	public final static Path SPEC_NAME = Paths.get("spec.txt");

	/**
	 * The name used for the file containing the updates performed during a code execution transaction.
	 */
	public final static Path UPDATES_NAME = Paths.get("updates.txt");

	private final Path root;
	private final short transactionsPerBlock;

	public MemoryBlockchain(Path root, short transactionsPerBlock) throws IOException {
		if (root == null)
			throw new NullPointerException("A root path must be specified");

		if (transactionsPerBlock <= 0)
			throw new IllegalArgumentException("transactionsPerBlock must be positive");

		// cleans the directory where the blockchain lives
		ensureDeleted(root);

		this.root = root;
		this.transactionsPerBlock = transactionsPerBlock;
	}

	@Override
	protected void collectUpdatesFor(StorageReference reference, Set<Update> where) throws TransactionException {
		try {
			TransactionReference cursor = reference.transaction;

			do {
				addPrimitiveUpdatesFor(reference, cursor, where);
				cursor = previousTransaction(cursor);
			}
			while (!cursor.isOlderThan(reference.transaction));
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot deserialize storage reference " + reference, t);
		}
	}

	private void addPrimitiveUpdatesFor(StorageReference object, TransactionReference where, Set<Update> updates) throws IOException {
		Path updatesPath = getPathFor(where, UPDATES_NAME);
		if (Files.exists(updatesPath))
			Files.lines(updatesPath)
				.map(Update::mkFromString)
				.filter(update -> update.object.equals(object) && !update.field.type.isLazilyLoaded() && notAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	private static boolean notAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().noneMatch(other -> other.object.equals(update.object) && other.field.equals(update.field));
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
	public Object deserializeLastUpdateFor(StorageReference reference, FieldReference field) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void addJarStoreTransactionInternal(Path jar, Classpath... dependencies) throws TransactionException {
		Path jarName = jar.getFileName();
		String jn = jarName.toString();
		if (!jn.endsWith(".jar"))
			throw new TransactionException("Jar file should end in .jar");

		if (jn.length() > 100)
			throw new TransactionException("Jar file name too long");

		Program program = mkProgram(jar, dependencies);

		Path original = getCurrentPathFor(jarName);
	
		try {
			ensureDeleted(original.getParent());
			Files.createDirectories(original.getParent());
		}
		catch (IOException e) {
			throw new TransactionException("Could not create transaction entry " + original.getParent());
		}
	
		// the original jar file is stored in this blockchain, but not used;
		// in a real blockchain, it is needed instead to reexecute and check the transaction
		try {
			Files.copy(jar, original);
		}
		catch (IOException e) {
			throw new TransactionException("Could not store jar into transaction entry " + original);
		}
	
		Path instrumented = getCurrentPathFor(INSTRUMENTED_JAR_NAME);
		try {
			new JarInstrumentation(jar, instrumented, program);
		}
		catch (IOException e) {
			throw new TransactionException("Could not store instrumented jar into transaction entry");
		}
	
		if (dependencies.length > 0) {
			Path dependenciesPath = getCurrentPathFor(DEPENDENCIES_NAME);
			try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(dependenciesPath.toFile())))) {
				for (Classpath dependency: dependencies)
					output.println(dependency);
			}
			catch (IOException e) {
				throw new TransactionException("Could not store dependencies into transaction entry " + original);
			}
		}
	}

	@Override
	protected void addConstructorCallTransactionInternal
		(Classpath classpath, ConstructorReference constructor, StorageValue[] actuals, StorageValue result, Throwable exception, SortedSet<Update> updates)
		throws TransactionException {

		String spec = "Constructor execution\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Constructor: " + constructor + "\n";
		spec += "actuals: " + Arrays.toString(actuals) + "\n";
		if (result != null)
			spec += "result: " + result + "\n";
		else
			spec += "exception: " + exception.getClass().getName() + "\n";

		addCodeExecutionTransactionInternal(spec, updates);
	}

	@Override
	protected void addInstanceMethodCallTransactionInternal(Classpath classpath, MethodReference method,
			StorageValue receiver, StorageValue[] actuals, StorageValue result, Throwable exception,
			SortedSet<Update> updates) throws TransactionException {

		String spec = "Instance method execution\n";
		spec += "Class path: " + classpath + "\n";
		spec += "Method: " + method + "\n";
		spec += "actuals: " + Arrays.toString(actuals) + "\n";
		if (result != null)
			spec += "result: " + result + "\n";
		else
			spec += "exception: " + exception.getClass().getName() + "\n";

		addCodeExecutionTransactionInternal(spec, updates);
	}

	private void addCodeExecutionTransactionInternal(String spec, SortedSet<Update> updates) throws TransactionException {
		Path specPath = getCurrentPathFor(SPEC_NAME);

		try {
			ensureDeleted(specPath.getParent());
			Files.createDirectories(specPath.getParent());
		}
		catch (IOException e) {
			throw new TransactionException("Could not create transaction entry " + specPath.getParent());
		}

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(specPath.toFile())))) {
			output.println(spec);
		}
		catch (IOException e) {
			throw new TransactionException("Could not store the specification of the transaction", e);
		}

		Path updatesPath = getCurrentPathFor(UPDATES_NAME);

		try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(updatesPath.toFile())))) {
			updates.forEach(output::println);
		}
		catch (Throwable t) {
			throw new TransactionException("Could not store the updates of the transaction", t);
		}
	}

	@Override
	protected void extractPathsRecursively(Classpath classpath, List<Path> result) throws TransactionException {
		if (classpath.recursive) {
			Path path = getPathFor(classpath.transaction, DEPENDENCIES_NAME);
			if (Files.exists(path)) {
				try {
					for (String line: Files.readAllLines(path))
						extractPathsRecursively(new Classpath(line), result);
				}
				catch (IOException e) {
					throw new TransactionException("Cannot read dependencies from " + path);
				}
			}
		}

		Path path = getPathFor(classpath.transaction, INSTRUMENTED_JAR_NAME);
		if (!Files.exists(path))
			throw new TransactionException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");

		result.add(path);
	}

	@Override
	protected boolean blockchainIsFull() {
		return currentBlock == Long.MAX_VALUE && currentTransaction == transactionsPerBlock;
	}

	@Override
	protected void moveToNextTransaction() {
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
	
		private void addURLs(Classpath classpath) throws TransactionException {
			if (classpath.recursive) {
				Path path = getPathFor(classpath.transaction, DEPENDENCIES_NAME);
				if (Files.exists(path)) {
					try {
						for (String line: Files.readAllLines(path))
							addURLs(new Classpath(line));
					}
					catch (IOException e) {
						throw new TransactionException("Cannot read dependencies from " + path);
					}
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