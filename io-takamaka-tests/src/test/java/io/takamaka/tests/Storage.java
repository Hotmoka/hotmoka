/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A test for the simple storage class.
 */
class Storage extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final ClassType SIMPLE_STORAGE = new ClassType("io.takamaka.tests.storage.SimpleStorage");
	private static final VoidMethodSignature SET = new VoidMethodSignature(SIMPLE_STORAGE, "set", INT);
	private static final NonVoidMethodSignature GET = new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT);
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_STORAGE = new ConstructorSignature("io.takamaka.tests.storage.SimpleStorage");
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference eoa;

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("storage.jar", ALL_FUNDS);
		eoa = account(0);
	}

	@Test @DisplayName("new SimpleStorage().get() is an int")
	void neverInitializedStorageYieldsInt() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference storage = addConstructorCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		StorageValue value = runViewInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), GET, storage);
		assertTrue(value instanceof IntValue);
	}

	@Test @DisplayName("new SimpleStorage().get() == 0")
	void neverInitializedStorageYields0() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference storage = addConstructorCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		IntValue value = (IntValue) runViewInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), GET, storage);
		assertEquals(value.value, 0);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then get() == 13")
	void set13ThenGet13() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference storage = addConstructorCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		addInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), SET, storage, new IntValue(13));
		IntValue value = (IntValue) runViewInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), GET, storage);
		assertEquals(value.value, 13);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then set(17) then get() == 17")
	void set13set17ThenGet17() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference storage = addConstructorCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		postInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), SET, storage, new IntValue(13));
		addInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), SET, storage, new IntValue(17));
		IntValue value = (IntValue) runViewInstanceMethodCallTransaction(eoa, _10_000, BigInteger.ONE, jar(), GET, storage);
		assertEquals(value.value, 17);
	}
}