/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.moka.MokaNew;
import io.hotmoka.moka.jars.JarsVerifyOutput;
import io.hotmoka.moka.jars.JarsVerifyOutput.ErrorJSON;
import io.hotmoka.node.local.AbstractLocalNode;
import io.takamaka.code.constants.Constants;

/**
 * Tests for the moka jars command.
 */
public class JarsTests extends AbstractMokaTest {

	@Test
	@DisplayName("[moka jars verify] the verification of a jar without errors terminates without errors")
	public void verifyJarWorksIfNoErrors(@TempDir Path dir) throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");
		var examplesBasicDependency = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basicdependency.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		var actual = JarsVerifyOutput.of(MokaNew.jarsVerify(examplesBasic + " --libs " + examplesBasicDependency + " --libs " + takamakaCode + " --json"));
		assertTrue(actual.getErrors().count() == 0);
	}

	@Test
	@DisplayName("[moka jars verify] the verification of a jar with errors terminates with errors")
	public void verifyJarWorksIfErrors(@TempDir Path dir) throws Exception {
		var callerNotOnThis = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-callernotonthis.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		var actual = JarsVerifyOutput.of(MokaNew.jarsVerify(callerNotOnThis + " --libs " + takamakaCode + " --json"));
		assertTrue(actual.getErrors().count() == 1);
		ErrorJSON error = actual.getErrors().findFirst().get();
		assertEquals("io/hotmoka/examples/errors/callernotonthis/C.java:30", error.where);
		assertEquals("caller() can only be called on \"this\"", error.message);
	}

	@Test
	@DisplayName("[moka jars instrument] the instrumentation of a jar without errors terminates without errors")
	public void instrumentJarWorksIfNoErrors(@TempDir Path dir) throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");
		var examplesBasicDependency = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basicdependency.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		Path instrumented = dir.resolve("basic-instrumented.jar");
		String result = MokaNew.jarsInstrument(examplesBasic + " " + instrumented + " --libs " + examplesBasicDependency + " --libs " + takamakaCode + " --json").trim();
		assertEquals("{}", result);
	}
}