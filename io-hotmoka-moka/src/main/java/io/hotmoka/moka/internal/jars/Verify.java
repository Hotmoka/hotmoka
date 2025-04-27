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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.Gson;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.jars.JarsVerifyOutput;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "verify",
	description = "Verify a jar.",
	showDefaultValues = true)
public class Verify extends AbstractCommand {

	@Parameters(index = "0", description = "the path of the jar to verify")
	private Path jar;

	@Option(names = "--libs", description = "the paths of the already instrumented dependencies of the jar")
	private List<Path> libs;

	@Option(names = "--init", description = "verify as during node initialization")
	private boolean init;

	@Option(names = "--version", description = "use the given version of the verification rules", defaultValue = "0")
	private int version;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

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

		var errors = new ArrayList<io.hotmoka.verification.api.VerificationError>();

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
			new Output(errors).println(System.out, json);
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements JarsVerifyOutput {
		private final ErrorJSON[] errors;

		private Output(List<io.hotmoka.verification.api.VerificationError> errors) {
			this.errors = errors.stream().map(ErrorJSON::new).toArray(ErrorJSON[]::new);
		}

		/**
		 * Yields the output of this command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 */
		public static Output of(String json) {
			return new Gson().fromJson(json, Output.class);
		}

		@Override
		public Stream<ErrorJSON> getErrors() {
			return Stream.of(errors);
		}

		@Override
		public void println(PrintStream out, boolean json) {
			if (json)
				out.println(new Gson().toJson(this));
			else {
				if (errors.length == 0)
					out.println("Verification succeeded");
				else {
					out.println("Verification failed with the following errors:");
					int counter = 1;
					for (var error: errors)
						out.println(counter++ + ": " + error);
				}
			}
		}
	}
}