package takamaka.memory;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

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
import takamaka.blockchain.values.StringValue;

public class MemoryBlockchainTest {
	private static int counter;

	private static <T> T run(String from, String message, Callable<T> what) {
		try {
			T result = what.call();
			System.out.printf("[%8s] %2d: %40s = %s\n", from, counter++, message, result);
			return result;
		}
		catch (Throwable t) {
			System.out.printf("[%8s] %2d: %40s raises %s since %s\n", from, counter++, message, t, t.getCause());
			return null;
		}
	}

	public static void main(String[] args) throws TransactionException, IOException, CodeExecutionException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"), (short) 5);

		// we need at least the base Takamaka classes in the blockchain
		TransactionReference takamaka_base = run("", "takamaka_base.jar", () -> blockchain.addJarStoreInitialTransaction(Paths.get("../takamaka_base/dist/takamaka_base.jar")));
		Classpath takamakaBaseClasspath = new Classpath(takamaka_base, false);  // true/false irrelevant here
		StorageReference gamete = run("", "gamete", () -> blockchain.addGameteCreationTransaction(takamakaBaseClasspath, BigInteger.valueOf(1000000)));
		TransactionReference test_contracts_dependency = run("gamete", "test_contract_dependency.jar", () -> blockchain.addJarStoreTransaction(gamete, 10000, takamakaBaseClasspath, Paths.get("../test_contracts_dependency/dist/test_contracts_dependency.jar"), takamakaBaseClasspath));
		TransactionReference test_contracts = run("gamete", "test_contracts.jar", () -> blockchain.addJarStoreTransaction(gamete, 10000, takamakaBaseClasspath, Paths.get("../test_contracts/dist/test_contracts.jar"), new Classpath(test_contracts_dependency, true))); // true relevant here
		Classpath classpath = new Classpath(test_contracts, true);

		StorageReference italianTime = run("gamete", "italianTime = new ItalianTime(13,25,40)", () -> blockchain.addConstructorCallTransaction(gamete, 20000, classpath, new ConstructorReference("takamaka.tests.ItalianTime", INT, INT, INT),
				new IntValue(13), new IntValue(25), new IntValue(40)));
		StorageReference wrapper1 = run("gamete", "wrapper1 = new Wrapper(italianTime)", () -> blockchain.addConstructorCallTransaction
				(gamete, 20000, classpath,
				new ConstructorReference("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time")),
				italianTime));
		run("gamete", "wrapper1.toString()", () -> (StringValue) blockchain.addInstanceMethodCallTransaction
			(gamete, 20000, classpath,
			new MethodReference(ClassType.OBJECT, "toString"),
			wrapper1));
		StorageReference wrapper2 = run("gamete", "wrapper2 = new Wrapper(italianTime,\"hello\",13011973,12345L)", () -> blockchain.addConstructorCallTransaction
				(gamete, 20000, classpath,
				new ConstructorReference("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time"), ClassType.STRING, ClassType.BIG_INTEGER, BasicTypes.LONG),
				italianTime, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(13011973)), new LongValue(12345L)));
		run("gamete", "wrapper2.toString()", () -> (StringValue) blockchain.addInstanceMethodCallTransaction
				(gamete, 20000, classpath,
				new MethodReference(ClassType.OBJECT, "toString"),
				wrapper2));
		// we try to call the constructor Sub(int): it does not exist since an @Entry requires an implicit Contract parameter
		run("gamete", "new Sub(1973)", () -> blockchain.addConstructorCallTransaction
			(gamete, 20000, classpath,
			new ConstructorReference("takamaka.tests.Sub", INT),
			new IntValue(1973)));

		// we try to call the constructor Sub(int,Contract): it exists but it is an entry, hence cannot be called this way
		run("gamete", "new Sub(1973, null)", () -> blockchain.addConstructorCallTransaction
			(gamete, 20000, classpath,
			new ConstructorReference("takamaka.tests.Sub", INT, new ClassType("takamaka.lang.Contract")),
			new IntValue(1973), NullValue.INSTANCE));

		StorageReference sub1 = run("gamete", "sub1 = new Sub()", () -> blockchain.addConstructorCallTransaction(gamete, 100, classpath, new ConstructorReference("takamaka.tests.Sub")));
		// we try to call Sub.m1(): it does not exist since an @Entry requires an implicit Contract parameter
		run("gamete", "sub1.m1()", () -> blockchain.addInstanceMethodCallTransaction
				(gamete, 20000, classpath, new MethodReference("takamaka.tests.Sub", "m1"), sub1));

		// we try to call Sub.m1(Contract): it exists but it is an entry, hence cannot be called this way
		run("gamete", "sub1.m1(null)", () -> blockchain.addInstanceMethodCallTransaction
				(gamete, 20000, classpath,
					new MethodReference("takamaka.tests.Sub", "m1", new ClassType("takamaka.lang.Contract")),
					sub1, NullValue.INSTANCE));

		// we try to call a static method in the wrong way
		run("gamete", "sub1.ms()", () -> blockchain.addInstanceMethodCallTransaction
				(gamete, 20000, classpath, new MethodReference("takamaka.tests.Sub", "ms"), sub1));

		// we call it in the correct way
		run("gamete", "Sub.ms()", () -> blockchain.addStaticMethodCallTransaction
			(gamete, 20000, classpath, new MethodReference("takamaka.tests.Sub", "ms")));

		// we try to call an instance method as if it were static
		run("gamete", "Sub.m5()", () -> blockchain.addStaticMethodCallTransaction
				(gamete, 20000, classpath, new MethodReference("takamaka.tests.Sub", "m5")));

		StorageReference eoa = run("gamete", "eoa = new ExternallyOwnedAccount()",
				() -> blockchain.addConstructorCallTransaction(gamete, 20000, classpath,
				new ConstructorReference("takamaka.lang.ExternallyOwnedAccount")));

		// we call the constructor Sub(int): it is @Entry but the caller has not enough funds
		run("eoa", "new Sub(1973)", () -> blockchain.addEntryConstructorCallTransaction
				(eoa, 20000, classpath,
				new ConstructorReference("takamaka.tests.Sub", INT),
				new IntValue(1973)));

		// we recharge eoa
		run("gamete", "eoa.receive(2000)", () -> blockchain.addEntryInstanceMethodCallTransaction
				(gamete, 20000, classpath, new MethodReference("takamaka.lang.PayableContract", "receive", INT), eoa, new IntValue(2000)));

		// still not enough funds since we are promising too much gas
		run("eoa", "sub2 = new Sub(1973)", () -> blockchain.addEntryConstructorCallTransaction
			(eoa, 20000, classpath, new ConstructorReference("takamaka.tests.Sub", INT), new IntValue(1973)));

		// now it's ok
		StorageReference sub2 = run("eoa", "sub2 = new Sub(1973)", () -> blockchain.addEntryConstructorCallTransaction
			(eoa, 150, classpath, new ConstructorReference("takamaka.tests.Sub", INT), new IntValue(1973)));

		run("gamete", "sub2.print(italianTime)", () -> blockchain.addEntryInstanceMethodCallTransaction
			(gamete, 20000, classpath, new MethodReference("takamaka.tests.Sub", "print", new ClassType("takamaka.tests.Time")), sub2, italianTime));
		
		run("gamete", "sub1.m4(13)", () -> blockchain.addEntryInstanceMethodCallTransaction
			(gamete, 20000, classpath, new MethodReference("takamaka.tests.Sub", "m4", INT), sub1, new IntValue(13)));
	}
}