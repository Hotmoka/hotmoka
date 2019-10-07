/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static takamaka.blockchain.types.BasicTypes.BOOLEAN;
import static takamaka.blockchain.types.BasicTypes.INT;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.NonVoidMethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.VoidMethodSignature;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.NullValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.blockchain.values.StringValue;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the storage map Takamaka class.
 */
class StorageMap extends TakamakaTest {

	private static final BigInteger _1_000 = BigInteger.valueOf(1000);

	private static final ClassType STORAGE_MAP = new ClassType("takamaka.util.StorageMap");

	private static final ConstructorSignature CONSTRUCTOR_STORAGE_MAP = new ConstructorSignature("takamaka.util.StorageMap");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	private static final StorageValue ONE = new BigIntegerValue(BigInteger.ONE);

	private static final StorageValue TWO = new BigIntegerValue(BigInteger.valueOf(2L));

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
		classpath = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(classpath, ALL_FUNDS));
	}

	@Test @DisplayName("new StorageMap()")
	void constructionSucceeds() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));
	}

	@Test @DisplayName("new StorageMap().size() == 0")
	void sizeIsInitially0() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(new IntValue(0), size);
	}

	@Test @DisplayName("new StorageMap().isEmpty() == true")
	void mapIsInitiallyEmpty() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		BooleanValue size = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "isEmpty", BOOLEAN), map));

		assertEquals(BooleanValue.TRUE, size);
	}

	@Test @DisplayName("new StorageMap().put(k,v) then get(k) yields v")
	void putThenGet() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
				map, eoa, ONE));

		BigIntegerValue get = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map, eoa));

		assertEquals(ONE, get);
	}

	@Test @DisplayName("new StorageMap().put(k1,v) then get(k2) yields null")
	void putThenGetWithOtherKey() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		StorageReference eoa2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
				map, eoa1, ONE));

		StorageValue get = (StorageValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map, eoa2));

		assertEquals(NullValue.INSTANCE, get);
	}

	@Test @DisplayName("new StorageMap().put(k1,v) then get(k2, _default) yields _default")
	void putThenGetWithOtherKeyAndDefaultValue() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		StorageReference eoa2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
				map, eoa1, ONE));

		StorageValue get = (StorageValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "getOrDefault", ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT),
				map, eoa2, TWO));

		assertEquals(TWO, get);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys then size is 100")
	void put100RandomThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		for (int i = 0; i < 100; i++) {
			StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));
		}

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(100, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 times the same key then size is 1")
	void put100TimesSameKeyThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		Random random = new Random();
		for (int i = 0; i < 100; i++)
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(1, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 times equal string keys then size is 1")
	void put100TimesEqualStringThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		for (int i = 0; i < 100; i++)
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(1, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 random BigInteger keys then min key is correct")
	void min() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		BigInteger min = null;
		for (int i = 0; i < 100; i++) {
			BigInteger bi = BigInteger.valueOf(random.nextLong()); 
			if (min == null || bi.compareTo(min) < 0)
				min = bi;

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, new BigIntegerValue(bi), new StringValue("hello")));
		}

		BigIntegerValue result = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "min", ClassType.OBJECT),
			map));

		assertEquals(min, result.value);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys then remove the last then size is 99")
	void put100RandomThenRemoveLastThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa;
		Random random = new Random();
		int i = 0;
		do {
			eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));
		}
		while (++i < 100);

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "remove", ClassType.OBJECT),
			map, eoa));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(99, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys and checks contains after each put")
	void put100RandomEachTimeCheckCOntains() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		for (int i = 0; i < 100; i++) {
			StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));

			BooleanValue contains = (BooleanValue) blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "contains", BOOLEAN, ClassType.OBJECT),
				map, eoa));

			assertEquals(true, contains.value);
		}
	}
}