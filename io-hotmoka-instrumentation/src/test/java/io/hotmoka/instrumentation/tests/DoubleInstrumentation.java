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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.LogManager;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.hotmoka.constants.Constants;
import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.UnsupportedVerificationVersionException;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.VerifiedJars;

/**
 * This test tries to instrument the same jar twice. This checks
 * that translation does not modify static information of the verified jar,
 * hence it can be applied twice without exceptions.
 */
class DoubleInstrumentation {

	@Test
	void translateTwice() throws IOException, ClassNotFoundException, UnsupportedVerificationVersionException, VerificationException {
		var origin = Paths.get("src","test","resources", "io-hotmoka-examples-" + Constants.HOTMOKA_VERSION + "-lambdas.jar");
		var classpath = Paths.get("../modules/explicit/io-takamaka-code-" + Constants.TAKAMAKA_VERSION + ".jar");
		var bytesOfClasspath = Files.readAllBytes(classpath);
		var bytesOfOrigin = Files.readAllBytes(origin);
		var classLoader = TakamakaClassLoaders.of(Stream.of(bytesOfClasspath, bytesOfOrigin), 0);
    	var verifiedJar = VerifiedJars.of(bytesOfOrigin, classLoader, false, false, false);
    	var costModel = GasCostModels.standard();
		InstrumentedJars.of(verifiedJar, costModel);
    	InstrumentedJars.of(verifiedJar, costModel);
	}

	static {
		String current = System.getProperty("java.util.logging.config.file");
		if (current == null) {
			// if the property is not set, we provide a default (if it exists)
			URL resource = DoubleInstrumentation.class.getClassLoader().getResource("logging.properties");
			if (resource != null)
				try (var is = resource.openStream()) {
					LogManager.getLogManager().readConfiguration(is);
				}
			catch (SecurityException | IOException e) {
				throw new RuntimeException("Cannot load logging.properties file", e);
			}
		}
	}
}