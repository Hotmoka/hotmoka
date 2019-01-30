package takamaka.translator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

class JarInstrumentation {
	private static final Logger LOGGER = Logger.getLogger(JarInstrumentation.class.getName());
	private final static String SUFFIX = "_takamaka.jar";

	JarInstrumentation(String jarName, Program program) throws IOException {
		new Initializer(jarName, program);
	}

	private class Initializer {
		private final JarFile originalJar;
		private final JarOutputStream instrumentedJar;
		private final byte buffer[] = new byte[10240];
		private final Program program;

		private Initializer(String jarName, Program program) throws IOException {
			LOGGER.fine(() -> "Processing " + jarName);

			this.program = program;

			try (final JarFile originalJar = this.originalJar = new JarFile(jarName);
				 final JarOutputStream instrumentedJar = this.instrumentedJar = new JarOutputStream(new FileOutputStream(new File(computeNameOfInstrumentedJar(jarName))))) {

				originalJar.stream().forEach(this::addEntry);
			}
			catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}

		private void addEntry(JarEntry entry) {
			try (final InputStream input = originalJar.getInputStream(entry)) {
				String entryName = entry.getName();
				instrumentedJar.putNextEntry(new JarEntry(entryName));

				if (entryName.endsWith(".class"))
					new ClassInstrumentation(input, entryName, instrumentedJar, program);
				else
					addJarEntryUnchanged(input);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private void addJarEntryUnchanged(InputStream input) throws IOException {
			int nRead;
			while ((nRead = input.read(buffer, 0, buffer.length)) > 0)
				instrumentedJar.write(buffer, 0, nRead);
		}

		private String computeNameOfInstrumentedJar(String jarName) {
			if (jarName.endsWith(".jar"))
				return jarName.substring(0, jarName.length() - 4) + SUFFIX;
			else
				return jarName + SUFFIX;
		}
	}
}