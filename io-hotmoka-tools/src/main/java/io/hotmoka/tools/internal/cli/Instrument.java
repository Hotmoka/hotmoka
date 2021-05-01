/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.tools.internal.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.instrumentation.InstrumentedJar;
import io.hotmoka.instrumentation.StandardGasCostModel;
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
	private Path jar;

	@Option(names = { "--libs" }, description = "the already instrumented dependencies of the jar")
	private List<Path> libs;

	@Parameters(description = "the name of the instrument jar")
	private Path destination;

	@Option(names = { "--init" }, description = "verifies as during node initialization")
	private boolean init;

	@Option(names = { "--allow-self-charged" }, description = "assumes that @SelfCharged methods are allowed")
	private boolean allowSelfCharged;

	@Option(names = { "--version" }, description = "uses the given version of the verification rules", defaultValue = "0")
	private int version;

	@Option(names = { "--skip-verification" }, description = "skips the preliminary verification of the jar")
	private boolean skipVerification;

	@Override
	protected void execute() throws Exception {
		byte[] bytesOfOrigin = readAllBytes(jar);
		Stream<byte[]> classpath = Stream.of(bytesOfOrigin);
		if (libs != null)
			classpath = Stream.concat(classpath, libs.stream().map(this::readAllBytes));

		TakamakaClassLoader classLoader = TakamakaClassLoader.of(classpath, version);
		VerifiedJar verifiedJar = VerifiedJar.of(bytesOfOrigin, classLoader, init, allowSelfCharged, skipVerification);
		verifiedJar.issues().forEach(System.err::println);
		if (verifiedJar.hasErrors())
			throw new CommandException("Verification failed because of errors, no instrumented jar was generated");
		else {
			Path parent = destination.getParent();
			if (parent != null)
				Files.createDirectories(parent);

			InstrumentedJar.of(verifiedJar, new StandardGasCostModel()).dump(destination);
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