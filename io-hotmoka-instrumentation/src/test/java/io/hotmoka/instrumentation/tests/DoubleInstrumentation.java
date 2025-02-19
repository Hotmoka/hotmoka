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

package io.hotmoka.instrumentation.tests;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;

import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.testing.AbstractLoggedTests;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;
import io.takamaka.code.constants.Constants;

/**
 * This test tries to instrument the same jar twice. This checks
 * that translation does not modify static information of the verified jar,
 * hence it can be applied twice without exceptions.
 */
class DoubleInstrumentation extends AbstractLoggedTests {

	@Test
	void translateTwice() throws Exception {
		var origin = Paths.get("src","test","resources", "io-hotmoka-examples-lambdas.jar");
		// the classpath consists of the Takamaka runtime, that we can find in the Maven repository
		var classpath = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		var bytesOfClasspath = Files.readAllBytes(classpath);
		var bytesOfOrigin = Files.readAllBytes(origin);
		var classLoader = TakamakaClassLoaders.of(Stream.of(bytesOfClasspath, bytesOfOrigin), 0);
    	var verifiedJar = VerifiedJars.of(bytesOfOrigin, classLoader, false, false);
    	var costModel = GasCostModels.standard();
		InstrumentedJars.of(verifiedJar, costModel);
    	InstrumentedJars.of(verifiedJar, costModel);
	}
}