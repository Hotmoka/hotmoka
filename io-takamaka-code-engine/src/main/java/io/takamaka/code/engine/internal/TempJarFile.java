package io.takamaka.code.engine.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A temporary jar file, created from its byte representation.
 */
public class TempJarFile implements AutoCloseable {

	/**
	 * The path where the file has been stored.
	 */
	private final Path temp;

	/**
	 * Creates a temporary jar file.
	 * 
	 * @param bytes the bytes of the jar file
	 * @throws IOException if the file cannot be created
	 */
	public TempJarFile(byte[] bytes) throws IOException {
		this.temp = Files.createTempFile("takamaka_", ".jar");
		Files.write(temp, bytes);
	}

	/**
	 * Yields the path where the file has been stored.
	 * 
	 * @return the path
	 */
	public Path toPath() {
		return temp;
	}

	@Override
	public void close() throws IOException{
		Files.deleteIfExists(temp);
	}
}