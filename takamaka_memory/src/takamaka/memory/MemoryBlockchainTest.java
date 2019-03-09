package takamaka.memory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;

public class MemoryBlockchainTest {

	public static void main(String[] args) throws TransactionException, IOException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);
		TransactionReference test_contracts_dependency = blockchain.addJarStoreTransaction(new JarFile("../test_contracts_dependency/dist/test_contracts_dependency.jar"));
		TransactionReference test_contracts = blockchain.addJarStoreTransaction(new JarFile("../test_contracts/dist/test_contracts.jar"), new Classpath(test_contracts_dependency, true)); // true/false irrelevant here
		blockchain.addCodeExecutionTransaction(new Classpath(test_contracts, true), null);
	}
}