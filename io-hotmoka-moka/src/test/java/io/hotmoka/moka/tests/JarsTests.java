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
import java.util.ArrayList;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import io.hotmoka.moka.MokaNew;
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
		String result = MokaNew.run("jars verify " + examplesBasic + " --libs " + examplesBasicDependency + " --libs " + takamakaCode + " --json");
		ArrayList<?> actual = new Gson().fromJson(result, ArrayList.class);
		assertTrue(actual.isEmpty());
	}

	@Test
	@DisplayName("[moka jars verify] the verification of a jar with errors terminates with errors")
	public void verifyJarWorksIfErrors(@TempDir Path dir) throws Exception {
		var callerNotOnThis = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-callernotonthis.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		String result = MokaNew.run("jars verify " + callerNotOnThis + " --libs " + takamakaCode + " --json");
		ArrayList<?> actual = new Gson().fromJson(result, ArrayList.class);
		assertTrue(actual.size() == 1);
		Object error = actual.get(0);
		// a bit fragile, it would be better to add a Gson deserializer for Error
		assertEquals("{where=io/hotmoka/examples/errors/callernotonthis/C.java:30, message=caller() can only be called on \"this\"}", error.toString());
	}

	@Test
	@DisplayName("[moka jars instrument] the instrumentation of a jar without errors terminates without errors")
	public void instrumentJarWorksIfNoErrors(@TempDir Path dir) throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");
		var examplesBasicDependency = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basicdependency.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		Path instrumented = dir.resolve("basic-instrumented.jar");
		String result = MokaNew.run("jars instrument " + examplesBasic + " " + instrumented + " --libs " + examplesBasicDependency + " --libs " + takamakaCode + " --json").trim();
		assertEquals("{}", result);
	}
}