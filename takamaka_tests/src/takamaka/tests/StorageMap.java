/**
 * 
 */
package takamaka.tests;

import static io.takamaka.code.blockchain.types.BasicTypes.BOOLEAN;
import static io.takamaka.code.blockchain.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.code.blockchain.AbstractSequentialBlockchain;
import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.NonVoidMethodSignature;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.VoidMethodSignature;
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.BigIntegerValue;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.NullValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;
import io.takamaka.code.blockchain.values.StringValue;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for the storage map Takamaka class.
 */
class StorageMap extends TakamakaTest {

	private static final BigInteger _20_000 = BigInteger.valueOf(20000);

	private static final ClassType STORAGE_MAP = ClassType.STORAGE_MAP;

	private static final ConstructorSignature CONSTRUCTOR_STORAGE_MAP = new ConstructorSignature(STORAGE_MAP);

	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(100_000_000);

	private static final StorageValue ONE = new BigIntegerValue(BigInteger.ONE);

	private static final StorageValue TWO = new BigIntegerValue(BigInteger.valueOf(2L));

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialBlockchain blockchain;

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

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"))));
		classpath = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(classpath, ALL_FUNDS));
	}

	@Test @DisplayName("new StorageMap()")
	void constructionSucceeds() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));
	}

	@Test @DisplayName("new StorageMap().size() == 0")
	void sizeIsInitially0() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(new IntValue(0), size);
	}

	@Test @DisplayName("new StorageMap().isEmpty() == true")
	void mapIsInitiallyEmpty() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		BooleanValue size = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "isEmpty", BOOLEAN), map));

		assertEquals(BooleanValue.TRUE, size);
	}

	@Test @DisplayName("new StorageMap().put(k,v) then get(k) yields v")
	void putThenGet() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
				map, eoa, ONE));

		BigIntegerValue get = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map, eoa));

		assertEquals(ONE, get);
	}

	@Test @DisplayName("new StorageMap().put(k1,v) then get(k2) yields null")
	void putThenGetWithOtherKey() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

		StorageReference eoa2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
				map, eoa1, ONE));

		StorageValue get = (StorageValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map, eoa2));

		assertEquals(NullValue.INSTANCE, get);
	}

	@Test @DisplayName("new StorageMap().put(k1,v) then get(k2, _default) yields _default")
	void putThenGetWithOtherKeyAndDefaultValue() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

		StorageReference eoa2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
				map, eoa1, ONE));

		StorageValue get = (StorageValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "getOrDefault", ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT),
				map, eoa2, TWO));

		assertEquals(TWO, get);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys then size is 100")
	void put100RandomThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		for (int i = 0; i < 100; i++) {
			StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));
		}

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(100, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 times the same key then size is 1")
	void put100TimesSameKeyThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

		Random random = new Random();
		for (int i = 0; i < 100; i++)
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(1, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 times equal string keys then size is 1")
	void put100TimesEqualStringThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		for (int i = 0; i < 100; i++)
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(1, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 random BigInteger keys then min key is correct")
	void min() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		BigInteger min = null;
		for (int i = 0; i < 100; i++) {
			BigInteger bi = BigInteger.valueOf(random.nextLong()); 
			if (min == null || bi.compareTo(min) < 0)
				min = bi;

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, new BigIntegerValue(bi), new StringValue("hello")));
		}

		BigIntegerValue result = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "min", ClassType.OBJECT),
			map));

		assertEquals(min, result.value);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys then remove the last then size is 99")
	void put100RandomThenRemoveLastThenSize() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		StorageReference eoa;
		Random random = new Random();
		int i = 0;
		do {
			eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, _20_000, classpath, new ConstructorSignature(ClassType.EOA)));

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));
		}
		while (++i < 100);

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(STORAGE_MAP, "remove", ClassType.OBJECT),
			map, eoa));

		IntValue size = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "size", INT), map));

		assertEquals(99, size.value);
	}

	@Test @DisplayName("new StorageMap() put 100 storage keys and checks contains after each put")
	void put100RandomEachTimeCheckCOntains() throws TransactionException, CodeExecutionException {
		StorageReference map = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _20_000, classpath, CONSTRUCTOR_STORAGE_MAP));

		Random random = new Random();
		for (int i = 0; i < 100; i++) {
			StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, _100_000, classpath, new ConstructorSignature(ClassType.EOA)));

			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT),
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong()))));

			BooleanValue contains = (BooleanValue) blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "contains", BOOLEAN, ClassType.OBJECT),
				map, eoa));

			assertEquals(true, contains.value);
		}
	}
}