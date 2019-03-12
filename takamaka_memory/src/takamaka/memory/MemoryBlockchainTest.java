package takamaka.memory;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.io.IOException;
import java.nio.file.Paths;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorReference;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;

public class MemoryBlockchainTest {

	public static void main(String[] args) throws TransactionException, IOException, CodeExecutionException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);
		TransactionReference test_contracts_dependency = blockchain.addJarStoreTransaction(Paths.get("../test_contracts_dependency/dist/test_contracts_dependency.jar"));
		TransactionReference test_contracts = blockchain.addJarStoreTransaction(Paths.get("../test_contracts/dist/test_contracts.jar"), new Classpath(test_contracts_dependency, true)); // true/false irrelevant here
		Classpath classpath = new Classpath(test_contracts, true);

		StorageReference italianTime = blockchain.addConstructorCallTransaction(classpath, new ConstructorReference("takamaka.tests.ItalianTime", INT, INT, INT),
				new IntValue(13), new IntValue(25), new IntValue(40));
		System.out.println(italianTime);
	}
}