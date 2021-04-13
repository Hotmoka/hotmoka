package io.hotmoka.tests;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
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
 * A test for from contract method.
 */
class FromContracts extends TakamakaTest {
	private final static ClassType FROM_CONTRACTS = new ClassType("io.hotmoka.examples.fromcontracts.FromContracts");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("fromcontracts.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test @DisplayName("new FromContracts().entry1() yields the eoa that calls the transaction")
	void callFromContract1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry1", ClassType.CONTRACT), entries);
		assertEquals(account(0), result);
	}

	@Test @DisplayName("new FromContracts().entry2() yields the FromContracts object itself")
	void callFromContract2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry2", ClassType.CONTRACT), entries);
		assertEquals(entries, result);
	}

	@Test @DisplayName("new FromContracts().entry3() yields the FromContracts object itself")
	void callFromContract3() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry3", ClassType.CONTRACT), entries);
		assertEquals(entries, result);
	}

	@Test @DisplayName("new FromContracts().entry4() yields the FromContracts object itself")
	void callFromContract4() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry4", ClassType.CONTRACT), entries);
		assertEquals(entries, result);
	}

	@Test @DisplayName("new FromContracts().entry5() yields the eoa that calls the transaction")
	void callFromContract5() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference entries = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(FROM_CONTRACTS));
		StorageValue result = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new NonVoidMethodSignature(FROM_CONTRACTS, "entry5", ClassType.CONTRACT), entries);
		assertEquals(account(0), result);
	}
}