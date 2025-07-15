/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests;

import static io.hotmoka.node.MethodSignatures.RECEIVE_INT;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.LONG;
import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.security.PrivateKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.local.SideEffectsInViewMethodException;
import io.hotmoka.node.local.UnmatchedTargetException;
import io.takamaka.code.constants.Constants;

/**
 * A test for basic storage and contract Takamaka classes.
 */
class Basic extends HotmokaTest {
	private static final ClassType ALIAS = StorageTypes.classNamed("io.hotmoka.examples.basicdependency.Alias");
	private static final ClassType SIMPLE = StorageTypes.classNamed("io.hotmoka.examples.basic.Simple");
	private static final ClassType WITH_LIST = StorageTypes.classNamed("io.hotmoka.examples.basic.WithList");
	private static final ClassType FROM_CONTRACT_FILTER = StorageTypes.classNamed("io.hotmoka.examples.basic.FromContractFilter");
	private static final ConstructorSignature CONSTRUCTOR_ALIAS = ConstructorSignatures.of(ALIAS);
	private static final ClassType SUB = StorageTypes.classNamed("io.hotmoka.examples.basic.Sub");
	private static final ClassType WRAPPER = StorageTypes.classNamed("io.hotmoka.examples.basicdependency.Wrapper");
	private static final VoidMethodSignature SUB_MS = MethodSignatures.ofVoid(SUB, "ms");
	private static final VoidMethodSignature SUB_M5 = MethodSignatures.ofVoid(SUB, "m5");
	private static final ConstructorSignature CONSTRUCTOR_WRAPPER_1 = ConstructorSignatures.of(WRAPPER, StorageTypes.classNamed("io.hotmoka.examples.basicdependency.Time"));
	private static final ConstructorSignature CONSTRUCTOR_WRAPPER_2 = ConstructorSignatures.of(WRAPPER, StorageTypes.classNamed("io.hotmoka.examples.basicdependency.Time"), StorageTypes.STRING, StorageTypes.BIG_INTEGER, StorageTypes.LONG);
	private static final ConstructorSignature CONSTRUCTOR_INTERNATIONAL_TIME = ConstructorSignatures.of(StorageTypes.classNamed("io.hotmoka.examples.basicdependency.InternationalTime"), INT, INT, INT);
	private static final NonVoidMethodSignature TIME_TO_STRING = MethodSignatures.ofNonVoid(StorageTypes.classNamed("io.hotmoka.examples.basicdependency.Time"), "toString", StorageTypes.STRING);
	private static final NonVoidMethodSignature WRAPPER_TO_STRING = MethodSignatures.ofNonVoid(WRAPPER, "toString", StorageTypes.STRING);
	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	/**
	 * The account that holds all funds.
	 */
	private StorageReference master;

	/**
	 * The classpath of the classes being tested.
	 */
	private TransactionReference classpath;

	/**
	 * The private key of {@link #master}.
	 */
	private PrivateKey key;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("basicdependency.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.ZERO);
		master = account(0);
		key = privateKey(0);
		classpath = addJarStoreTransaction(key, master, _1_000_000, ONE, takamakaCode(), bytesOf("basic.jar"), jar());
	}

