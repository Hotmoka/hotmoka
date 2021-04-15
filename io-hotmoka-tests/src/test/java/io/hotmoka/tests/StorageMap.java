package io.hotmoka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static io.hotmoka.beans.types.ClassType.MODIFIABLE_STORAGE_MAP;
import static io.hotmoka.beans.types.ClassType.STORAGE_MAP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
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

/**
 * A test for the storage map Takamaka class.
 */
class StorageMap extends TakamakaTest {
	private static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	private static final ConstructorSignature STORAGE_TREE_MAP_INIT = new ConstructorSignature("io.takamaka.code.util.StorageTreeMap");
	private static final NonVoidMethodSignature MK_EMPTY_EXPORTED_STORAGE_MAP = new NonVoidMethodSignature("io.hotmoka.examples.storagemap.ExportedStorageMapMaker", "mkEmptyExportedStorageMap", MODIFIABLE_STORAGE_MAP);
	private static final NonVoidMethodSignature STORAGE_MAP_ISEMPTY = new NonVoidMethodSignature(STORAGE_MAP, "isEmpty", BOOLEAN);
	private static final NonVoidMethodSignature STORAGE_MAP_MIN = new NonVoidMethodSignature(STORAGE_MAP, "min", ClassType.OBJECT);
	private static final NonVoidMethodSignature STORAGE_MAP_SIZE = new NonVoidMethodSignature(STORAGE_MAP, "size", INT);
	private static final NonVoidMethodSignature STORAGE_MAP_GET = new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT);
	private static final VoidMethodSignature MODIFIABLE_STORAGE_MAP_PUT = new VoidMethodSignature(MODIFIABLE_STORAGE_MAP, "put", ClassType.OBJECT, ClassType.OBJECT);
	private static final VoidMethodSignature MODIFIABLE_STORAGE_MAP_REMOVE = new VoidMethodSignature(MODIFIABLE_STORAGE_MAP, "remove", ClassType.OBJECT);
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
	 * The private key of {@link #account0}.
	 */
	private PrivateKey key;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("storagemap.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
		classpath = jar();
		account0 = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("new StorageTreeMap()")
	void constructionSucceeds() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
	}

	@Test @DisplayName("new StorageTreeMap().size() == 0")
	void sizeIsInitially0() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(new IntValue(0), size);
	}

	@Test @DisplayName("new StorageTreeMap().isEmpty() == true")
	void mapIsInitiallyEmpty() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
		BooleanValue size = (BooleanValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_ISEMPTY, map);

		assertEquals(BooleanValue.TRUE, size);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k,v) then get(k) yields v")
	void putThenGet() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
		StorageReference eoa = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT, map, eoa, ONE);
		BigIntegerValue get = (BigIntegerValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_GET, map, eoa);

		assertEquals(ONE, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k1,v) then get(k2) yields null")
	void putThenGetWithOtherKey() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys1 = signature().getKeyPair();
		String publicKey1 = Base64.getEncoder().encodeToString(keys1.getPublic().getEncoded());
		StorageReference eoa1 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey1));
		KeyPair keys2 = signature().getKeyPair();
		String publicKey2 = Base64.getEncoder().encodeToString(keys2.getPublic().getEncoded());
		StorageReference eoa2 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey2));
		addInstanceMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT, map, eoa1, ONE);
		StorageValue get = runInstanceMethodCallTransaction
			(account0, _50_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "get", ClassType.OBJECT, ClassType.OBJECT), map, eoa2);

		assertEquals(NullValue.INSTANCE, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k1,v) then get(k2, _default) yields default")
	void putThenGetWithOtherKeyAndDefaultValue() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys1 = signature().getKeyPair();
		String publicKey1 = Base64.getEncoder().encodeToString(keys1.getPublic().getEncoded());
		StorageReference eoa1 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey1));
		KeyPair keys2 = signature().getKeyPair();
		String publicKey2 = Base64.getEncoder().encodeToString(keys2.getPublic().getEncoded());
		StorageReference eoa2 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey2));
		addInstanceMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT, map, eoa1, ONE);
		StorageValue get = runInstanceMethodCallTransaction(account0, _50_000, classpath, new NonVoidMethodSignature(STORAGE_MAP, "getOrDefault", ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT), map, eoa2, TWO);

		assertEquals(TWO, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys then size is 10")
	void put100RandomThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		StorageReference[] accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
			accounts[i] = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey));
		}

		Random random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT, map, accounts[i], new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(10, size.value);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 times the same key then size is 1")
	void put100TimesSameKeyThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
		StorageReference eoa = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey));

		Random random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceMethodCallTransaction
				(key, account0, _50_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT,
					map, eoa, new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(1, size.value);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 times equal string keys then size is 1")
	void put100TimesEqualStringThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		Random random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceMethodCallTransaction
				(key, account0, _50_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT,
					map, new StringValue("hello"), new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(1, size.value);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 random BigInteger keys then min key is correct")
	void min() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		Random random = new Random();
		BigInteger min = null;
		for (int i = 0; i < 10; i++) {
			BigInteger bi = BigInteger.valueOf(random.nextLong()); 
			if (min == null || bi.compareTo(min) < 0)
				min = bi;

			addInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT, map, new BigIntegerValue(bi), new StringValue("hello"));
		}

		BigIntegerValue result = (BigIntegerValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_MIN, map);

		assertEquals(min, result.value);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys then remove the last then size is 9")
	void put100RandomThenRemoveLastThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		StorageReference[] accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
			accounts[i] = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey));
		}

		Random random = new Random();
		int i = 0;
		do {
			addInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT,
					map, accounts[i], new BigIntegerValue(BigInteger.valueOf(random.nextLong())));
		}
		while (++i < 10);

		addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_REMOVE, map, accounts[random.nextInt(10)]);
		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(9, size.value);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys and checks contains after each put")
	void put100RandomEachTimeCheckContains() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		StorageReference[] accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
			accounts[i] = addConstructorCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, new ConstructorSignature(ClassType.EOA, ClassType.STRING), new StringValue(publicKey));
		}

		BooleanValue[] results = new BooleanValue[10];

		Random random = new Random();
		for (int i = 0; i < 10; i++) {
			addInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, MODIFIABLE_STORAGE_MAP_PUT, map, accounts[i], new BigIntegerValue(BigInteger.valueOf(random.nextLong())));

			results[i] = (BooleanValue) addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(STORAGE_MAP, "containsKey", BOOLEAN, ClassType.OBJECT), map, accounts[i]);
		}

		for (BooleanValue result: results)
			assertTrue(result.value);
	}
}