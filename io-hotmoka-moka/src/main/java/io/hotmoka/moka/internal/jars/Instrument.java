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

import com.google.gson.Gson;

import io.hotmoka.cli.CommandException;
import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.moka.JarsInstrumentOutputs;
import io.hotmoka.moka.api.jars.JarsInstrumentOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.json.JarsInstrumentOutputJson;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationException;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "instrument",
	header = "Instrument a jar.",
	showDefaultValues = true)
public class Instrument extends AbstractMokaCommand {

	@Parameters(description = "the path of the jar to instrument")
	private Path jar;

	@Parameters(description = "the path of the instrumented jar")
	private Path destination;

	@Option(names = "--libs", paramLabel = "<paths>", description = "the already instrumented dependencies of the jar; use --libs repeatedly to include more dependencies")
	private List<Path> libs;

	@Option(names = "--init", description = "verify as during node initialization")
	private boolean init;

	@Option(names = "--version", description = "use the given version of the verification rules", defaultValue = "0")
	private int version;

	@Option(names = "--skip-verification", description = "skip the preliminary verification of the jar")
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

		VerifiedJar verifiedJar;

		try {
			var classLoader = TakamakaClassLoaders.of(Stream.of(classpath), version);
			verifiedJar = VerifiedJars.of(classpath[0], classLoader, init, __error -> {}, skipVerification);
			Path parent = destination.getParent();
			if (parent != null)
				Files.createDirectories(parent);

			InstrumentedJars.of(verifiedJar, GasCostModels.standard()).dump(destination);
		}
		catch (UnknownTypeException | VerificationException | IllegalJarException e) {
			throw new CommandException("Instrumentation failed", e);
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new CommandException("Verification version " + version + " is not supported");
		}
		catch (IOException e) {
			throw new CommandException("Cannot create file " + destination, e);
		}

		report(new Output(), JarsInstrumentOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements JarsInstrumentOutput {

		private Output() {}

		/**
		 * Yields the output of this command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 */
		public static Output of(String json) {
			return new Gson().fromJson(json, Output.class);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(JarsInstrumentOutputJson json) throws InconsistentJsonException {}

		@Override
		public String toString() {
			return "";
		}
	}
}