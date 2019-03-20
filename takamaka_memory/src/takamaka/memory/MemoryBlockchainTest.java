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
import takamaka.blockchain.values.NullValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.blockchain.values.StringValue;

public class MemoryBlockchainTest {

	public static void main(String[] args) throws TransactionException, IOException, CodeExecutionException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);
		TransactionReference takamaka_utils = blockchain.addJarStoreTransaction(Paths.get("../takamaka_base/dist/takamaka_base.jar"));
		TransactionReference test_contracts_dependency = blockchain.addJarStoreTransaction(Paths.get("../test_contracts_dependency/dist/test_contracts_dependency.jar"), new Classpath(takamaka_utils, true)); // true/false irrelevant here
		TransactionReference test_contracts = blockchain.addJarStoreTransaction(Paths.get("../test_contracts/dist/test_contracts.jar"), new Classpath(test_contracts_dependency, true)); // true relevant here
		Classpath classpath = new Classpath(test_contracts, true);

		StorageReference italianTimeRef = blockchain.addConstructorCallTransaction(classpath, new ConstructorReference("takamaka.tests.ItalianTime", INT, INT, INT),
				new IntValue(13), new IntValue(25), new IntValue(40));
		System.out.println(" 1: " + italianTimeRef);
		StorageReference wrapper1Ref = blockchain.addConstructorCallTransaction
				(classpath,
				new ConstructorReference("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time")),
				italianTimeRef);
		System.out.println(" 2: " + wrapper1Ref);
		StringValue toString1 = (StringValue) blockchain.addInstanceMethodCallTransaction
			(classpath,
			new MethodReference(ClassType.OBJECT, "toString"),
			wrapper1Ref);
		System.out.println(" 3: " + toString1);
		StorageReference wrapper2Ref = blockchain.addConstructorCallTransaction
				(classpath,
				new ConstructorReference("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time"), ClassType.STRING, ClassType.BIG_INTEGER, BasicTypes.LONG),
				italianTimeRef, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(13011973)), new LongValue(12345L));
		System.out.println(" 4: " + wrapper2Ref);
		StringValue toString2 = (StringValue) blockchain.addInstanceMethodCallTransaction
				(classpath,
				new MethodReference(ClassType.OBJECT, "toString"),
				wrapper2Ref);
		System.out.println(" 5: " + toString2);
		// we try to call the constructor Sub(int): it does not exist since an @Entry requires an implicit Contract parameter
		try {
			blockchain.addConstructorCallTransaction
				(classpath,
				new ConstructorReference("takamaka.tests.Sub", BasicTypes.INT),
				new IntValue(1973));
		}
		catch (TransactionException e) {
			System.out.println(" 6: " + e.getCause());
		}

		// we try to call the constructor Sub(int,Contract): it exists but it is an entry, hence cannot be called this way
		try {
			blockchain.addConstructorCallTransaction
				(classpath,
					new ConstructorReference("takamaka.tests.Sub", BasicTypes.INT, new ClassType("takamaka.lang.Contract")),
					new IntValue(1973), NullValue.INSTANCE);
		}
		catch (TransactionException e) {
			System.out.println(" 7: " + e.getCause());
		}

		StorageReference subRef1 = blockchain.addConstructorCallTransaction(classpath, new ConstructorReference("takamaka.tests.Sub"));
		System.out.println(" 8: " + subRef1);

		// we try to call Sub.m1(): it does not exist since an @Entry requires an implicit Contract parameter
		try {
			blockchain.addInstanceMethodCallTransaction
				(classpath, new MethodReference("takamaka.tests.Sub", "m1"), subRef1);
		}
		catch (TransactionException e) {
			System.out.println(" 9: " + e.getCause());
		}

		// we try to call Sub.m1(Contract): it exists but it is an entry, hence cannot be called this way
		try {
			blockchain.addInstanceMethodCallTransaction
				(classpath,
					new MethodReference("takamaka.tests.Sub", "m1", new ClassType("takamaka.lang.Contract")),
					subRef1, NullValue.INSTANCE);
		}
		catch (TransactionException e) {
			System.out.println("10: " + e.getCause());
		}

		// we try to call a static method in the wrong way
		try {
			blockchain.addInstanceMethodCallTransaction
				(classpath, new MethodReference("takamaka.tests.Sub", "ms"), subRef1);
		}
		catch (TransactionException e) {
			System.out.println("11: " + e.getCause());
		}

		// we call it in the correct way
		StorageValue o = blockchain.addStaticMethodCallTransaction
			(classpath, new MethodReference("takamaka.tests.Sub", "ms"));
		System.out.println("12: " + o); /// we will get null since the method is void

		// we try to call an instance method as it it was static
		try {
			blockchain.addStaticMethodCallTransaction
				(classpath, new MethodReference("takamaka.tests.Sub", "m5"));
		}
		catch (TransactionException e) {
			System.out.println("13: " + e.getCause());
		}

		// we call the constructor Sub(int): it is @Entry but the caller has not enough funds
		try {
			blockchain.addEntryConstructorCallTransaction
				(classpath,
				new ConstructorReference("takamaka.tests.Sub", BasicTypes.INT),
				subRef1, new IntValue(1973));
		}
		catch (CodeExecutionException e) {
			System.out.println("14: " + e.getCause());
		}

		// we call the constructor Sub(int): this time we do not transfer money
		StorageReference subRef2 = blockchain.addEntryConstructorCallTransaction
			(classpath,
			new ConstructorReference("takamaka.tests.Sub", BasicTypes.INT),
			subRef1, new IntValue(0));
		System.out.println("15: " + subRef2);
	}
}