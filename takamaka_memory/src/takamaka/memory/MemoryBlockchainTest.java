package takamaka.memory;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorReference;
import takamaka.blockchain.MethodReference;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;

public class MemoryBlockchainTest {

	public static void main(String[] args) throws TransactionException, IOException, CodeExecutionException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);
		TransactionReference test_contracts_dependency = blockchain.addJarStoreTransaction(Paths.get("../test_contracts_dependency/dist/test_contracts_dependency.jar"));
		TransactionReference test_contracts = blockchain.addJarStoreTransaction(Paths.get("../test_contracts/dist/test_contracts.jar"), new Classpath(test_contracts_dependency, true)); // true/false irrelevant here
		Classpath classpath = new Classpath(test_contracts, true);

		StorageReference italianTimeRef = blockchain.addConstructorCallTransaction(classpath, new ConstructorReference("takamaka.tests.ItalianTime", INT, INT, INT),
				new IntValue(13), new IntValue(25), new IntValue(40));
		System.out.println("1: " + italianTimeRef);
		StorageReference wrapper1Ref = blockchain.addConstructorCallTransaction
				(classpath,
				new ConstructorReference("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time")),
				italianTimeRef);
		System.out.println("2: " + wrapper1Ref);
		StringValue toString1 = (StringValue) blockchain.addInstanceMethodCallTransaction
			(classpath,
			new MethodReference(ClassType.OBJECT, "toString"),
			wrapper1Ref);
		System.out.println("3: " + toString1);
		StorageReference wrapper2Ref = blockchain.addConstructorCallTransaction
				(classpath,
				new ConstructorReference("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time"), ClassType.STRING, ClassType.BIG_INTEGER, BasicTypes.LONG),
				italianTimeRef, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(13011973)), new LongValue(12345L));
		System.out.println("4: " + wrapper2Ref);
		StringValue toString2 = (StringValue) blockchain.addInstanceMethodCallTransaction
				(classpath,
				new MethodReference(ClassType.OBJECT, "toString"),
				wrapper2Ref);
		System.out.println("5: " + toString2);
	}
}