package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.RedGreenMemoryBlockchain;
import io.takamaka.code.verification.VerificationException;

public abstract class TakamakaTest {
	public interface TestBody {
		public void run() throws Exception;
	}

	protected static MemoryBlockchain mkMemoryBlockchain(BigInteger... coins) throws IOException, TransactionException, CodeExecutionException {
		return MemoryBlockchain.of(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
	}

	protected static RedGreenMemoryBlockchain mkRedGreenMemoryBlockchain(BigInteger... coins) throws IOException, TransactionException, CodeExecutionException {
		return RedGreenMemoryBlockchain.of(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), coins);
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(Paths.get("../io-takamaka-examples/target/io-takamaka-examples-1.0-" + fileName));
	}

	protected static void throwsTransactionExceptionWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected.getName()))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionExceptionWithCause(String expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionException(TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			return;
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}	

	protected static void throwsVerificationException(TestBody what) {
		throwsTransactionExceptionWithCause(VerificationException.class, what);
	}
}