/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static takamaka.blockchain.types.BasicTypes.INT;
import static takamaka.blockchain.types.BasicTypes.LONG;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.DeserializationError;
import takamaka.blockchain.IllegalTransactionRequestException;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.SideEffectsInViewMethodException;
import takamaka.blockchain.TransactionException;
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
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;
import takamaka.lang.InsufficientFundsError;
import takamaka.lang.RequirementViolationException;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for basic storage and contract Takamaka classes.
 */
class Basic {

	private static final BigInteger _1_000 = BigInteger.valueOf(1000);

	private static final ConstructorSignature CONSTRUCTOR_ALIAS = new ConstructorSignature(new ClassType("takamaka.tests.basicdependency.Alias"));

	private static final MethodSignature PAYABLE_CONTRACT_RECEIVE = new MethodSignature("takamaka.lang.PayableContract", "receive", INT);

	private static final MethodSignature SUB_MS = new MethodSignature("takamaka.tests.basic.Sub", "ms");

	private static final MethodSignature SUB_M5 = new MethodSignature("takamaka.tests.basic.Sub", "m5");

	private static final ConstructorSignature CONSTRUCTOR_WRAPPER_1 = new ConstructorSignature("takamaka.tests.basicdependency.Wrapper", new ClassType("takamaka.tests.basicdependency.Time"));

	private static final ConstructorSignature CONSTRUCTOR_WRAPPER_2 = new ConstructorSignature("takamaka.tests.basicdependency.Wrapper", new ClassType("takamaka.tests.basicdependency.Time"), ClassType.STRING, ClassType.BIG_INTEGER, BasicTypes.LONG);

	private static final ConstructorSignature CONSTRUCTOR_INTERNATIONAL_TIME = new ConstructorSignature("takamaka.tests.basicdependency.InternationalTime", INT, INT, INT);

	private static final MethodSignature TO_STRING = new MethodSignature(ClassType.OBJECT, "toString");

	private static final BigInteger _200 = BigInteger.valueOf(200);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private Blockchain blockchain;

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference basicdependency = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, BigInteger.valueOf(10000), takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/basicdependency.jar")), takamakaBase));

