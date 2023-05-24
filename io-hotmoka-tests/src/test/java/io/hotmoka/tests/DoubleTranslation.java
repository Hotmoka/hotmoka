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

package io.hotmoka.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.hotmoka.constants.Constants;
import io.hotmoka.instrumentation.InstrumentedJar;
import io.hotmoka.instrumentation.StandardGasCostModel;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.VerifiedJars;

/**
 * This test tries to translate the same jar twice. This checks
 * that translation does not modify static information of the verified jar,
 * hence it can be applied twice without exceptions.
 */
class DoubleTranslation {

	@Test
	void translateTwice() throws IOException, ClassNotFoundException {
		var origin = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + Constants.HOTMOKA_VERSION + "-lambdas.jar");
		var classpath = Paths.get("../modules/explicit/io-takamaka-code-" + Constants.TAKAMAKA_VERSION + ".jar");
		var bytesOfClasspath = Files.readAllBytes(classpath);
		var bytesOfOrigin = Files.readAllBytes(origin);
		var classLoader = TakamakaClassLoaders.of(Stream.of(bytesOfClasspath, bytesOfOrigin), 0);
    	var verifiedJar = VerifiedJars.of(bytesOfOrigin, classLoader, false, false, false);
    	var costModel = new StandardGasCostModel();
		InstrumentedJar.of(verifiedJar, costModel);
    	InstrumentedJar.of(verifiedJar, costModel);
	}
}