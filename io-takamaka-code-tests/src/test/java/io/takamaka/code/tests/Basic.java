/**
 * 
 */
package io.takamaka.code.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static io.hotmoka.beans.types.BasicTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.takamaka.code.constants.Constants;

/**
 * A test for basic storage and contract Takamaka classes.
 */
class Basic extends TakamakaTest {
	private static final ClassType ALIAS = new ClassType("io.takamaka.tests.basicdependency.Alias");
	private static final ClassType SIMPLE = new ClassType("io.takamaka.tests.basic.Simple");
	private static final ClassType WITH_LIST = new ClassType("io.takamaka.tests.basic.WithList");
	private static final ClassType ENTRY_FILTER = new ClassType("io.takamaka.tests.basic.EntryFilter");
	private static final BigInteger _5_000 = BigInteger.valueOf(5000);
	private static final ConstructorSignature CONSTRUCTOR_ALIAS = new ConstructorSignature(ALIAS);
	private static final MethodSignature PAYABLE_CONTRACT_RECEIVE = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", INT);
	private static final MethodSignature SUB_MS = new VoidMethodSignature("io.takamaka.tests.basic.Sub", "ms");
	private static final MethodSignature SUB_M5 = new VoidMethodSignature("io.takamaka.tests.basic.Sub", "m5");
	private static final ConstructorSignature CONSTRUCTOR_WRAPPER_1 = new ConstructorSignature("io.takamaka.tests.basicdependency.Wrapper", new ClassType("io.takamaka.tests.basicdependency.Time"));
	private static final ConstructorSignature CONSTRUCTOR_WRAPPER_2 = new ConstructorSignature("io.takamaka.tests.basicdependency.Wrapper", new ClassType("io.takamaka.tests.basicdependency.Time"), ClassType.STRING, ClassType.BIG_INTEGER, BasicTypes.LONG);
	private static final ConstructorSignature CONSTRUCTOR_INTERNATIONAL_TIME = new ConstructorSignature("io.takamaka.tests.basicdependency.InternationalTime", INT, INT, INT);
	private static final MethodSignature TIME_TO_STRING = new NonVoidMethodSignature(new ClassType("io.takamaka.tests.basicdependency.Time"), "toString", ClassType.STRING);
	private static final MethodSignature WRAPPER_TO_STRING = new NonVoidMethodSignature(new ClassType("io.takamaka.tests.basicdependency.Wrapper"), "toString", ClassType.STRING);
	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);

	/**
	 * The account that holds all funds.
	 */
	private StorageReference master;

	/**
	 * The classpath of the classes being tested.
	 */
	private TransactionReference classpath;

	/**
	 * The private key of {@linkplain #master}.
	 */
	private PrivateKey key;

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("basicdependency.jar", ALL_FUNDS, BigInteger.ZERO);
		master = account(0);
		key = privateKey(0);
		// true relevant below
		classpath = addJarStoreTransaction(key, master, BigInteger.valueOf(10000), BigInteger.ONE, takamakaCode(), bytesOf("basic.jar"), jar());
	}

	@Test @DisplayName("new InternationalTime(13,25,40).toString().equals(\"13:25:40\")")
	void testToStringInternationTime() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME, new IntValue(13), new IntValue(25), new IntValue(40));
		assertEquals(new StringValue("13:25:40"), runViewInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, TIME_TO_STRING, internationalTime));
	}

	@Test @DisplayName("new Wrapper(new InternationalTime(13,25,40)).toString().equals(\"wrapper(13:25:40,null,null,0)\")")
	void testToStringWrapperInternationTime1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40));
		StorageReference wrapper = addConstructorCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, CONSTRUCTOR_WRAPPER_1, internationalTime);
		assertEquals(new StringValue("wrapper(13:25:40,null,null,0)"),
			runViewInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, WRAPPER_TO_STRING, wrapper));
	}

	@Test @DisplayName("new Wrapper(new InternationalTime(13,25,40),\"hello\",13011973,12345L).toString().equals(\"wrapper(13:25:40,hello,13011973,12345)\")")
	void testToStringWrapperInternationTime2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40));
		StorageReference wrapper = addConstructorCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, CONSTRUCTOR_WRAPPER_2,
			internationalTime, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(13011973)), new LongValue(12345L));
		assertEquals(new StringValue("wrapper(13:25:40,hello,13011973,12345)"),
			runViewInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, WRAPPER_TO_STRING, wrapper));
	}

	@Test @DisplayName("new Sub(1973)")
	void callPayableConstructor() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973));
	}

	@Test @DisplayName("new Sub().m1() throws TransactionException since RequirementViolationException")
	void callEntryFromSameContract() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference sub = addConstructorCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub"));

		try {
			runViewInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, new VoidMethodSignature("io.takamaka.tests.basic.Sub", "m1"), sub);
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME) && e.getMessage().endsWith("An @Entry can only be called from a distinct contract object@Sub.java:21"))
				return;

			fail("wrong exception");
		}

		fail("expected exception");
	}

	@Test @DisplayName("new Sub().ms() throws TransactionException since NoSuchMethodException")
	void callStaticAsInstance() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference sub = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub"));

		throwsTransactionExceptionWithCause(NoSuchMethodException.class, () ->
			runViewInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, SUB_MS, sub)
		);
	}

	@Test @DisplayName("Sub.ms()")
	void callStaticAsStatic() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		runViewStaticMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, SUB_MS);
	}

	@Test @DisplayName("Sub.m5() throws TransactionException since NoSuchMethodException")
	void callInstanceAsStatic() throws CodeExecutionException, TransactionException {
		throwsTransactionExceptionWithCause(NoSuchMethodException.class, () ->
			runViewStaticMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, SUB_M5)
		);
	}

	@Test @DisplayName("new Sub(1973) without gas")
	void callerHasNotEnoughFundsForGas() {
		throwsTransactionRejectedException(() ->
			addConstructorCallTransaction(privateKey(1), account(1), _200_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973))
		);
	}

	@Test @DisplayName("new Sub(1973) with gas but without enough coins to pay the @Entry")
	void callerHasNotEnoughFundsForPayableEntry() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, PAYABLE_CONTRACT_RECEIVE, account(1), new IntValue(200000));

		throwsTransactionExceptionWithCause(Constants.INSUFFICIENT_FUNDS_ERROR_NAME, () ->
			addConstructorCallTransaction(privateKey(1), account(1), _200_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973))
		);
	}

	@Test @DisplayName("new Sub(1973) with gas and enough coins to pay the @Entry")
	void callerHasEnoughFundsForPayableEntry() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		postInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, PAYABLE_CONTRACT_RECEIVE, account(1), new IntValue(20000));
		addConstructorCallTransaction(privateKey(1), account(1), _5_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973));
	}

	@Test @DisplayName("new Sub(1973).print(new InternationalTime(13,25,40))")
	void callInstanceMethod() throws CodeExecutionException, TransactionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		postInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, PAYABLE_CONTRACT_RECEIVE, account(1), new IntValue(20000));
		CodeSupplier<StorageReference> internationalTime = postConstructorCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			new IntValue(13), new IntValue(25), new IntValue(40));
		StorageReference sub = addConstructorCallTransaction
			(privateKey(1), account(1), _5_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973));
		addInstanceMethodCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, new VoidMethodSignature("io.takamaka.tests.basic.Sub", "print", new ClassType("io.takamaka.tests.basicdependency.Time")), sub, internationalTime.get());
	}

	@Test @DisplayName("new Sub(1973).m4(13).equals(\"Sub.m4 receives 13 coins from an externally owned account with public balance\")")
	void callPayableEntryWithInt() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		postInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, PAYABLE_CONTRACT_RECEIVE, account(1), new IntValue(20000));
		StorageReference sub = addConstructorCallTransaction
			(privateKey(1), account(1), _5_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973));
		assertEquals(new StringValue("Sub.m4 receives 13 coins from an externally owned account with public balance"), addInstanceMethodCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, new NonVoidMethodSignature("io.takamaka.tests.basic.Sub", "m4", ClassType.STRING, INT), sub, new IntValue(13)));
	}

	@Test @DisplayName("new Sub(1973).m4_1(13L).equals(\"Sub.m4_1 receives 13 coins from an externally owned account with public balance\")")
	void callPayableEntryWithLong() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		postInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, PAYABLE_CONTRACT_RECEIVE, account(1), new IntValue(2000000));
		StorageReference sub = addConstructorCallTransaction
			(privateKey(1), account(1), _200_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973));
		assertEquals(new StringValue("Sub.m4_1 receives 13 coins from an externally owned account with public balance"),
			addInstanceMethodCallTransaction
				(key, master, _200_000, BigInteger.ONE, classpath, new NonVoidMethodSignature("io.takamaka.tests.basic.Sub", "m4_1", ClassType.STRING, LONG), sub, new LongValue(13L)));
	}

	@Test @DisplayName("new Sub(1973).m4_2(BigInteger.valueOf(13)).equals(\"Sub.m4_2 receives 13 coins from an externally owned account with public balance\")")
	void callPayableEntryWithBigInteger() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		postInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, PAYABLE_CONTRACT_RECEIVE, account(1), new IntValue(20000));
		StorageReference sub = addConstructorCallTransaction(privateKey(1), account(1), _5_000, BigInteger.ONE, classpath, new ConstructorSignature("io.takamaka.tests.basic.Sub", INT), new IntValue(1973));
		assertEquals(new StringValue("Sub.m4_2 receives 13 coins from an externally owned account with public balance"),
			addInstanceMethodCallTransaction
			(key, master, _200_000, BigInteger.ONE, classpath, new NonVoidMethodSignature("io.takamaka.tests.basic.Sub", "m4_2", ClassType.STRING, ClassType.BIG_INTEGER),
			sub, new BigIntegerValue(BigInteger.valueOf(13L))));
	}

	@Test @DisplayName("a1 = new Alias(); a2 = new Alias(); a1.test(a1, a2)=false")
	void aliasBetweenStorage1() throws CodeExecutionException, TransactionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> a1 = postConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		StorageReference a2 = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		assertEquals(new BooleanValue(false), runViewInstanceMethodCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(ALIAS, "test", BasicTypes.BOOLEAN, ALIAS, ALIAS), a1.get(), a1.get(), a2));
	}

	@Test @DisplayName("a1 = new Alias(); a1.test(a1, a1)=true")
	void aliasBetweenStorage2() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference a1 = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		assertEquals(new BooleanValue(true), runViewInstanceMethodCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(ALIAS, "test", BasicTypes.BOOLEAN, ALIAS, ALIAS), a1, a1, a1));
	}

	@Test @DisplayName("a1 = new Alias(); s1 = \"hello\"; s2 = \"hello\"; a1.test(s1, s2)=false")
	void aliasBetweenString() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference a1 = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		StringValue s1 = new StringValue("hello");
		StringValue s2 = new StringValue("hello");
		assertEquals(new BooleanValue(false), runViewInstanceMethodCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(ALIAS, "test", BasicTypes.BOOLEAN, ClassType.STRING, ClassType.STRING), a1, s1, s2));
	}

	@Test @DisplayName("a1 = new Alias(); s1 = \"hello\"; a1.test(s1, s1)=false")
	void aliasBetweenString2() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference a1 = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		StringValue s1 = new StringValue("hello");
		assertEquals(new BooleanValue(false), runViewInstanceMethodCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(ALIAS, "test", BasicTypes.BOOLEAN, ClassType.STRING, ClassType.STRING), a1, s1, s1));
	}

	@Test @DisplayName("a1 = new Alias(); bi1 = BigInteger.valueOf(13L); bi2 = BigInteger.valueOf(13L); a1.test(bi1, bi2)=false")
	void aliasBetweenBigInteger1() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference a1 = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		BigIntegerValue bi1 = new BigIntegerValue(BigInteger.valueOf(13L));
		BigIntegerValue bi2 = new BigIntegerValue(BigInteger.valueOf(13L));
		assertEquals(new BooleanValue(false), runViewInstanceMethodCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(ALIAS, "test", BasicTypes.BOOLEAN, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER), a1, bi1, bi2));
	}

	@Test @DisplayName("a1 = new Alias(); bi1 = BigInteger.valueOf(13L); a1.test(bi1, bi2)=false")
	void aliasBetweenBigInteger2() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference a1 = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, CONSTRUCTOR_ALIAS);
		BigIntegerValue bi1 = new BigIntegerValue(BigInteger.valueOf(13L));
		assertEquals(new BooleanValue(false), runViewInstanceMethodCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(ALIAS, "test", BasicTypes.BOOLEAN, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER), a1, bi1, bi1));
	}

	@Test @DisplayName("new Simple(13).foo1() throws TransactionException since SideEffectsInViewMethodException")
	void viewMethodViolation1() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference s = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(SIMPLE, INT), new IntValue(13));

		throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () ->
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(SIMPLE, "foo1"), s)
		);
	}

	@Test @DisplayName("new Simple(13).foo2() throws TransactionException since SideEffectsInViewMethodException")
	void viewMethodViolation2() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference s = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(SIMPLE, INT), new IntValue(13));

		throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () ->
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE, "foo2", SIMPLE), s)
		);
	}

	@Test @DisplayName("new Simple(13).foo3() == 13")
	void viewMethodOk1() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference s = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(SIMPLE, INT), new IntValue(13));

		assertEquals(new IntValue(13),
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE, "foo3", INT), s));
	}

	@Test @DisplayName("new Simple(13).foo4() == 13")
	void viewMethodOk2() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference s = addConstructorCallTransaction
			(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(SIMPLE, BasicTypes.INT), new IntValue(13));

		assertEquals(new IntValue(13),
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE, "foo4", INT), s));
	}

	@Test @DisplayName("new Simple(13).foo5() == 13")
	void viewMethodOk3() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		assertEquals(new IntValue(14),
			runViewStaticMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE, "foo5", INT)));
	}

	@Test @DisplayName("new WithList().toString().equals(\"[hello,how,are,you]\")")
	void listCreation() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference wl = addConstructorCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, new ConstructorSignature(WITH_LIST));
		assertEquals(new StringValue("[hello,how,are,you]"),
			runViewInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(WITH_LIST, "toString", ClassType.STRING), wl));
	}

	@Test @DisplayName("new WithList().illegal() throws TransactionException since DeserializationError")
	void deserializationError() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference wl = addConstructorCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, new ConstructorSignature(WITH_LIST));
		
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			addInstanceMethodCallTransaction(key, master, _200_000, BigInteger.ONE, classpath, new VoidMethodSignature(WITH_LIST, "illegal"), wl)
		);
	}

	@Test @DisplayName("new EntryFilter().foo1() called by an ExternallyOwnedAccount")
	void entryFilterOk1() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ef = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(ENTRY_FILTER));
		runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(ENTRY_FILTER, "foo1"), ef);
	}

	@Test @DisplayName("new EntryFilter().foo2() called by an ExternallyOwnedAccount")
	void entryFilterOk2() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ef = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(ENTRY_FILTER));
		runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(ENTRY_FILTER, "foo2"), ef);
	}

	@Test @DisplayName("new EntryFilter().foo3() called by an ExternallyOwnedAccount")
	void entryFilterOk3() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ef = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(ENTRY_FILTER));
		runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(ENTRY_FILTER, "foo3"), ef);
	}

	@Test @DisplayName("new EntryFilter().foo4() called by an ExternallyOwnedAccount throws TransactionException since ClassCastException")
	void entryFilterFails() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ef = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(ENTRY_FILTER));

		throwsTransactionExceptionWithCause(ClassCastException.class, () ->
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(ENTRY_FILTER, "foo4"), ef)
		);
	}

	@Test @DisplayName("new EntryFilter().foo5() throws CodeExecutionException since MyCheckedException")
	void entryFilterFailsWithThrowsExceptions() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ef = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(ENTRY_FILTER));

		try {
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(ENTRY_FILTER, "foo5"), ef);
		}
		catch (CodeExecutionException e) {
			Assertions.assertTrue(e.getMessage().startsWith("io.takamaka.tests.basic.MyCheckedException"), "wrong exception");
			return;
		}

		fail("expected exception");
	}

	@Test @DisplayName("new EntryFilter().foo6() fails")
	void entryFilterFailsWithoutThrowsExceptions() throws CodeExecutionException, TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ef = addConstructorCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new ConstructorSignature(ENTRY_FILTER));

		Assertions.assertThrows(TransactionException.class, () ->
			runViewInstanceMethodCallTransaction(key, master, _5_000, BigInteger.ONE, classpath, new VoidMethodSignature(ENTRY_FILTER, "foo6"), ef));
	}
}