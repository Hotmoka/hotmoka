package takamaka.tests;

import static org.junit.jupiter.api.Assertions.fail;

import takamaka.blockchain.TransactionException;
import takamaka.instrumentation.VerificationException;
import takamaka.instrumentation.issues.Issue;

public abstract class TakamakaTest {
	public interface TestBody {
		public void run() throws Exception;
	}

	protected static void throwsTransactionExceptionWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			Class<? extends Throwable> actual = e.getCause().getClass();
			if (expected.isAssignableFrom(actual))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + actual.getName());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + expected.getName());
	}

	protected static void throwsVerificationExceptionWithCause(Class<? extends Issue> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof VerificationException) {
				Class<? extends takamaka.instrumentation.issues.Error> actual = ((VerificationException) cause).getError().getClass();
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