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

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import io.hotmoka.verification.TakamakaClassLoader;
import io.hotmoka.verification.VerifiedJar;

/**
 * This test tries to verify the same jar twice. This checks
 * that verification does not modify static information of the verified jar,
 * hence it can be applied twice without exceptions.
 */
class DoubleVerification {
	
	@Test
	void verifyTwice() throws IOException, XmlPullParserException {
		// we access the project.version property from the pom.xml file of the parent project
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = reader.read(new FileReader("../pom.xml"));
		String hotmokaVersion = (String) model.getProperties().get("hotmoka.version");
        String takamakaVersion = (String) model.getProperties().get("takamaka.version");
		Path origin = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + hotmokaVersion + "-lambdas.jar");
		Path classpath = Paths.get("../modules/explicit/io-takamaka-code-" + takamakaVersion + ".jar");
		byte[] bytesOfOrigin = Files.readAllBytes(origin);
		byte[] bytesOfClasspath = Files.readAllBytes(classpath);
    	TakamakaClassLoader classLoader = TakamakaClassLoader.of(Stream.of(bytesOfClasspath, bytesOfOrigin), 0);
    	VerifiedJar.of(bytesOfOrigin, classLoader, false, false, false);
    	VerifiedJar.of(bytesOfOrigin, classLoader, false, false, false);
	}
}