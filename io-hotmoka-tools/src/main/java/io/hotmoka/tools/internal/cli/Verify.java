package io.hotmoka.tools.internal.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "verify",
	description = "Verifies a jar",
	showDefaultValues = true)
public class Verify extends AbstractCommand {

	@Parameters(description = "the jar to verify")
	private Path jar;

	@Option(names = { "--libs" }, description = "the already instrumented dependencies of the jar")
	private List<Path> libs;

	@Option(names = { "--init" }, description = "verifies as during node initialization")
	private boolean init;

	@Option(names = { "--allow-self-charged" }, description = "assumes that @SelfCharged methods are allowed")
	private boolean allowSelfCharged;

	@Option(names = { "--version" }, description = "uses the given version of the verification rules", defaultValue = "0")
	private int version;

	@Override
	public void run() {
		try {
			byte[] bytesOfOrigin = readAllBytes(jar);
			Stream<byte[]> classpath = Stream.of(bytesOfOrigin);
			if (libs != null)
				classpath = Stream.concat(classpath, libs.stream().map(this::readAllBytes));

			TakamakaClassLoader classLoader = TakamakaClassLoader.of(classpath, version);
			VerifiedJar verifiedJar = VerifiedJar.of(bytesOfOrigin, classLoader, init, allowSelfCharged, false);
			verifiedJar.issues().forEach(System.err::println);
			if (verifiedJar.hasErrors())
				System.err.println("Verification failed because of errors");
			else
				System.out.println("Verification succeeded");
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}

	private byte[] readAllBytes(Path jar) {
		try {
			return Files.readAllBytes(jar);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}