		TransactionReference basic = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, BigInteger.valueOf(10000), takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/basic.jar")), new Classpath(basicdependency, true))); // true relevant here
		classpath = new Classpath(basic, true);
	}

	@Test @DisplayName("new InternationalTime(13,25,40).toString().equals(\"13:25:40\")")
	void testToStringInternationTime() throws TransactionException, CodeExecutionException {
		StorageReference internationalTime = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40)));
		assertEquals(new StringValue("13:25:40"),
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, TO_STRING, internationalTime)));
	}

	@Test @DisplayName("new Wrapper(new InternationalTime(13,25,40)).toString().equals(\"wrapper(13:25:40,null,null,0)\")")
	void testToStringWrapperInternationTime1() throws TransactionException, CodeExecutionException {
		StorageReference internationalTime = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40)));
		StorageReference wrapper = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_WRAPPER_1,
			internationalTime));
		assertEquals(new StringValue("wrapper(13:25:40,null,null,0)"),
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, TO_STRING, wrapper)));
	}

	@Test @DisplayName("new Wrapper(new InternationalTime(13,25,40),\"hello\",13011973,12345L).toString().equals(\"wrapper(13:25:40,hello,13011973,12345)\")")
	void testToStringWrapperInternationTime2() throws TransactionException, CodeExecutionException {
		StorageReference internationalTime = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40)));
		StorageReference wrapper = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_WRAPPER_2,
			internationalTime, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(13011973)), new LongValue(12345L)));
		assertEquals(new StringValue("wrapper(13:25:40,hello,13011973,12345)"),
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, TO_STRING, wrapper)));
	}

	@Test @DisplayName("new Sub(1973)")
	void callPayableConstructor() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
	}

	@Test @DisplayName("new Sub().m1() throws TransactionException since RequirementViolationException")
	void callEntryFromSameContract() throws CodeExecutionException, TransactionException {
		StorageReference sub = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub")));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature("takamaka.tests.basic.Sub", "m1"), sub));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException && e.getCause().getMessage().equals("An @Entry can only be called from a distinct contract object"))
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Sub().ms() throws TransactionException since NoSuchMethodException")
	void callStaticAsInstance() throws CodeExecutionException, TransactionException {
		StorageReference sub = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub")));

		try {
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, SUB_MS, sub));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof NoSuchMethodException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("Sub.ms()")
	void callStaticAsStatic() throws CodeExecutionException, TransactionException {
		blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(gamete, _20_000, classpath, SUB_MS));
	}

	@Test @DisplayName("Sub.m5() throws TransactionException since NoSuchMethodException")
	void callInstanceAsStatic() throws CodeExecutionException, TransactionException {
		try {
			blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(gamete, _20_000, classpath, SUB_M5));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof NoSuchMethodException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Sub(1973) without gas")
	void callerHasNotEnoughFundsForGas() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		try {
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(eoa, _20_000, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof IllegalTransactionRequestException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Sub(1973) with gas but without enough coins to pay the @Entry")
	void callerHasNotEnoughFundsForPayableEntry() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, PAYABLE_CONTRACT_RECEIVE, eoa, new IntValue(2000)));

		try {
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(eoa, _20_000, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof InsufficientFundsError)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Sub(1973) with gas and enough coins to pay the @Entry")
	void callerHasEnoughFundsForPayableEntry() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, PAYABLE_CONTRACT_RECEIVE, eoa, new IntValue(2000)));
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
	}

	@Test @DisplayName("new Sub(1973).print(new InternationalTime(13,25,40))")
	void callInstanceMethod() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, PAYABLE_CONTRACT_RECEIVE, eoa, new IntValue(2000)));
		StorageReference internationalTime = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40)));
		StorageReference sub = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature("takamaka.tests.basic.Sub", "print", new ClassType("takamaka.tests.basicdependency.Time")), sub, internationalTime));
	}

	@Test @DisplayName("new Sub(1973).m4(13).equals(\"Sub.m4 receives 13 coins from an externally owned account\")")
	void callPayableEntryWithInt() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, PAYABLE_CONTRACT_RECEIVE, eoa, new IntValue(2000)));
		StorageReference sub = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
		assertEquals(new StringValue("Sub.m4 receives 13 coins from an externally owned account"), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature("takamaka.tests.basic.Sub", "m4", INT), sub, new IntValue(13))));
	}

	@Test @DisplayName("new Sub(1973).m4_1(13L).equals(\"Sub.m4_1 receives 13 coins from an externally owned account\")")
	void callPayableEntryWithLong() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, PAYABLE_CONTRACT_RECEIVE, eoa, new IntValue(2000)));
		StorageReference sub = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
		assertEquals(new StringValue("Sub.m4_1 receives 13 coins from an externally owned account"), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature("takamaka.tests.basic.Sub", "m4_1", LONG), sub, new LongValue(13L))));
	}

	@Test @DisplayName("new Sub(1973).m4_2(BigInteger.valueOf(13)).equals(\"Sub.m4_2 receives 13 coins from an externally owned account\")")
	void callPayableEntryWithBigInteger() throws CodeExecutionException, TransactionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, PAYABLE_CONTRACT_RECEIVE, eoa, new IntValue(2000)));
		StorageReference sub = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(eoa, _200, classpath, new ConstructorSignature("takamaka.tests.basic.Sub", INT), new IntValue(1973)));
		assertEquals(new StringValue("Sub.m4_2 receives 13 coins from an externally owned account"), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature("takamaka.tests.basic.Sub", "m4_2", ClassType.BIG_INTEGER),
			sub, new BigIntegerValue(BigInteger.valueOf(13L)))));
	}

	@Test @DisplayName("a1 = new Alias(); a2 = new Alias(); a1.test(a1, a2)=false")
	void aliasBetweenStorage1() throws CodeExecutionException, TransactionException {
		ClassType alias = new ClassType("takamaka.tests.basicdependency.Alias");
		StorageReference a1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		StorageReference a2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		assertEquals(new BooleanValue(false), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(alias, "test", alias, alias), a1, a1, a2)));
	}

	@Test @DisplayName("a1 = new Alias(); a1.test(a1, a1)=true")
	void aliasBetweenStorage2() throws CodeExecutionException, TransactionException {
		ClassType alias = new ClassType("takamaka.tests.basicdependency.Alias");
		StorageReference a1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		assertEquals(new BooleanValue(true), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(alias, "test", alias, alias), a1, a1, a1)));
	}

	@Test @DisplayName("a1 = new Alias(); s1 = \"hello\"; s2 = \"hello\"; a1.test(s1, s2)=false")
	void aliasBetweenString() throws CodeExecutionException, TransactionException {
		ClassType alias = new ClassType("takamaka.tests.basicdependency.Alias");
		StorageReference a1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		StringValue s1 = new StringValue("hello");
		StringValue s2 = new StringValue("hello");
		assertEquals(new BooleanValue(false), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(alias, "test", ClassType.STRING, ClassType.STRING), a1, s1, s2)));
	}

	@Test @DisplayName("a1 = new Alias(); s1 = \"hello\"; a1.test(s1, s1)=false")
	void aliasBetweenString2() throws CodeExecutionException, TransactionException {
		ClassType alias = new ClassType("takamaka.tests.basicdependency.Alias");
		StorageReference a1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		StringValue s1 = new StringValue("hello");
		assertEquals(new BooleanValue(false), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(alias, "test", ClassType.STRING, ClassType.STRING), a1, s1, s1)));
	}

	@Test @DisplayName("a1 = new Alias(); bi1 = BigInteger.valueOf(13L); bi2 = BigInteger.valueOf(13L); a1.test(bi1, bi2)=false")
	void aliasBetweenBigInteger1() throws CodeExecutionException, TransactionException {
		ClassType alias = new ClassType("takamaka.tests.basicdependency.Alias");
		StorageReference a1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		BigIntegerValue bi1 = new BigIntegerValue(BigInteger.valueOf(13L));
		BigIntegerValue bi2 = new BigIntegerValue(BigInteger.valueOf(13L));
		assertEquals(new BooleanValue(false), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(alias, "test", ClassType.BIG_INTEGER, ClassType.BIG_INTEGER),
			a1, bi1, bi2)));
	}

	@Test @DisplayName("a1 = new Alias(); bi1 = BigInteger.valueOf(13L); a1.test(bi1, bi2)=false")
	void aliasBetweenBigInteger2() throws CodeExecutionException, TransactionException {
		ClassType alias = new ClassType("takamaka.tests.basicdependency.Alias");
		StorageReference a1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_ALIAS));
		BigIntegerValue bi1 = new BigIntegerValue(BigInteger.valueOf(13L));
		assertEquals(new BooleanValue(false), blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(alias, "test", ClassType.BIG_INTEGER, ClassType.BIG_INTEGER),
			a1, bi1, bi1)));
	}

	@Test @DisplayName("new Simple(13).foo1() throws TransactionException since SideEffectsInViewMethodException")
	void viewMethodViolation1() throws CodeExecutionException, TransactionException {
		ClassType simple = new ClassType("takamaka.tests.basic.Simple");
		StorageReference s = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(simple, BasicTypes.INT), new IntValue(13)));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(simple, "foo1"), s));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof SideEffectsInViewMethodException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Simple(13).foo2() throws TransactionException since SideEffectsInViewMethodException")
	void viewMethodViolation2() throws CodeExecutionException, TransactionException {
		ClassType simple = new ClassType("takamaka.tests.basic.Simple");
		StorageReference s = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(simple, BasicTypes.INT), new IntValue(13)));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(simple, "foo2"), s));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof SideEffectsInViewMethodException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Simple(13).foo3() == 13")
	void viewMethodOk1() throws CodeExecutionException, TransactionException {
		ClassType simple = new ClassType("takamaka.tests.basic.Simple");
		StorageReference s = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(simple, BasicTypes.INT), new IntValue(13)));

		assertEquals(new IntValue(13),
			blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(simple, "foo3"), s)));
	}

	@Test @DisplayName("new Simple(13).foo4() == 13")
	void viewMethodOk2() throws CodeExecutionException, TransactionException {
		ClassType simple = new ClassType("takamaka.tests.basic.Simple");
		StorageReference s = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(simple, BasicTypes.INT), new IntValue(13)));

		assertEquals(new IntValue(13),
			blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(simple, "foo4"), s)));
	}

	@Test @DisplayName("new Simple(13).foo5() == 13")
	void viewMethodOk3() throws CodeExecutionException, TransactionException {
		ClassType simple = new ClassType("takamaka.tests.basic.Simple");
		assertEquals(new IntValue(14),
			blockchain.addStaticMethodCallTransaction
			(new StaticMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(simple, "foo5"))));
	}

	@Test @DisplayName("new WithList().toString().equals(\"[hello,how,are,you]\")")
	void listCreation() throws CodeExecutionException, TransactionException {
		ClassType withList = new ClassType("takamaka.tests.basic.WithList");
		StorageReference wl = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(withList)));
		assertEquals(new StringValue("[hello,how,are,you]"),
			blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature(withList, "toString"),
			wl)));
	}

	@Test @DisplayName("new WithList().illegal() throws TransactionException since DeserializationError")
	void deserializationError() throws CodeExecutionException, TransactionException {
		ClassType withList = new ClassType("takamaka.tests.basic.WithList");
		StorageReference wl = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(withList)));
		
		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature(withList, "illegal"),
				wl));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof DeserializationError)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new EntryFilter().foo1() called by an ExternallyOwnedAccount")
	void entryFilterOk1() throws CodeExecutionException, TransactionException {
		ClassType entryFilter = new ClassType("takamaka.tests.basic.EntryFilter");
		StorageReference ef = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(entryFilter)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(entryFilter, "foo1"), ef));
	}

	@Test @DisplayName("new EntryFilter().foo2() called by an ExternallyOwnedAccount")
	void entryFilterOk2() throws CodeExecutionException, TransactionException {
		ClassType entryFilter = new ClassType("takamaka.tests.basic.EntryFilter");
		StorageReference ef = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(entryFilter)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(entryFilter, "foo2"), ef));
	}

	@Test @DisplayName("new EntryFilter().foo3() called by an ExternallyOwnedAccount")
	void entryFilterOk3() throws CodeExecutionException, TransactionException {
		ClassType entryFilter = new ClassType("takamaka.tests.basic.EntryFilter");
		StorageReference ef = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(entryFilter)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(entryFilter, "foo3"), ef));
	}

	@Test @DisplayName("new EntryFilter().foo4() called by an ExternallyOwnedAccount throws TransactionException since ClassCastException")
	void entryFilterFails() throws CodeExecutionException, TransactionException {
		ClassType entryFilter = new ClassType("takamaka.tests.basic.EntryFilter");
		StorageReference ef = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(entryFilter)));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(entryFilter, "foo4"), ef));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof ClassCastException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new EntryFilter().foo5() called by an ExternallyOwnedAccount throws CodeExecutionException since ClassCastException")
	void entryFilterFailsWothThrowsExceptions() throws CodeExecutionException, TransactionException {
		ClassType entryFilter = new ClassType("takamaka.tests.basic.EntryFilter");
		StorageReference ef = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, new ConstructorSignature(entryFilter)));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(entryFilter, "foo5"), ef));
		}
		catch (CodeExecutionException e) {
			if (e.getCause() instanceof ClassCastException)
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}
}