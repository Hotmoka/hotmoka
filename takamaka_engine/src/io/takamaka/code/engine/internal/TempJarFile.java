package io.takamaka.code.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempJarFile implements AutoCloseable {
	private final Path temp;

	public TempJarFile(byte[] bytes) throws IOException {
		this.temp = Files.createTempFile("takamaka_", ".jar");
		Files.write(temp, bytes);
	}

	public Path toPath() {
		return temp;
	}

	@Override
	public void close() throws IOException{
		Files.deleteIfExists(temp);
	}
}