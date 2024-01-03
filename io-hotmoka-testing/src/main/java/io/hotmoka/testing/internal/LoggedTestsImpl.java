/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.testing.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.hotmoka.testing.internal.LoggedTestsImpl.InitTestLogging;

/**
 * Shared code of test classes. It configures the logging system in such a way
 * to log each test class in its own log file and to report a header before the logs of each test.
 */
@ExtendWith(InitTestLogging.class)
public abstract class LoggedTestsImpl {

	/**
	 * The handler used for the logs. It is created before all tests are run
	 * and gets closed after their execution.
	 */
	private static FileHandler handler;

	private final static Logger LOGGER = Logger.getLogger(LoggedTestsImpl.class.getName());

	/**
	 * A callback used to configure the logging system. It is similar
	 * to a {@code @@BeforeAll} method, but allows one to access the actual
	 * name of the test class being run, used to create a per-class log file.
	 */
	public static class InitTestLogging implements BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) throws Exception {
			var maybeTestClass = context.getTestClass();
			if (maybeTestClass.isPresent())
				initTestLogging(maybeTestClass.get());
		}

		private static void initTestLogging(Class<?> clazz) throws SecurityException, IOException {
			if (System.getProperty("java.util.logging.config.file") == null) {
				// if the property is not set, we provide a default
				handler = new FileHandler(clazz.getSimpleName() + ".log", 100000000, 1);
				handler.setFormatter(new MyFormatter());
				handler.setLevel(Level.INFO);

				var logger = Logger.getLogger("");
				logger.setUseParentHandlers(false);
				Stream.of(logger.getHandlers()).forEach(logger::removeHandler);
				logger.addHandler(handler);
			}
		}
	}

	/**
	 * The formatter used to format the log entries. It treats log messages starting
	 * with {@code ENTRY} specially, by reporting a test header in the log file.
	 */
	private static class MyFormatter extends SimpleFormatter {
		private final static String format = "[%1$tF %1$tT] [%2$s] %3$s -- [%4$s]%5$s%n";
		private final static String SEPARATOR = System.lineSeparator();
		private final static String ENTRY_MARK = "ENTRY";

		@Override
		public String format(LogRecord lr) {
			if (lr.getMessage().startsWith(ENTRY_MARK))
				return SEPARATOR + lr.getMessage().substring(ENTRY_MARK.length()) + SEPARATOR + SEPARATOR;
			else {
				var thrown = lr.getThrown();
				String stackTrace;
				if (thrown != null) {
					try (var sw = new StringWriter(); var pw = new PrintWriter(sw)) {
						thrown.printStackTrace(pw);
						stackTrace = sw.toString();
						if (!stackTrace.isEmpty())
							stackTrace = "\n" + stackTrace;
					}
					catch (IOException e) {
						LOGGER.warning("cannot print the stack trace of a thrown exception");
						stackTrace = "";
					}
				}
				else
					stackTrace = "";

				return String.format(format,
					new Date(lr.getMillis()),
					lr.getLevel().getLocalizedName(),
					lr.getMessage(),
					lr.getSourceClassName() + " " + lr.getSourceMethodName(),
					stackTrace
				);
			}
		}
	}

	/**
	 * Closed after all tests in the class have run. It closes the handler,
	 * so that its lock file disappears.
	 */
	@AfterAll
    public static void closeHadler() {
    	var handler = LoggedTestsImpl.handler;
    	if (handler != null)
    		handler.close();
    }

	/**
	 * Logs a header before each test. This reports the name of the test and its display name (if any).
	 * 
	 * @param testInfo information about the test, from where the test name can be recovered
	 */
    @BeforeEach
	public void logTestHeader(TestInfo testInfo) {
		String methodName = testInfo.getTestMethod().get().getName();
		String displayName = testInfo.getDisplayName();
		String separator = System.lineSeparator();

		if (displayName.startsWith(methodName + "(")) {
			// there is no real display name
			String asterisks = "*".repeat(methodName.length());
			LOGGER.info(MyFormatter.ENTRY_MARK + asterisks + separator + methodName);
		}
		else {
			String asterisks = "*".repeat(displayName.length() + 2);
			LOGGER.info(MyFormatter.ENTRY_MARK + asterisks + separator + methodName + separator + "(" + displayName + ")");
		}
	}
}