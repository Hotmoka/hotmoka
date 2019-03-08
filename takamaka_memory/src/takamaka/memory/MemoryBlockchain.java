package takamaka.memory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeReference;
import takamaka.blockchain.FieldReference;
import takamaka.blockchain.StorageReference;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Storage;

public class MemoryBlockchain implements Blockchain {
	/**
	 * The name used for the instrumented jars stored in the chain.
	 */
	public final String INSTRUMENTED_JAR_NAME = "instrumented.jar";

	/**
	 * The name used for the file describing the dependencies of a jar.
	 */
	public final String DEPENDENCIES_NAME = "deps.txt";

	private final Path root;
	private final short transactionsPerBlock;
	private long currentBlock;
	private short currentTransaction;

	public MemoryBlockchain(Path root, short transactionsPerBlock) throws IOException {
		if (root == null)
			throw new NullPointerException("A root path must be specified");

		if (transactionsPerBlock <= 0)
			throw new IllegalArgumentException("transactionsPerBlock must be positive");

		// cleans the directory where the blockchain lives
		if (Files.exists(root))
			Files.walk(root)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);

		this.root = root;
		this.transactionsPerBlock = transactionsPerBlock;
	}

	@Override
	public TransactionReference getCurrentTransactionReference() {
		return new TransactionReference(currentBlock, currentTransaction);
	}

	@Override
	public Storage deserialize(StorageReference reference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object deserializeLastUpdateFor(StorageReference reference, FieldReference field) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarFile jar, Classpath... dependencies) throws TransactionException {
		if (currentBlock == Long.MAX_VALUE && currentTransaction == transactionsPerBlock)
			throw new TransactionException("No more transactions available in blockchain");

		TransactionReference ref = getCurrentTransactionReference();
		for (Classpath dependency: dependencies)
			if (!dependency.transaction.isOlderThan(ref))
				throw new TransactionException("A transaction can only depend on older transactions");

		Path jarFile = getCurrentPathFor(getJarSimpleName(jar.getName()));

		try {
			Files.createDirectories(jarFile.getParent());
		}
		catch (IOException e) {
			throw new TransactionException("Could not create transaction entry " + jarFile.getParent());
		}

		// the original jar file is stored in this blockchain, but not used;
		// in a real blockchain, it is needed instead to reexecute and check the transaction
		try {
			Files.copy(Paths.get(jar.getName()), jarFile);
		}
		catch (IOException e) {
			throw new TransactionException("Could not store jar into transaction entry " + jarFile);
		}

		Path instrumentedJarFile = getCurrentPathFor(INSTRUMENTED_JAR_NAME);
		try {
			// TODO: the instrumented jar must go here
			Files.copy(Paths.get(jar.getName()), instrumentedJarFile);
		}
		catch (IOException e) {
			throw new TransactionException("Could not store instrumented jar into transaction entry");
		}

		if (dependencies.length > 0) {
			Path dependenciesFile = getCurrentPathFor(DEPENDENCIES_NAME);
			try (final PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(dependenciesFile.toFile())))) {
				for (Classpath dependency: dependencies)
					output.printf("%s;%b\n", dependency.transaction, dependency.recursive);
			}
			catch (IOException e) {
				throw new TransactionException("Could not store dependencies into transaction entry " + jarFile);
			}
		}

		moveToNextTransaction();
		return ref;
	}

	private static String getJarSimpleName(String name) throws TransactionException {
		if (!name.endsWith(".jar"))
			throw new TransactionException("Jar name must end in .jar");

		int last = name.lastIndexOf(File.separatorChar);
		if (last >= 0)
			name = name.substring(last + 1);

		if (name.isEmpty())
			throw new TransactionException("Jar name cannot be empty");
		else if (name.length() > 100)
			throw new TransactionException("Jar name cannot be longer than 100 characters");

		return name;
	}

	@Override
	public StorageValue addCodeExecutionTransaction(Classpath classpath, CodeReference sig, StorageValue... pars) throws TransactionException {
		if (currentBlock == Long.MAX_VALUE && currentTransaction == transactionsPerBlock)
			throw new TransactionException("No more transactions available in blockchain");

		// create class loader
		List<URL> urls = new ArrayList<>();
		addURLs(classpath, urls);

		System.out.println(urls);

		// create thread running code
		
		// TODO Auto-generated method stub
		moveToNextTransaction();
		return null;
	}

	private void addURLs(Classpath classpath, List<URL> urls) throws TransactionException {
		if (classpath.recursive) {
			Path path = getPathFor(classpath.transaction, DEPENDENCIES_NAME);
			if (Files.exists(path)) {
				try {
					Files.lines(path).forEachOrdered(System.out::println);
				} catch (IOException e) {
					throw new TransactionException("Cannot read dependencies from " + path);
				}
			}
		}

		Path path = getPathFor(classpath.transaction, INSTRUMENTED_JAR_NAME);
		if (!Files.exists(path))
			throw new TransactionException("Transaction " + classpath.transaction + " does not seem to contain an instrumented jar");

		try {
			urls.add(path.toFile().toURI().toURL());
		}
		catch (MalformedURLException e) {
			throw new TransactionException("Found illegal classpath: " + e.getMessage());
		}
	}

	private Path getCurrentPathFor(String fileName) {
		return Paths.get(root.toString(), "b" + currentBlock, "t" + currentTransaction, fileName);
	}

	private Path getPathFor(TransactionReference ref, String fileName) {
		return Paths.get(root.toString(), "b" + ref.blockNumber, "t" + ref.transactionNumber, fileName);
	}

	private void moveToNextTransaction() {
		if (++currentTransaction == transactionsPerBlock) {
			currentTransaction = 0;
			currentBlock++;
		}
	}

	public static void main(String[] args) throws TransactionException, IOException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);
		TransactionReference test_contracts_dependency = blockchain.addJarStoreTransaction(new JarFile("../test_contracts_dependency/dist/test_contracts_dependency.jar"));
		TransactionReference test_contracts = blockchain.addJarStoreTransaction(new JarFile("../test_contracts/dist/test_contracts.jar"), new Classpath(test_contracts_dependency, true)); // true/false irrelevant here
		blockchain.addCodeExecutionTransaction(new Classpath(test_contracts, true), null);
	}
}