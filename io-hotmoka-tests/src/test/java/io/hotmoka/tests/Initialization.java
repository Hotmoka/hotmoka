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

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A test for node initialization.
 */
class Initialization extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("an initial transaction fails in an initialized node")
	void initialFailsInInitialized() {
		// the node is already initialized, since a non-initial transaction has been used to create
		// the account with ALL_FUNDS. Hence an attempt to run an initial transaction will fail
		throwsTransactionRejectedException(() -> addJarStoreInitialTransaction(Files.readAllBytes(Paths.get("jars/c13.jar")), takamakaCode()));
	}
}