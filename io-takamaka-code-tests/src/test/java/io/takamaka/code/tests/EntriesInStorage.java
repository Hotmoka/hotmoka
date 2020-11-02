/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A test for entries from contracts.
 */
class EntriesInStorage extends TakamakaTest {
	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	private final static ClassType ENTRIES = new ClassType("io.takamaka.tests.entriesinstorage.Entries");

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("entriesinstorage.jar", BigInteger.valueOf(100_000_000));
	}

	@Test @DisplayName("new Entries().entry1() yields the eoa that calls the transaction")
	void callEntry1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(ENTRIES));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(ENTRIES, "entry1", ClassType.CONTRACT), entries);
		assertEquals(account(0), result);
	}

	@Test @DisplayName("new Entries().entry5() yields the eoa that calls the transaction")
	void callEntry5() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(ENTRIES));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(ENTRIES, "entry5", ClassType.CONTRACT), entries);
		assertEquals(account(0), result);
	}
}