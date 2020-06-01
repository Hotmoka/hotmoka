/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.Node.CodeSupplier;

/**
 * A test for the storage map Takamaka class.
 */
class StorageMap extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20000);
	private static final ClassType STORAGE_MAP = ClassType.STORAGE_MAP;
	private static final NonVoidMethodSignature STORAGE_MAP_ISEMPTY = new NonVoidMethodSignature(STORAGE_MAP, "isEmpty", BOOLEAN);
	private static final NonVoidMethodSignature STORAGE_MAP_MIN = new NonVoidMethodSignature(STORAGE_MAP, "min", ClassType.OBJECT);
	private static final NonVoidMethodSignature STORAGE_MAP_SIZE = new NonVoidMethodSignature(STORAGE_MAP, "size", INT);
	private static final VoidMethodSignature STORAGE_MAP_PUT = new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT);
	private static final ConstructorSignature CONSTRUCTOR_STORAGE_MAP = new ConstructorSignature(STORAGE_MAP);
	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(100_000_000);
	private static final StorageValue ONE = new BigIntegerValue(BigInteger.ONE);
	private static final StorageValue TWO = new BigIntegerValue(BigInteger.valueOf(2L));

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference account0;

	/**
	 * The classpath of the classes being tested.
	 */
	private TransactionReference classpath;

	/**
	 * The private key of {@linkplain #account0}.
	 */
	private PrivateKey key;

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(ALL_FUNDS);
		classpath = takamakaCode();
		account0 = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("new StorageMap()")
	void constructionSucceeds() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
	}

	@Test @DisplayName("new StorageMap().size() == 0")
	void sizeIsInitially0() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
		IntValue size = (IntValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(new IntValue(0), size);
	}

	@Test @DisplayName("new StorageMap().isEmpty() == true")
	void mapIsInitiallyEmpty() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
		BooleanValue size = (BooleanValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_ISEMPTY, map);

		assertEquals(BooleanValue.TRUE, size);
	}

	@Test @DisplayName("new StorageMap().put(k,v) then get(k) yields v")
	void putThenGet() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
		StorageReference eoa = addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));
		addInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map.get(), eoa, ONE);
		BigIntegerValue get = (BigIntegerValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map.get(), eoa);

		assertEquals(ONE, get);
	}

	@Test @DisplayName("new StorageMap().put(k1,v) then get(k2) yields null")
	void putThenGetWithOtherKey() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
		CodeSupplier<StorageReference> eoa1 = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));
		CodeSupplier<StorageReference> eoa2 = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));
		addInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map.get(), eoa1.get(), ONE);
		StorageValue get = (StorageValue) runViewInstanceMethodCallTransaction
			(key, account0, _20_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map.get(), eoa2.get());

		assertEquals(NullValue.INSTANCE, get);
	}

	@Test @DisplayName("new StorageMap().put(k1,v) then get(k2, _default) yields default")
	void putThenGetWithOtherKeyAndDefaultValue() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
		CodeSupplier<StorageReference> eoa1 = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));
		CodeSupplier<StorageReference> eoa2 = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));
		addInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map.get(), eoa1.get(), ONE);
		StorageValue get = (StorageValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(STORAGE_MAP, "getOrDefault", ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT), map.get(), eoa2.get(), TWO);

		assertEquals(TWO, get);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys then size is 100")
	void put100RandomThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);

		CodeSupplier<?> accounts[] = new CodeSupplier<?>[100];
		for (int i = 0; i < 100; i++)
			accounts[i] = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));

		Random random = new Random();
		VoidMethodSignature put = STORAGE_MAP_PUT;
		CodeSupplier<StorageValue> future = null;
		for (int i = 0; i < 100; i++)
			future = postInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, put, map.get(), accounts[i].get(), new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

		future.get(); // we wait until everything has been committed
		IntValue size = (IntValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_SIZE, map.get());

		assertEquals(100, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 times the same key then size is 1")
	void put100TimesSameKeyThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);
		StorageReference eoa = addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));

		Random random = new Random();
		CodeSupplier<StorageValue> future = null;
		for (int i = 0; i < 100; i++)
			future = postInstanceMethodCallTransaction
				(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT,
					map.get(), eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

		future.get(); // we wait until everything has been committed
		IntValue size = (IntValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_SIZE, map.get());

		assertEquals(1, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 times equal string keys then size is 1")
	void put100TimesEqualStringThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		StorageReference map = addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);

		Random random = new Random();
		CodeSupplier<StorageValue> future = null;
		for (int i = 0; i < 100; i++)
			future = postInstanceMethodCallTransaction
				(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT,
					map, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

		future.get(); // we wait until everything has been committed
		IntValue size = (IntValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(1, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 random BigInteger keys then min key is correct")
	void min() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		StorageReference map = addConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);

		Random random = new Random();
		BigInteger min = null;
		CodeSupplier<StorageValue> future = null;
		for (int i = 0; i < 100; i++) {
			BigInteger bi = BigInteger.valueOf(random.nextLong()); 
			if (min == null || bi.compareTo(min) < 0)
				min = bi;

			future = postInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, new BigIntegerValue(bi), new StringValue("hello"));
		}

		future.get(); // we wait until everything has been committed
		BigIntegerValue result = (BigIntegerValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_MIN, map);

		assertEquals(min, result.value);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys then remove the last then size is 99")
	void put100RandomThenRemoveLastThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);

		CodeSupplier<?> accounts[] = new CodeSupplier<?>[100];
		for (int i = 0; i < 100; i++)
			accounts[i] = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));

		Random random = new Random();
		int i = 0;
		do {
			postInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT,
					map.get(), accounts[i].get(), new BigIntegerValue(BigInteger.valueOf(random.nextLong())));
		}
		while (++i < 100);

		VoidMethodSignature STORAGE_MAP_REMOVE = new VoidMethodSignature(STORAGE_MAP, "remove", ClassType.OBJECT);
		addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_REMOVE, map.get(), accounts[random.nextInt(100)].get());
		IntValue size = (IntValue) runViewInstanceMethodCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, STORAGE_MAP_SIZE, map.get());

		assertEquals(99, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys and checks contains after each put")
	void put100RandomEachTimeCheckContains() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> map = postConstructorCallTransaction(key, account0, _20_000, BigInteger.ONE, classpath, CONSTRUCTOR_STORAGE_MAP);

		CodeSupplier<?> accounts[] = new CodeSupplier<?>[100];
		for (int i = 0; i < 100; i++)
			accounts[i] = postConstructorCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA));

		List<CodeSupplier<?>> results = new ArrayList<>();

		Random random = new Random();
		for (int i = 0; i < 100; i++) {
			postInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map.get(), accounts[i].get(), new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

			results.add(postInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(STORAGE_MAP, "contains", BOOLEAN, ClassType.OBJECT), map.get(), accounts[i].get()));
		}

		for (CodeSupplier<?> result: results)
			assertTrue(((BooleanValue) result.get()).value);
	}
}