	@Test @DisplayName("new InternationalTime(13,25,40).toString().equals(\"13:25:40\")")
	void testToStringInternationTime() throws Exception {
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME, StorageValues.intOf(13), StorageValues.intOf(25), StorageValues.intOf(40));
		assertEquals(StorageValues.stringOf("13:25:40"), runInstanceNonVoidMethodCallTransaction(master, _200_000, classpath, TIME_TO_STRING, internationalTime));
	}

	@Test @DisplayName("new Wrapper(new InternationalTime(13,25,40)).toString().equals(\"wrapper(13:25:40,null,null,0)\")")
	void testToStringWrapperInternationTime1() throws Exception {
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			StorageValues.intOf(13), StorageValues.intOf(25), StorageValues.intOf(40));
		StorageReference wrapper = addConstructorCallTransaction(key, master, _200_000, ONE, classpath, CONSTRUCTOR_WRAPPER_1, internationalTime);
		assertEquals(StorageValues.stringOf("wrapper(13:25:40,null,null,0)"),
			runInstanceNonVoidMethodCallTransaction(master, _200_000, classpath, WRAPPER_TO_STRING, wrapper));
	}

	@Test @DisplayName("new Wrapper(new InternationalTime(13,25,40),\"hello\",13011973,12345L).toString().equals(\"wrapper(13:25:40, IllegalArgumentException::new,hello,13011973,12345)\")")
	void testToStringWrapperInternationTime2() throws Exception {
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			StorageValues.intOf(13), StorageValues.intOf(25), StorageValues.intOf(40));
		StorageReference wrapper = addConstructorCallTransaction
			(key, master, _200_000, ONE, classpath, CONSTRUCTOR_WRAPPER_2,
			internationalTime, StorageValues.stringOf("hello"), StorageValues.bigIntegerOf(BigInteger.valueOf(13011973)), StorageValues.longOf(12345L));
		assertEquals(StorageValues.stringOf("wrapper(13:25:40,hello,13011973,12345)"),
			runInstanceNonVoidMethodCallTransaction(master, _200_000, classpath, WRAPPER_TO_STRING, wrapper));
	}

	@Test @DisplayName("new Sub(1973)")
	void callPayableConstructor() throws Exception {
		addConstructorCallTransaction(key, master, _200_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973));
	}

	@Test @DisplayName("new Sub().m1() succeeds in calling an entry from same contract")
	void callEntryFromSameContract() throws Exception {
		StorageReference sub = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(SUB));
		runInstanceVoidMethodCallTransaction(master, _200_000, classpath, MethodSignatures.ofVoid(SUB, "m1"), sub);
	}

	@Test @DisplayName("new Sub().ms() throws TransactionException since UnmatchedTargetException")
	void callStaticAsInstance() throws Exception {
		StorageReference sub = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(SUB));

		throwsTransactionExceptionWithCause(UnmatchedTargetException.class, () ->
			runInstanceVoidMethodCallTransaction(master, _200_000, classpath, SUB_MS, sub)
		);
	}

	@Test @DisplayName("Sub.ms()")
	void callStaticAsStatic() throws Exception {
		runStaticVoidMethodCallTransaction(master, _200_000, classpath, SUB_MS);
	}

	@Test @DisplayName("Sub.m5() throws TransactionException since NoSuchMethodException")
	void callInstanceAsStatic() {
		throwsTransactionExceptionWithCause(UnmatchedTargetException.class, () ->
			runStaticVoidMethodCallTransaction(master, _200_000, classpath, SUB_M5)
		);
	}

	@Test @DisplayName("new Sub(1973) without gas")
	void callerHasNotEnoughFundsForGas() {
		assertThrows(TransactionRejectedException.class, () ->
			addConstructorCallTransaction(privateKey(1), account(1), _200_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973))
		);
	}

	@Test @DisplayName("new Sub(1973) with gas but without enough coins to pay the @Entry")
	void callerHasNotEnoughFundsForPayableEntry() throws Exception {
		addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, RECEIVE_INT, account(1), StorageValues.intOf(200000));

		throwsTransactionExceptionWithCause(Constants.INSUFFICIENT_FUNDS_ERROR_NAME, () ->
			addConstructorCallTransaction(privateKey(1), account(1), _200_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973))
		);
	}

	@Test @DisplayName("new Sub(1973) with gas and enough coins to pay the @Entry")
	void callerHasEnoughFundsForPayableEntry() throws Exception {
		addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, RECEIVE_INT, account(1), StorageValues.intOf(200000));
		addConstructorCallTransaction(privateKey(1), account(1), _100_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973));
	}

	@Test @DisplayName("new Sub(1973).print(new InternationalTime(13,25,40))")
	void callInstanceMethod() throws Exception {
		addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, RECEIVE_INT, account(1), StorageValues.intOf(200000));
		StorageReference internationalTime = addConstructorCallTransaction
			(key, master, _200_000, ONE, classpath, CONSTRUCTOR_INTERNATIONAL_TIME,
			StorageValues.intOf(13), StorageValues.intOf(25), StorageValues.intOf(40));
		StorageReference sub = addConstructorCallTransaction
			(privateKey(1), account(1), _100_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973));
		addInstanceVoidMethodCallTransaction
			(key, master, _200_000, ONE, classpath,
			MethodSignatures.ofVoid(SUB, "print", StorageTypes.classNamed("io.hotmoka.examples.basicdependency.Time")), sub, internationalTime);
	}

	@Test @DisplayName("new Sub(1973).m4(13).equals(\"Sub.m4 receives 13 coins from an externally owned account with public balance\")")
	void callPayableEntryWithInt() throws Exception {
		addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, RECEIVE_INT, account(1), StorageValues.intOf(200000));
		StorageReference sub = addConstructorCallTransaction
			(privateKey(1), account(1), _100_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973));
		assertEquals(StorageValues.stringOf("Sub.m4 receives 13 coins from an externally owned account"), addInstanceNonVoidMethodCallTransaction
			(key, master, _200_000, ONE, classpath, MethodSignatures.ofNonVoid(SUB, "m4", StorageTypes.STRING, INT), sub, StorageValues.intOf(13)));
	}

	@Test @DisplayName("new Sub(1973).m4_1(13L).equals(\"Sub.m4_1 receives 13 coins from an externally owned account with public balance\")")
	void callPayableEntryWithLong() throws Exception {
		addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, RECEIVE_INT, account(1), StorageValues.intOf(2000000));
		StorageReference sub = addConstructorCallTransaction
			(privateKey(1), account(1), _200_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973));
		assertEquals(StorageValues.stringOf("Sub.m4_1 receives 13 coins from an externally owned account"),
			addInstanceNonVoidMethodCallTransaction
				(key, master, _200_000, ONE, classpath, MethodSignatures.ofNonVoid(SUB, "m4_1", StorageTypes.STRING, LONG), sub, StorageValues.longOf(13L)));
	}

	@Test @DisplayName("new Sub(1973).m4_2(BigInteger.valueOf(13)).equals(\"Sub.m4_2 receives 13 coins from an externally owned account with public balance\")")
	void callPayableEntryWithBigInteger() throws Exception {
		addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, RECEIVE_INT, account(1), StorageValues.intOf(200000));
		StorageReference sub = addConstructorCallTransaction(privateKey(1), account(1), _100_000, ONE, classpath, ConstructorSignatures.of(SUB, INT), StorageValues.intOf(1973));
		assertEquals(StorageValues.stringOf("Sub.m4_2 receives 13 coins from an externally owned account"),
			addInstanceNonVoidMethodCallTransaction
			(key, master, _200_000, ONE, classpath, MethodSignatures.ofNonVoid(SUB, "m4_2", StorageTypes.STRING, StorageTypes.BIG_INTEGER),
			sub, StorageValues.bigIntegerOf(BigInteger.valueOf(13L))));
	}

	@Test @DisplayName("a1 = new Alias(); a2 = new Alias(); a1.test(a1, a2)=false")
	void aliasBetweenStorage1() throws Exception {
		StorageReference a1 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		StorageReference a2 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		assertEquals(StorageValues.FALSE, runInstanceNonVoidMethodCallTransaction
			(master, _50_000, classpath, MethodSignatures.ofNonVoid(ALIAS, "test", StorageTypes.BOOLEAN, ALIAS, ALIAS), a1, a1, a2));
	}

	@Test @DisplayName("a1 = new Alias(); a1.test(a1, a1)=true")
	void aliasBetweenStorage2() throws Exception {
		StorageReference a1 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		assertEquals(StorageValues.TRUE, runInstanceNonVoidMethodCallTransaction
			(master, _50_000, classpath, MethodSignatures.ofNonVoid(ALIAS, "test", StorageTypes.BOOLEAN, ALIAS, ALIAS), a1, a1, a1));
	}

	@Test @DisplayName("a1 = new Alias(); s1 = \"hello\"; s2 = \"hello\"; a1.test(s1, s2)=false")
	void aliasBetweenString() throws Exception {
		StorageReference a1 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		StringValue s1 = StorageValues.stringOf("hello");
		StringValue s2 = StorageValues.stringOf("hello");
		assertEquals(StorageValues.FALSE, runInstanceNonVoidMethodCallTransaction
			(master, _50_000, classpath, MethodSignatures.ofNonVoid(ALIAS, "test", StorageTypes.BOOLEAN, StorageTypes.STRING, StorageTypes.STRING), a1, s1, s2));
	}

	@Test @DisplayName("a1 = new Alias(); s1 = \"hello\"; a1.test(s1, s1)=false")
	void aliasBetweenString2() throws Exception {
		StorageReference a1 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		StringValue s1 = StorageValues.stringOf("hello");
		assertEquals(StorageValues.FALSE, runInstanceNonVoidMethodCallTransaction
			(master, _50_000, classpath, MethodSignatures.ofNonVoid(ALIAS, "test", StorageTypes.BOOLEAN, StorageTypes.STRING, StorageTypes.STRING), a1, s1, s1));
	}

	@Test @DisplayName("a1 = new Alias(); bi1 = BigInteger.valueOf(13L); bi2 = BigInteger.valueOf(13L); a1.test(bi1, bi2)=false")
	void aliasBetweenBigInteger1() throws Exception {
		StorageReference a1 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		BigIntegerValue bi1 = StorageValues.bigIntegerOf(BigInteger.valueOf(13L));
		BigIntegerValue bi2 = StorageValues.bigIntegerOf(BigInteger.valueOf(13L));
		assertEquals(StorageValues.FALSE, runInstanceNonVoidMethodCallTransaction
			(master, _50_000, classpath, MethodSignatures.ofNonVoid(ALIAS, "test", StorageTypes.BOOLEAN, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER), a1, bi1, bi2));
	}

	@Test @DisplayName("a1 = new Alias(); bi1 = BigInteger.valueOf(13L); a1.test(bi1, bi2)=false")
	void aliasBetweenBigInteger2() throws Exception {
		StorageReference a1 = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, CONSTRUCTOR_ALIAS);
		BigIntegerValue bi1 = StorageValues.bigIntegerOf(BigInteger.valueOf(13L));
		assertEquals(StorageValues.FALSE, runInstanceNonVoidMethodCallTransaction
			(master, _50_000, classpath, MethodSignatures.ofNonVoid(ALIAS, "test", StorageTypes.BOOLEAN, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER), a1, bi1, bi1));
	}

	@Test @DisplayName("new Simple(13).foo1() throws TransactionException since SideEffectsInViewMethodException")
	void viewMethodViolation1() throws Exception {
		StorageReference s = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(SIMPLE, INT), StorageValues.intOf(13));

		throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () ->
			runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(SIMPLE, "foo1"), s)
		);
	}

	@Test @DisplayName("new Simple(13).foo2() throws TransactionException since SideEffectsInViewMethodException")
	void viewMethodViolation2() throws Exception {
		StorageReference s = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(SIMPLE, INT), StorageValues.intOf(13));

		throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () ->
			runInstanceNonVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofNonVoid(SIMPLE, "foo2", SIMPLE), s)
		);
	}

	@Test @DisplayName("new Simple(13).foo3() == 13")
	void viewMethodOk1() throws Exception {
		StorageReference s = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(SIMPLE, INT), StorageValues.intOf(13));

		assertEquals(StorageValues.intOf(13),
			runInstanceNonVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofNonVoid(SIMPLE, "foo3", INT), s));
	}

	@Test @DisplayName("new Simple(13).foo4() == 13")
	void viewMethodOk2() throws Exception {
		StorageReference s = addConstructorCallTransaction
			(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(SIMPLE, StorageTypes.INT), StorageValues.intOf(13));

		assertEquals(StorageValues.intOf(13),
			runInstanceNonVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofNonVoid(SIMPLE, "foo4", INT), s));
	}

	@Test @DisplayName("new Simple(13).foo5() == 13")
	void viewMethodOk3() throws Exception {
		assertEquals(StorageValues.intOf(14),
			runStaticNonVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofNonVoid(SIMPLE, "foo5", INT)));
	}

	@Test @DisplayName("new WithList().toString().equals(\"[hello,how,are,you]\")")
	void listCreation() throws Exception {
		StorageReference wl = addConstructorCallTransaction(key, master, _200_000, ONE, classpath, ConstructorSignatures.of(WITH_LIST));
		assertEquals(StorageValues.stringOf("[hello,how,are,you]"),
			runInstanceNonVoidMethodCallTransaction(master, _200_000, classpath, MethodSignatures.ofNonVoid(WITH_LIST, "toString", StorageTypes.STRING), wl));
	}

	@Test @DisplayName("new WithList().illegal() throws TransactionException since UpdatesExtractionException")
	void updatesExtractionException() throws Exception {
		StorageReference wl = addConstructorCallTransaction(key, master, _200_000, ONE, classpath, ConstructorSignatures.of(WITH_LIST));
		
		throwsTransactionExceptionWithCause(IllegalAssignmentToFieldInStorageException.class, () ->
			addInstanceVoidMethodCallTransaction(key, master, _200_000, ONE, classpath, MethodSignatures.ofVoid(WITH_LIST, "illegal"), wl)
		);
	}

	@Test @DisplayName("new FromContractFilter().foo1() called by an ExternallyOwnedAccount")
	void fromContractFilterOk1() throws Exception {
		StorageReference ef = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(FROM_CONTRACT_FILTER));
		runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(FROM_CONTRACT_FILTER, "foo1"), ef);
	}

	@Test @DisplayName("new FromContractFilter().foo2() called by an ExternallyOwnedAccount")
	void fromContractFilterOk2() throws Exception {
		StorageReference ef = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(FROM_CONTRACT_FILTER));
		runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(FROM_CONTRACT_FILTER, "foo2"), ef);
	}

	@Test @DisplayName("new FromContractFilter().foo3() called by an ExternallyOwnedAccount")
	void fromContractFilterOk3() throws Exception {
		StorageReference ef = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(FROM_CONTRACT_FILTER));
		runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(FROM_CONTRACT_FILTER, "foo3"), ef);
	}

	@Test @DisplayName("new FromContractFilter().foo4() called by an ExternallyOwnedAccount throws TransactionException since ClassCastException")
	void fromContractFilterFails() throws Exception {
		StorageReference ef = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(FROM_CONTRACT_FILTER));

		throwsTransactionExceptionWithCause(ClassCastException.class, () ->
			runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(FROM_CONTRACT_FILTER, "foo4"), ef)
		);
	}

	@Test @DisplayName("new FromContractFilter().foo5() throws CodeExecutionException since MyCheckedException")
	void fromContractFilterFailsWithThrowsExceptions() throws Exception {
		StorageReference ef = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(FROM_CONTRACT_FILTER));

		try {
			runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(FROM_CONTRACT_FILTER, "foo5"), ef);
		}
		catch (CodeExecutionException e) {
			Assertions.assertTrue(e.getMessage().startsWith("io.hotmoka.examples.basic.MyCheckedException"), "wrong exception");
			return;
		}

		fail("expected exception");
	}

	@Test @DisplayName("new FromContractFilter().foo6() fails")
	void fromContractFilterFailsWithoutThrowsExceptions() throws Exception {
		StorageReference ef = addConstructorCallTransaction(key, master, _50_000, ONE, classpath, ConstructorSignatures.of(FROM_CONTRACT_FILTER));

		Assertions.assertThrows(TransactionException.class, () ->
			runInstanceVoidMethodCallTransaction(master, _50_000, classpath, MethodSignatures.ofVoid(FROM_CONTRACT_FILTER, "foo6"), ef));
	}
}