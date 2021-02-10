/**
 * 
 */
package io.hotmoka.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;

/**
 * A test for node initialization.
 */
class Initialization extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("an initial transaction fails in an initialized node")
	void initialFailsInInitialized() throws CodeExecutionException, TransactionException, TransactionRejectedException, IOException {
		// the node is already initialized, since a non-initial transaction has been used to create
		// the account with ALL_FUNDS. Hence an attempt to run an initial transaction will fail
		throwsTransactionRejectedException(() -> addJarStoreInitialTransaction(Files.readAllBytes(Paths.get("jars/c13.jar")), takamakaCode()));
	}
}