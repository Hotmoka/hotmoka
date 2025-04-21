package io.hotmoka.moka.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import io.hotmoka.moka.MokaNew;

public abstract class AbstractMokaTest {

	protected static String runWithRedirectedStandardOutput(String command) throws IOException {
		var originalOut = System.out;

		try (var baos = new ByteArrayOutputStream(); var out = new PrintStream(baos)) {
			System.setOut(out);
			MokaNew.main(command);
			return new String(baos.toByteArray());
		}
		finally {
			System.setOut(originalOut);
		}
	}
}