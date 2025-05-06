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

import io.hotmoka.moka.JarsInstallOutputs;
import io.hotmoka.moka.JarsVerifyOutputs;
import io.hotmoka.moka.MokaNew;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.VerificationException;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.verification.api.VerificationError;
import io.takamaka.code.constants.Constants;

/**
 * Tests for the moka jars command.
 */
public class JarsTests extends AbstractMokaTestWithNode {

	@Test
	@DisplayName("[moka jars verify] the verification of a jar without errors terminates without errors")
	public void verifyJarWorksIfNoErrors(@TempDir Path dir) throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");
		var examplesBasicDependency = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basicdependency.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		var actual = JarsVerifyOutputs.from(MokaNew.jarsVerify(examplesBasic + " --libs " + examplesBasicDependency + " --libs " + takamakaCode + " --json"));
		assertTrue(actual.getErrors().count() == 0);
	}

	@Test
	@DisplayName("[moka jars verify] the verification of a jar with errors terminates with errors")
	public void verifyJarWorksIfErrors(@TempDir Path dir) throws Exception {
		var callerNotOnThis = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-callernotonthis.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		var actual = JarsVerifyOutputs.from(MokaNew.jarsVerify(callerNotOnThis + " --libs " + takamakaCode + " --json"));
		assertTrue(actual.getErrors().count() == 1);
		VerificationError error = actual.getErrors().findFirst().get();
		assertEquals("io/hotmoka/examples/errors/callernotonthis/C.java:30", error.getWhere());
		assertEquals("caller() can only be called on \"this\"", error.getMessage());
	}

	@Test
	@DisplayName("[moka jars instrument] the instrumentation of a jar without errors terminates without errors")
	public void instrumentJarWorksIfNoErrors(@TempDir Path dir) throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");
		var examplesBasicDependency = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basicdependency.jar");
		var takamakaCode = Maven.resolver().resolve("io.hotmoka:io-takamaka-code:" + Constants.TAKAMAKA_VERSION).withoutTransitivity().asSingleFile().toPath();
		Path instrumented = dir.resolve("basic-instrumented.jar");
		MokaNew.jarsInstrument(examplesBasic + " " + instrumented + " --libs " + examplesBasicDependency + " --libs " + takamakaCode + " --json");
		// no exceptions
	}

	@Test
	@DisplayName("[moka jars install] the installation of a jar without errors works correctly")
	public void installJarWorksIfNoErrors() throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");
		var examplesBasicDependency = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basicdependency.jar");

		// we install basicdependency.jar first, letting the gamete pay; no libs, therefore takamakaCode will be added by default
		var basicDependencyInstallOutput = JarsInstallOutputs.from(MokaNew.jarsInstall(examplesBasicDependency + " " + gamete + " --password-of-payer=" + passwordOfGamete + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));

		// then we install basic.jar, letting the gamete pay; we provide basicdependency.jar as dependency
		var basicInstallOutput = JarsInstallOutputs.from(MokaNew.jarsInstall(examplesBasic + " " + gamete + " --password-of-payer=" + passwordOfGamete + " --libs=" + basicDependencyInstallOutput.getTransaction() + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT));

		// finally we can call a static method without errors
		node.runStaticMethodCallTransaction(TransactionRequests.staticViewMethodCall(gamete, _100_000, basicInstallOutput.getTransaction(), MethodSignatures.ofVoid(StorageTypes.classNamed("io.hotmoka.examples.basic.Sub"), "ms")));
	}

	@Test
	@DisplayName("[moka jars install] the installation of a jar missing a dependency throws a TransactionException")
	public void installJarFailsIfDependencyIsMissingErrors() throws Exception {
		var examplesBasic = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-basic.jar");

		// we try to install basic.jar, letting the gamete pay; we do not provide basicdependency.jar as dependency, therefore this will fail
		assertTrue(MokaNew.jarsInstall(examplesBasic + " " + gamete + " --password-of-payer=" + passwordOfGamete + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT)
				.contains(TransactionException.class.getName()));
	}

	@Test
	@DisplayName("[moka jars install] the installation of an illegal jar throws a TransactionException")
	public void installJarFailsIfCodeIsIllegal() throws Exception {
		var illegalJar = Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + AbstractLocalNode.HOTMOKA_VERSION + "-illegalcalltofromcontract1.jar");

		// we try to install basic.jar, letting the gamete pay; we do not provide basicdependency.jar as dependency, therefore this will fail
		assertTrue(MokaNew.jarsInstall(illegalJar + " " + gamete + " --password-of-payer=" + passwordOfGamete + " --json --dir=" + dir + " --uri=ws://localhost:" + PORT)
				.contains(VerificationException.class.getName()));
	}
}