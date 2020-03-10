package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.RedGreenMemoryBlockchain;
import io.hotmoka.nodes.CodeExecutionException;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.issues.Issue;

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
			Class<? extends Throwable> actual = e.getCause().getClass();
			if (expected.isAssignableFrom(actual))
				return;

			e.printStackTrace();
			fail("wrong cause: expected " + expected.getName() + " but got " + actual.getName());
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
			Class<? extends Throwable> actual = e.getCause().getClass();
			if (actual.getName().equals(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + actual.getName());
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

	protected static void throwsVerificationExceptionWithCause(Class<? extends Issue> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof VerificationException) {
				Class<? extends io.takamaka.code.verification.issues.Error> actual = ((VerificationException) cause).getError().getClass();
				if (expected.isAssignableFrom(actual))
					return;

				fail("wrong issue: expected " + expected.getName() + " but got " + actual.getName());
			}

			fail("wrong cause: expected " + VerificationException.class.getName() + " but got " + cause.getClass().getName());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + VerificationException.class.getName());
	}
}