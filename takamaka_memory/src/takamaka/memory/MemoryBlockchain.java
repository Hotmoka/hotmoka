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
import java.util.Comparator;
import java.util.jar.JarFile;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.FieldReference;
import takamaka.blockchain.StorageReference;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.lang.Storage;

public class MemoryBlockchain extends AbstractBlockchain {
	/**
	 * The name used for the instrumented jars stored in the chain.
	 */
	public final static String INSTRUMENTED_JAR_NAME = "instrumented.jar";

	/**
	 * The name used for the file describing the dependencies of a jar.
	 */
	public final static String DEPENDENCIES_NAME = "deps.txt";

	private final Path root;
	private final short transactionsPerBlock;

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
	protected void addJarStoreTransactionInternal(JarFile jar, Classpath... dependencies) throws TransactionException {
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
			try (PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(dependenciesFile.toFile())))) {
				for (Classpath dependency: dependencies)
					output.println(dependency);
			}
			catch (IOException e) {
				throw new TransactionException("Could not store dependencies into transaction entry " + jarFile);
			}
		}
	}

	@Override
	protected boolean blockchainIsFull() {
		return currentBlock == Long.MAX_VALUE && currentTransaction == transactionsPerBlock;
	}

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

	private Path getCurrentPathFor(String fileName) {
		return Paths.get(root.toString(), "b" + currentBlock, "t" + currentTransaction, fileName);
	}

	private Path getPathFor(TransactionReference ref, String fileName) {
		return Paths.get(root.toString(), "b" + ref.blockNumber, "t" + ref.transactionNumber, fileName);
	}
}