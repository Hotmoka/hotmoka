package takamaka.memory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
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
	 * The name used for the jars stored into the chain. A constant small name
	 * keeps the chain small and avoids the risk of a long string injection.
	 */
	public final String JAR_NAME = "a.jar";

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
		Path jarFile = getCurrentPathFor(JAR_NAME);

		try {
			Files.createDirectories(jarFile.getParent());
		}
		catch (IOException e) {
			throw new TransactionException("Could not create transaction entry " + jarFile.getParent());
		}

		try {
			Files.copy(Paths.get(jar.getName()), jarFile);
		}
		catch (IOException e) {
			throw new TransactionException("Could not store jar into transaction entry " + jarFile);
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

		TransactionReference ref = new TransactionReference(currentBlock, currentTransaction);
		moveToNextTransaction();
		return ref;
	}

	private Path getCurrentPathFor(String fileName) {
		return Paths.get(root.toString(), "b" + currentBlock, "t" + currentTransaction, fileName);
	}

	@Override
	public TransactionReference addCodeExecutionTransaction(Classpath classpath, CodeReference sig, StorageValue[] pars) throws TransactionException {
		// TODO Auto-generated method stub
		TransactionReference ref = new TransactionReference(currentBlock, currentTransaction);
		moveToNextTransaction();
		return ref;
	}

	private void moveToNextTransaction() {
		if (++currentTransaction == transactionsPerBlock) {
			currentTransaction = 0;
			currentBlock++;
		}
	}

	public static void main(String[] args) throws TransactionException, IOException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);
		TransactionReference runtime = blockchain.addJarStoreTransaction(new JarFile("../takamaka_runtime/dist/takamaka_runtime.jar"));
		TransactionReference auction = blockchain.addJarStoreTransaction(new JarFile("../auction/dist/auction.jar"), new Classpath(runtime, true)); // true/false irrelevant here
	}
}