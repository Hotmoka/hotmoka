package io.hotmoka.tools.internal.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.instrumentation.StandardGasCostModel;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "instrument",
	description = "Instruments a jar",
	showDefaultValues = true)
public class Instrument extends AbstractCommand {

	@Parameters(description = "the jar to instrument")
	private String jar;

	@Option(names = { "--libs" }, description = "specifies the already instrumented dependencies of the jar")
	private List<String> libs;

	@Parameters(description = "the name of the instrument jar")
	private String destination;

	@Option(names = { "--init" }, description = "verifies as during node initialization")
	private boolean init;

	@Option(names = { "--allow-self-charged" }, description = "assumes that @SelfCharged methods are allowed")
	private boolean allowSelfCharged;

	@Option(names = { "--version" }, description = "uses the given version of the verification rules", defaultValue = "0")
	private int version;

	@Option(names = { "--skip-verification" }, description = "skips the preliminary verification of the jar")
	private boolean skipVerification;

	@Override
	public void run() {
		try {
			byte[] bytesOfOrigin = readAllBytes(jar);
			Stream<byte[]> classpath = Stream.of(bytesOfOrigin);
			if (libs != null)
				classpath = Stream.concat(classpath, libs.stream().map(this::readAllBytes));

			TakamakaClassLoader classLoader = TakamakaClassLoader.of(classpath, version);
			VerifiedJar verifiedJar = VerifiedJar.of(bytesOfOrigin, classLoader, init, allowSelfCharged, skipVerification);
			verifiedJar.issues().forEach(System.err::println);
	    	if (verifiedJar.hasErrors())
	    		System.err.println("Verification failed because of errors, no instrumented jar was generated");
	    	else {
	    		Path destination = Paths.get(this.destination);
		    	Path parent = destination.getParent();
		    	if (parent != null)
		    		Files.createDirectories(parent);

		    	InstrumentedJar.of(verifiedJar, new StandardGasCostModel()).dump(destination);
	    	}
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}

	private byte[] readAllBytes(String lib) {
		try {
			return Files.readAllBytes(Paths.get(lib));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}