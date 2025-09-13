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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.JarsVerifyOutputs;
import io.hotmoka.moka.api.jars.JarsVerifyOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.json.JarsVerifyOutputJson;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerificationErrors;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationError;
import io.hotmoka.verification.api.VerificationException;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "verify",
	header = "Verify a jar.",
	showDefaultValues = true)
public class Verify extends AbstractMokaCommand {

	@Parameters(index = "0", description = "the path of the jar to verify")
	private Path jar;

	@Option(names = "--libs", paramLabel = "<paths>", description = "the already instrumented dependencies of the jar; use --libs repeatedly to include more dependencies")
	private List<Path> libs;

	@Option(names = "--init", description = "verify as during node initialization")
	private boolean init;

	@Option(names = "--version", description = "use the given version of the verification rules", defaultValue = "0")
	private int version;

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
			throw new CommandException("Some type cannot be resolved", e);
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new CommandException("Verification version " + version + " is not supported");
		}

		var errors = new ArrayList<VerificationError>();

		try {
			VerifiedJars.of(classpath[0], classLoader, init, errors::add, false);
		}
		catch (UnknownTypeException e) {
			throw new CommandException("Some type cannot be resolved", e);
		}
		catch (IllegalJarException e) {
			throw new CommandException("The jar file is illegal", e);
		}
		catch (VerificationException e) {
		}
		finally {
			report(new Output(errors), JarsVerifyOutputs.Encoder::new);
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements JarsVerifyOutput {
		private final VerificationError[] errors;

		private Output(List<VerificationError> errors) {
			this.errors = errors.toArray(VerificationError[]::new);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(JarsVerifyOutputJson json) throws InconsistentJsonException {
			var errorsJson = json.getErrors().toArray(VerificationErrors.Json[]::new);
			this.errors = new VerificationError[errorsJson.length];
			for (int pos = 0; pos < errorsJson.length; pos++)
				this.errors[pos] = errorsJson[pos].unmap();
		}

		@Override
		public Stream<VerificationError> getErrors() {
			return Stream.of(errors);
		}

		@Override
		public String toString() {
			if (errors.length == 0)
				return "Verification succeeded\n";
			else {
				var sb = new StringBuilder();
				sb.append("Verification failed with the following errors:\n");
				int counter = 1;
				for (var error: errors)
					sb.append(counter++ + ": " + error + "\n");

				return sb.toString();
			}
		}
	}
}