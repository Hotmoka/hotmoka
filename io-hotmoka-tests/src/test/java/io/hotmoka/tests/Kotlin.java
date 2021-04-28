package io.hotmoka.tests;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A test for running code written in Kotlin.
 */
class Kotlin extends TakamakaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar(Paths.get("../kotlin/target/kotlin-0.0.1.jar"));
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	//@Test
	void installJar() {
		
	}
}