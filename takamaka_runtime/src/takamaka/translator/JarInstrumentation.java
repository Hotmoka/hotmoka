package takamaka.translator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

public class JarInstrumentation {
	private static final Logger LOGGER = Logger.getLogger(JarInstrumentation.class.getName());

	public JarInstrumentation(Path origin, Path destination, Program program) throws IOException {
		new Initializer(origin, destination, program);
	}

	private class Initializer {
		private final JarFile originalJar;
		private final JarOutputStream instrumentedJar;
		private final byte buffer[] = new byte[10240];
		private final Program program;

		private Initializer(Path origin, Path destination, Program program) throws IOException {
			LOGGER.fine(() -> "Processing " + origin);

			this.program = program;

			try (JarFile originalJar = this.originalJar = new JarFile(origin.toFile());
				 JarOutputStream instrumentedJar = this.instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {

				originalJar.stream().forEach(this::addEntry);
			}
			catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}

		private void addEntry(JarEntry entry) {
			try (InputStream input = originalJar.getInputStream(entry)) {
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
	}
}