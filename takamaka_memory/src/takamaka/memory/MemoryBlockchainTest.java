package takamaka.memory;

import static takamaka.blockchain.types.BasicTypes.INT;
import static takamaka.blockchain.types.BasicTypes.LONG;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;

/**
 * This test assumes the existence of the following compiled projects:
 * ../takamaka_base/dist/takamaka_base.jar
 * ../test_contracts/dist/test_contracts.jar
 * ../test_contracts_dependency/dist/test_contracts_dependency.jar
 */
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

	public static void main(String[] args) throws IOException {
		Blockchain blockchain = new MemoryBlockchain(Paths.get("chain"));

		// we need at least the base Takamaka classes in the blockchain
		TransactionReference takamaka_base = run("", "takamaka_base.jar",
			() -> blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_base/dist/takamaka_base.jar")))));
		Classpath takamakaBaseClasspath = new Classpath(takamaka_base, false);  // true/false irrelevant here
		StorageReference gamete = run("", "gamete", () -> blockchain.addGameteCreationTransaction
			(new GameteCreationTransactionRequest(takamakaBaseClasspath, BigInteger.valueOf(1000000))));

		TransactionReference test_contracts_dependency = run("gamete", "test_contract_dependency.jar",
			() -> blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(gamete, BigInteger.valueOf(10000), takamakaBaseClasspath,
				Files.readAllBytes(Paths.get("../test_contracts_dependency/dist/test_contracts_dependency.jar")), takamakaBaseClasspath)));

		TransactionReference test_contracts = run("gamete", "test_contracts.jar",
			() -> blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(gamete, BigInteger.valueOf(10000), takamakaBaseClasspath,
				Files.readAllBytes(Paths.get("../test_contracts/dist/test_contracts.jar")), new Classpath(test_contracts_dependency, true)))); // true relevant here
		Classpath classpath = new Classpath(test_contracts, true);

		StorageReference italianTime = run("gamete", "italianTime = new ItalianTime(13,25,40)",
			() -> blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new ConstructorSignature("takamaka.tests.ItalianTime", INT, INT, INT),
			new IntValue(13), new IntValue(25), new IntValue(40))));

		StorageReference wrapper1 = run("gamete", "wrapper1 = new Wrapper(italianTime)", () -> blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath,
				new ConstructorSignature("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time")),
				italianTime)));

		run("gamete", "wrapper1.toString()", () -> (StringValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath,
			new MethodSignature(ClassType.OBJECT, "toString"),
			wrapper1)));

		StorageReference wrapper2 = run("gamete", "wrapper2 = new Wrapper(italianTime,\"hello\",13011973,12345L)", () -> blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath,
				new ConstructorSignature("takamaka.tests.Wrapper", new ClassType("takamaka.tests.Time"), ClassType.STRING, ClassType.BIG_INTEGER, BasicTypes.LONG),
				italianTime, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(13011973)), new LongValue(12345L))));

		run("gamete", "wrapper2.toString()", () -> (StringValue) blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath,
				new MethodSignature(ClassType.OBJECT, "toString"),
				wrapper2)));
		// we call the @Entry constructor Sub(int)
		run("gamete", "new Sub(1973)", () -> blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, BigInteger.valueOf(20000), classpath, new ConstructorSignature("takamaka.tests.Sub", INT), new IntValue(1973))));

		StorageReference sub1 = run("gamete", "sub1 = new Sub()",
			() -> blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, BigInteger.valueOf(200), classpath, new ConstructorSignature("takamaka.tests.Sub"))));
		// we try to call Sub.m1(): it is an entry that goes into a runtime exception
		run("gamete", "sub1.m1()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "m1"), sub1)));

		// we try to call a static method in the wrong way
		run("gamete", "sub1.ms()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "ms"), sub1)));

		// we call it in the correct way
		run("gamete", "Sub.ms()", () -> blockchain.addStaticMethodCallTransaction
			(new StaticMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "ms"))));

		// we try to call an instance method as if it were static
		run("gamete", "Sub.m5()", () -> blockchain.addStaticMethodCallTransaction
			(new StaticMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "m5"))));

		StorageReference eoa = run("gamete", "eoa = new ExternallyOwnedAccount()",
			() -> blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, BigInteger.valueOf(20000), classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount"))));

		// we call the constructor Sub(int): it is @Entry but the caller has not enough funds
		run("eoa", "new Sub(1973)", () -> blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, BigInteger.valueOf(20000), classpath, new ConstructorSignature("takamaka.tests.Sub", INT), new IntValue(1973))));

		// we recharge eoa
		run("gamete", "eoa.receive(2000)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.lang.PayableContract", "receive", INT), eoa, new IntValue(2000))));

		// still not enough funds since we are promising too much gas
		run("eoa", "sub2 = new Sub(1973)", () -> blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, BigInteger.valueOf(20000), classpath, new ConstructorSignature("takamaka.tests.Sub", INT), new IntValue(1973))));

		// we recharge eoa
		run("gamete", "eoa.receive(2000)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.lang.PayableContract", "receive", INT), eoa, new IntValue(2000))));

		// now it's ok
		StorageReference sub2 = run("eoa", "sub2 = new Sub(1973)", () -> blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, BigInteger.valueOf(200), classpath, new ConstructorSignature("takamaka.tests.Sub", INT), new IntValue(1973))));

		run("gamete", "sub2.print(italianTime)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "print", new ClassType("takamaka.tests.Time")), sub2, italianTime)));
		
		run("gamete", "sub1.m4(13)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "m4", INT), sub1, new IntValue(13))));

		run("gamete", "sub1.m4_1(13L)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "m4_1", LONG), sub1, new LongValue(13L))));

		ClassType bigInteger = ClassType.BIG_INTEGER;
		run("gamete", "sub1.m4_2(BigInteger.valueOf(13))", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath, new MethodSignature("takamaka.tests.Sub", "m4_2", bigInteger), sub1, new BigIntegerValue(BigInteger.valueOf(13)))));

		// alias tests
		ClassType alias = new ClassType("takamaka.tests.Alias");
		StorageReference a1 = run("gamete", "a1 = new Alias()", () -> blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new ConstructorSignature(alias))));
		StorageReference a2 = run("gamete", "a2 = new Alias()", () -> blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new ConstructorSignature(alias))));

		// this test should return false
		run("gamete", "a1.test(a1, a2)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(alias, "test", alias, alias), a1, a1, a2)));
		// this test should return true
		run("gamete", "a1.test(a1, a1)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(alias, "test", alias, alias), a1, a1, a1)));

		StringValue s1 = new StringValue("hello");
		StringValue s2 = new StringValue("hello");
		ClassType string = ClassType.STRING;

		// this test should return false
		run("gamete", "a1.test(s1, s2)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(alias, "test", string, string), a1, s1, s2)));
		// this test should return false since String parameters are considered different
		run("gamete", "a1.test(s1, s1)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(alias, "test", string, string), a1, s1, s1)));

		BigIntegerValue bi1 = new BigIntegerValue(BigInteger.valueOf(13));
		BigIntegerValue bi2 = new BigIntegerValue(BigInteger.valueOf(13));

		// this test should return false
		run("gamete", "a1.test(bi1, bi2)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(alias, "test", bigInteger, bigInteger), a1, bi1, bi2)));
		// this test should return false since BigInteger parameters are considered different
		run("gamete", "a1.test(bi1, bi1)", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(alias, "test", bigInteger, bigInteger), a1, bi1, bi1)));

		ClassType simple = new ClassType("takamaka.tests.Simple");
		StorageReference s = run("gamete", "s = new Simple(13)", () -> blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new ConstructorSignature(simple, BasicTypes.INT), new IntValue(13))));
		run("gamete", "s.foo1()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(simple, "foo1"), s)));
		run("gamete", "s.foo2()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(simple, "foo2"), s)));
		run("gamete", "s.foo3()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(simple, "foo3"), s)));
		run("gamete", "s.foo4()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(simple, "foo4"), s)));
		run("gamete", "s.foo5()", () -> blockchain.addStaticMethodCallTransaction
			(new StaticMethodCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new MethodSignature(simple, "foo5"))));

		ClassType withList = new ClassType("takamaka.tests.WithList");
		StorageReference wl = run("gamete", "wl = new WithList()", () -> blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, BigInteger.valueOf(1000), classpath, new ConstructorSignature(withList))));
		run("gamete", "wl.toString()", () -> (StringValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath,
			new MethodSignature(withList, "toString"),
			wl)));
		run("gamete", "wl.illegal()", () -> blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, BigInteger.valueOf(20000), classpath,
			new MethodSignature(withList, "illegal"),
			wl)));
	}
}