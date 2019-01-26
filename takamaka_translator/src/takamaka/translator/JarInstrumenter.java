package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

class JarInstrumenter {
	private final JarFile originalJar;
	private final JarOutputStream instrumentedJar;
	private final ClassInstrumenter classInstrumenter;
	private final byte buffer[] = new byte[10240];

	JarInstrumenter(JarFile originalJar, JarOutputStream instrumentedJar) {
		this.originalJar = originalJar;
		this.instrumentedJar = instrumentedJar;
		this.classInstrumenter = new ClassInstrumenter(instrumentedJar);
	}

	void addEntry(JarEntry entry) {
		try (final InputStream input = originalJar.getInputStream(entry)) {
			String entryName = entry.getName();
			instrumentedJar.putNextEntry(new JarEntry(entryName));

			if (entryName.endsWith(".class"))
				classInstrumenter.addInstrumentationOf(input, entryName);
			else
				addJarEntry(input);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void addJarEntry(InputStream input) throws IOException {
		int nRead;
		while ((nRead = input.read(buffer, 0, buffer.length)) > 0)
			instrumentedJar.write(buffer, 0, nRead);
	}
}