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

package io.hotmoka.moka.internal.jars;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "instrument",
	description = "Instrument a jar.",
	showDefaultValues = true)
public class Instrument extends AbstractCommand {

	@Parameters(description = "the jar to instrument")
	private Path jar;

	@Option(names = { "--libs" }, description = "the already instrumented dependencies of the jar")
	private List<Path> libs;

	@Parameters(description = "the name of the instrument jar")
	private Path destination;

	@Option(names = { "--init" }, description = "verify as during node initialization")
	private boolean init;

	@Option(names = { "--version" }, description = "use the given version of the verification rules", defaultValue = "0")
	private int version;

	@Option(names = { "--skip-verification" }, description = "skip the preliminary verification of the jar")
	private boolean skipVerification;

	@Override
	protected void execute() throws CommandException {
		byte[][] classpath = new byte[libs== null ? 1 : (libs.size() + 1)][];
		int pos = 0;

		try {
			classpath[pos++] = Files.readAllBytes(jar);
		}
		catch (IOException e) {
			throw new CommandException("The file \"" + jar + "\" cannot be accessed", e);
		}

		if (libs != null)
			for (Path lib: libs) {
				try {
					classpath[pos++] = Files.readAllBytes(lib);
				}
				catch (IOException e) {
					throw new CommandException("The file \""+ lib + "\" cannot be accessed", e);
				}
			}

		TakamakaClassLoader classLoader;

		try {
			classLoader = TakamakaClassLoaders.of(Stream.of(classpath), version);
		}
		catch (UnknownTypeException e) {
			throw new CommandException("The Takamaka runtime is not reachable from the classpath", e);
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new CommandException("Verification version " + version + " is not supported");
		}

		var verifiedJar = VerifiedJars.of(classpath[0], classLoader, init, __error -> {}, skipVerification,
				e -> new CommandException(e.getMessage() + ": no instrumented jar was generated", e),
				e -> new CommandException(e.getMessage() + ": no instrumented jar was generated", e),
				e -> new CommandException(e.getMessage() + ": no instrumented jar was generated", e));

		try {
			Path parent = destination.getParent();
			if (parent != null)
				Files.createDirectories(parent);
			InstrumentedJars.of(verifiedJar, GasCostModels.standard()).dump(destination);
		}
		catch (UnknownTypeException | IllegalJarException e) {
			throw new CommandException("Cannot instrument the jar: " + e.getMessage(), e);
		}
		catch (IOException e) {
			throw new CommandException("Cannot create file " + destination + ": " + e.getMessage());
		}
	}
}