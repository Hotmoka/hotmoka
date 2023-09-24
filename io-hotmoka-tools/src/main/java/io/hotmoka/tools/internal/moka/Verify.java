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

package io.hotmoka.tools.internal.moka;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "verify",
	description = "Verify a jar",
	showDefaultValues = true)
public class Verify extends AbstractCommand {

	@Parameters(index = "0", description = "the jar to verify")
	private Path jar;

	@Option(names = { "--libs" }, description = "the already instrumented dependencies of the jar")
	private List<Path> libs;

	@Option(names = { "--init" }, description = "verify as during node initialization")
	private boolean init;

	@Option(names = { "--allow-self-charged" }, description = "assume that @SelfCharged methods are allowed")
	private boolean allowSelfCharged;

	@Option(names = { "--version" }, description = "use the given version of the verification rules", defaultValue = "0")
	private int version;

	@Override
	protected void execute() throws Exception {
		byte[] bytesOfOrigin = readAllBytes(jar);
		var classpath = Stream.of(bytesOfOrigin);
		if (libs != null)
			classpath = Stream.concat(classpath, libs.stream().map(this::readAllBytes));

		var classLoader = TakamakaClassLoaders.of(classpath, version);
		var verifiedJar = VerifiedJars.of(bytesOfOrigin, classLoader, init, allowSelfCharged, false);
		verifiedJar.forEachError(System.err::println);
		if (verifiedJar.hasErrors())
			throw new CommandException("Verification failed because of errors");
		else
			System.out.println("Verification succeeded");
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